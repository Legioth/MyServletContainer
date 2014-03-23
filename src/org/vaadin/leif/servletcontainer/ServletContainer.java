package org.vaadin.leif.servletcontainer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.vaadin.leif.servletcontainer.MyServletContext.MappingResult;
import org.vaadin.leif.servletcontainer.servlet.MySecondServlet;
import org.vaadin.leif.servletcontainer.servlet.MyServlet;

public class ServletContainer {
    private static final String explodedWarPath = null;

    private static final boolean useComplexStaticContexts = true;

    private final Map<String, MyServletContext> contexts = new HashMap<String, MyServletContext>();
    private final int port;

    public ServletContainer(int port) throws IOException {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        ServletContainer container = new ServletContainer(8182);

        setUpContexts(container);

        container.handleConnections();
    }

    private static void setUpContexts(ServletContainer container)
            throws Exception {
        MyServletContext context1 = new MyServletContext("");
        container.addContext(context1);

        if (useComplexStaticContexts) {
            context1.addServlet(new MyServlet());

            MySecondServlet servlet = new MySecondServlet();
            context1.addServlet(servlet);

            // Read init parameter just to very servlet has been properly
            // initialized
            String initParameter = servlet.getInitParameter("myParam");
            if (!"myValue".equals(initParameter)) {
                throw new IllegalStateException(
                        "Servlet has not been properly initialzed");
            }

            MyServletContext context2 = new MyServletContext("/bar");
            context2.addServlet(new MyServlet());
            container.addContext(context2);
        }

        if (explodedWarPath != null) {
            File explodedWarFile = new File(explodedWarPath);
            if (!explodedWarFile.exists() || !explodedWarFile.isDirectory()) {
                throw new RuntimeException(explodedWarPath
                        + " should be a directory");
            }
            container.addContext(new MyExplodedWarServletContext("/vaadin",
                    explodedWarFile));
        }
    }

    private void addContext(MyServletContext context) {
        contexts.put(context.getContextPath(), context);
    }

    private void handleConnections() throws IOException, ServletException {
        /*
         * Step 1
         * 
         * Open a TCP port and wait for connections, when someone connects, pass
         * the socket to the handleConnection method. Then close that connection
         * and start waiting for the next connection.
         */

        ServerSocket socket = new ServerSocket(port);

        while (true) {
            Socket connection = socket.accept();
            handleConnection(connection);
            connection.close();
        }
    }

    private void handleConnection(Socket connection) throws IOException,
            ServletException {
        try {
            MyServletInputStream stream = new MyServletInputStream(
                    connection.getInputStream());

            String requestLine = stream.readStringLine();

            String method;
            String path;
            String queryString;
            String protocol;

            /*
             * Step 2
             * 
             * Parse the HTTP request line to extract method, path, query string
             * and protocol.
             */

            // Could also use this lovely regex: (.+?) (.+?)(\\?(.+?))?( (.+))?

            String[] lineParts = requestLine.split(" ");
            if (lineParts.length < 2 || lineParts.length > 3) {
                throw new RuntimeException("Invalid start line: " + requestLine);
            }

            method = lineParts[0];

            String uri = lineParts[1];
            int queryStart = uri.indexOf('?');
            if (queryStart < 0) {
                path = uri;
                queryString = null;
            } else {
                path = uri.substring(0, queryStart);
                queryString = uri.substring(queryStart + 1);
            }

            if (lineParts.length == 3) {
                protocol = lineParts[2];
            } else {
                protocol = "HTTP/1.0";
            }

            if (!protocol.equals("HTTP/1.1") && !protocol.equals("HTTP/1.0")) {
                throw new RuntimeException("Unsupported protocol: " + protocol);
            }

            MyServletContext context = findContext(path);

            HttpServlet servlet;
            String servletPath;
            String pathInfo;

            if (useComplexStaticContexts) {
                /*
                 * Step 7, step 1 / 2
                 * 
                 * Find the right servlet by passing the appropriate path to
                 * context.findServlet(). Then use that to discover the
                 * servletPath and the pathInfo that are needed in the request.
                 * findServlet() should be passed what remains in the path after
                 * removing the prefix that matched the context. Similarly,
                 * servletPath should be the part that was actually matched by
                 * the servlet mapping and pathInfo should be the remainder of
                 * the requested path.
                 * 
                 * http://localhost:8182/baz/ should show the second servlet,
                 * initialized with myValue
                 * 
                 * http://localhost:8182/baz/bar/ should show Context path:
                 * <empty>, Servlet path: /baz/bar, Path info: /
                 * 
                 * http://localhost:8182/bar/asdf should show Context path:
                 * /bar, Servlet path: <empty>, Path info: /asdf
                 * 
                 * This step continues in MyServletContext.findServlet(String)
                 */

                String contextPath = context.getContextPath();

                String pathInContext = path.substring(contextPath.length());

                MappingResult mappingResult = context
                        .findSerlvet(pathInContext);
                if (mappingResult == null) {
                    throw new RuntimeException("Couldn't find servlet for "
                            + pathInContext + " in context " + contextPath);
                }

                servletPath = mappingResult.getServletPath();
                servlet = mappingResult.getServlet();
                pathInfo = pathInContext.substring(servletPath.length());

            } else {
                // Hardcoded simple values
                servlet = new MyServlet();
                servletPath = "";
                pathInfo = path;
            }

            String contextPath = context.getContextPath();

            MyServletRequest request = new MyServletRequest(connection,
                    context, method, contextPath, servletPath, pathInfo,
                    queryString, protocol);
            MyServletResponse response = new MyServletResponse(request,
                    connection.getOutputStream());

            servlet.service(request, response);

            response.close();

        } catch (Exception e) {
            e.printStackTrace();
            tryToWriteError(connection, e);
            System.exit(-1);
        }
    }

    private MyServletContext findContext(String path) {
        if (useComplexStaticContexts) {
            PathIterator pathIterator = new PathIterator(path);
            while (pathIterator.hasNext()) {
                String prefix = pathIterator.next();
                MyServletContext context = contexts.get(prefix);
                if (context != null) {
                    return context;
                }
            }
            return null;
        } else {
            return contexts.get("");
        }
    }

    private static void tryToWriteError(Socket connection, Exception e) {
        try {
            // Do this first to avoid doing other things if this already fails
            OutputStream outputStream = connection.getOutputStream();
            PrintWriter headerWriter = new PrintWriter(outputStream);
            headerWriter.println("HTTP/1.0 500 " + e.getMessage());
            headerWriter.flush();

            // Buffer to find content-length before sending headers
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintWriter bodyWriter = new PrintWriter(buffer);
            bodyWriter.println("Uncaught exception, terminating.");
            bodyWriter.println();
            e.printStackTrace(bodyWriter);
            bodyWriter.flush();

            headerWriter.println("Content-Type: text/plain");
            headerWriter.println("Content-Length: " + buffer.size());
            headerWriter.println("");
            headerWriter.flush();

            buffer.writeTo(outputStream);
            outputStream.close();

        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    private static final void specialMethod() {
        /*
         * A special method that is here just to keep some classes that might be
         * needed later on imported even when there's a save action that removes
         * unused imports.
         */

        Class<?>[] classes = { ServerSocket.class, Pattern.class,
                Matcher.class, MappingResult.class, Entry.class };
    }

}
