package org.vaadin.leif.servletcontainer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class MyServletResponse implements HttpServletResponse {

    private static SimpleDateFormat headerDateFormat = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    static {
        headerDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final MyServletRequest request;
    private final ServletOutputStream socketStream;

    private String characterEncoding = "ISO-8859-1";
    private String contentType = "text/html; charset=" + characterEncoding;

    private PrintWriter writer;
    private ServletOutputStream outputStream;

    private int bufferLength = 0;
    // 10 mb should be enough for anyone
    private byte[] buffer = new byte[10 * 1024 * 1024];
    private OutputStream bufferStream = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            /*
             * Step 3, part 2 / 2
             * 
             * Implement stream to record bytes into the buffer, keeping
             * bufferLength up to date. There's no need to worry about the
             * buffer filling up right now.
             */
            if (bufferLength == buffer.length) {
                throw new UnsupportedOperationException(
                        "Buffer is full and chunked writing is not supported");
            }
            buffer[bufferLength++] = (byte) b;
        }
    };

    private Map<String, List<String>> headers = new HashMap<String, List<String>>();

    private List<Cookie> pendingCookies = new ArrayList<Cookie>();

    public MyServletResponse(MyServletRequest request, final OutputStream out) {
        this.request = request;
        this.socketStream = new ServletOutputStream() {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void close() throws IOException {
                out.close();
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }
        };
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException(
                    "Cannot use both getWriter() and getOutputStream()");
        }

        if (outputStream == null) {
            outputStream = createOutputStream();
        }

        return outputStream;
    }

    private ServletOutputStream createOutputStream() throws IOException {
        /*
         * Step 13
         * 
         * Create a ServletOutputStream that buffers data in the buffer array
         * using bufferStream.
         */
        throw new UnsupportedOperationException("Implement in step 13");
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException(
                    "Cannot use both getWriter() and getOutputStream()");
        }
        if (writer == null) {
            writer = createWriter(bufferStream);
        }

        return writer;
    }

    private PrintWriter createWriter(OutputStream bufferStream)
            throws IOException {
        /*
         * Step 3, part 1 / 2
         * 
         * Create a print writer that buffers data in the buffer array using
         * bufferStream. The writer should use the charset returned by
         * getCharacterEncoding().
         * 
         * This step continues in bufferStream.write(int)
         */
        return new PrintWriter(new OutputStreamWriter(bufferStream,
                getCharacterEncoding()));
    }

    @Override
    public void setCharacterEncoding(String characterEncoding) {
        if (writer != null) {
            return;
        }

        this.characterEncoding = characterEncoding;

        // Also update charset in contentType
        contentType = contentType.replaceFirst("(; charset=[^;]*)|$",
                "; charset=" + characterEncoding);
    }

    @Override
    public void setContentLength(int contentLenght) {
        setIntHeader("Content-Length", contentLenght);
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;

        if (writer == null) {
            // Extract charset
            String[] parameters = contentType.split("; ");
            // Ignore first part
            for (int i = 1; i < parameters.length; i++) {
                String parameter = parameters[i];
                if (parameter.startsWith("charset=")) {
                    characterEncoding = parameter
                            .substring("charset=".length());
                    // Stop searching
                    break;
                }
            }
        }
    }

    @Override
    public void addCookie(Cookie cookie) {
        /*
         * Step 11, step 1 / 2
         * 
         * Add a Set-Cookie header for the cookie. In this exercise, you can
         * ignore all cookie attributes and just set the name and the value.
         * This implementation can use addHeader, even though that might not be
         * completely correct since calling setHeader("Set-Cookie", "foo") would
         * then discard all previously set cookies.
         * 
         * A cookie header has the format "<name>=<value>".
         * 
         * This step continues in MyServletRequest.readCookies(Hashtable)
         */
        throw new UnsupportedOperationException("Implement in step 11");
    }

    @Override
    public void addHeader(String name, String value) {
        List<String> list = headers.get(name);
        if (list == null) {
            list = new ArrayList<String>();
            headers.put(name, list);
        }

        list.add(value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, Integer.toString(value));
    }

    @Override
    public boolean containsHeader(String header) {
        return headers.containsKey(header);
    }

    @Override
    public String getHeader(String header) {
        List<String> list = headers.get(header);
        if (list == null) {
            return null;
        } else {
            return list.get(0);
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        return new HashSet<String>(headers.keySet());
    }

    @Override
    public Collection<String> getHeaders(String header) {
        List<String> list = headers.get(header);
        if (list == null) {
            return new ArrayList<String>();
        } else {
            return new ArrayList<String>(list);
        }
    }

    @Override
    public void setDateHeader(String name, long timestamp) {
        setHeader(name, headerDateFormat.format(new Date(timestamp)));
    }

    @Override
    public void setHeader(String name, String value) {
        headers.remove(name);
        addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, Integer.toString(value));
    }

    void close() throws IOException {
        checkForSession();

        // Flush to make sure bufferLocation is up to date
        if (writer != null) {
            writer.flush();
        } else if (outputStream != null) {
            outputStream.flush();
        }

        writeResponseLineAndRequiredHeaders(socketStream, bufferLength);

        writeConfiguredHeaders(socketStream, headers);

        writeResponseBody(socketStream, buffer, bufferLength);
    }

    private void checkForSession() {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (UnsupportedOperationException e) {
            // Ignore this until session support is implemented
            return;
        }

        if (session != null) {
            /*
             * Step 12
             * 
             * If the session is new, make sure a JSESSIONID cookie is set.
             */
            throw new UnsupportedOperationException("Implement in step 12");
        }
    }

    private void writeResponseLineAndRequiredHeaders(
            ServletOutputStream socketStream, int bufferLength)
            throws IOException {
        /*-
         * Step 4, part 1 / 2
         * 
         * Write the beginning of a proper HTTP response to socketStream.
         * The written data should include: 
         * - Response line ("<http version> <status code> <message>") 
         * - Content-Type header based on set content type
         * - Content-Length header based on buffer length
         * - Connection: close header to avoid HTTP keep-alive
         * 
         * This step continues in writeResponseBody()
         */
        socketStream.println(request.getProtocol() + " 200 OK");

        if (!containsHeader("Content-Type")) {
            socketStream.println("Content-Type: " + contentType);
        }

        if (!containsHeader("Content-Length")) {
            socketStream.println("Content-Length: " + bufferLength);
        }

        // We don't support any keep alive stuff
        socketStream.println("Connection: close");
    }

    private static void writeConfiguredHeaders(
            ServletOutputStream socketStream, Map<String, List<String>> headers)
            throws IOException {
        if (headers.isEmpty()) {
            return;
        } else {
            /*
             * Step 10, part 2 / 2
             * 
             * Write headers configured by the servlet to socketStream. Each
             * header value is written to a separate line in the format
             * "<name>: <value>".
             */

            for (Entry<String, List<String>> entry : headers.entrySet()) {
                String header = entry.getKey();
                List<String> values = entry.getValue();

                for (String value : values) {
                    socketStream.println(header + ": " + value);
                }
            }
        }
    }

    private void writeResponseBody(ServletOutputStream socketStream,
            byte[] buffer, int bufferLength) throws IOException {
        /*-
         * Step 4, part 2 / 2
         * 
         * Write the end of a proper HTTP response to socketStream.
         * The written data should include:
         * - Empty line to denote the end of the headers 
         * - The actual response body
         */

        socketStream.println();

        socketStream.write(buffer, 0, bufferLength);

        socketStream.flush();
        socketStream.close();
    }

    private static final void specialMethod() {
        /*
         * A special method that is here just to keep some classes that might be
         * needed later on imported even when there's a save action that removes
         * unused imports.
         */

        Class<?>[] classes = { OutputStreamWriter.class, Entry.class };
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBufferSize(int arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDateHeader(String arg0, long arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectURL(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public String encodeRedirectUrl(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeURL(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public String encodeUrl(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int arg0) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int arg0, String arg1) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void setStatus(int status, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushBuffer() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(int status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBufferSize() {
        throw new UnsupportedOperationException();
    }

}
