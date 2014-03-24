package org.vaadin.leif.servletcontainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

public class MyServletRequest implements HttpServletRequest {

    private final Socket connection;
    private final MyServletInputStream stream;

    private final String method;
    private final String protocol;

    private final Hashtable<String, Vector<String>> headers;

    private Cookie[] cookies = null;

    private final Hashtable<String, String[]> parameters = new Hashtable<String, String[]>();
    private final Hashtable<String, Object> attributes = new Hashtable<String, Object>();

    private final MyServletContext context;
    private final String contextPath;
    private final String servletPath;
    private final String pathInfo;
    private final String queryString;

    private String requestedSessionId;
    private boolean sessionLookupDone = false;
    private MyHttpSession session;

    private boolean paramsRead = false;

    private BufferedReader reader = null;

    public MyServletRequest(Socket connection, MyServletContext context,
            String method, String contextPath, String servletPath,
            String pathInfo, String queryString, String protocol)
            throws IOException {
        this.connection = connection;
        this.context = context;
        this.method = method;
        this.contextPath = contextPath;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.protocol = protocol;

        this.stream = new MyServletInputStream(connection.getInputStream());

        this.headers = new Hashtable<String, Vector<String>>();

        try {
            // Read headers, but don't fail if header reading is not yet
            // implemented
            ensureHeadersRead();
        } catch (UnsupportedOperationException e) {
            // Ignore
        }
    }

    private static void readHeaders(MyServletInputStream stream,
            Hashtable<String, Vector<String>> headers) throws IOException {
        /*
         * Step 10, part 1 / 2
         * 
         * Read headers from the stream until an empty line is read. There is
         * one header on each row, in the format "<name>: <value>". There can be
         * multiple headers with the same name, and they should all included in
         * the returned map.
         * 
         * This step continues in MyServletResponse.writeConfiguredHeaders()
         */

        while (true) {
            String line = stream.readStringLine();
            if (line.isEmpty()) {
                break;
            }
            String[] parts = line.split(":\\s+", 2);
            if (parts.length != 2) {
                throw new RuntimeException("Malformed? header line: " + line);
            }

            String name = parts[0];
            Vector<String> vector = headers.get(name);
            if (vector == null) {
                vector = new Vector<String>();
                headers.put(name, vector);
            }
            vector.add(parts[1]);
        }
    }

    private static void readCookies(Hashtable<String, Vector<String>> headers,
            List<Cookie> cookies) {
        /*
         * Step 11, part 2 / 2
         * 
         * Read cookies from all Cookie headers and add the cookies to the
         * provided list. One header can contain multiple cookies, separated by
         * "; ". Each cookie is in the form "<name>=<value>"
         */

        Vector<String> cookieHeaders = headers.get("Cookie");
        Pattern pattern = Pattern.compile("\\G(.+?)=([^;]*)(; )?");

        for (String cookieHeader : cookieHeaders) {
            Matcher matcher = pattern.matcher(cookieHeader);
            while (matcher.find()) {
                String name = matcher.group(1);
                String value = matcher.group(2);
                cookies.add(new Cookie(name, value));
            }
        }
    }

    @Override
    public int getContentLength() {
        String header = getHeader("Content-Length");
        if (header == null) {
            return -1;
        } else {
            return Integer.parseInt(header);
        }
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public Locale getLocale() {
        // TODO Read accept header
        return Locale.getDefault();
    }

    @Override
    public String getParameter(String name) {
        ensureParamsRead();

        String[] values = parameters.get(name);
        if (values == null) {
            return null;
        } else {
            return values[0];
        }
    }

    private void ensureParamsRead() {
        if (!paramsRead) {
            paramsRead = true;

            HashMap<String, List<String>> parameters = new HashMap<String, List<String>>();

            if (queryString != null && !queryString.isEmpty()) {
                parseParameters(parameters, queryString);
            }

            if ("POST".equals(method)) {
                /*
                 * Step 14
                 * 
                 * If the content type is application/x-www-form-urlencoded,
                 * read POST parameters from the first line of the request body
                 * from "stream" and use parseParameters to incorporate those
                 * parameters into the the same parameter map as for query
                 * string parameters.
                 */

                throw new UnsupportedOperationException("Implement in step 14");
            }

            for (Entry<String, List<String>> entry : parameters.entrySet()) {
                List<String> value = entry.getValue();
                this.parameters.put(entry.getKey(),
                        value.toArray(new String[value.size()]));
            }
        }
    }

    private static void parseParameters(
            HashMap<String, List<String>> parameters, String parameterString) {
        /*
         * Step 5
         * 
         * Parse the query string and add all parameters to the provided map.
         * Set the value to an empty string if only a parameter name but no
         * value is defined in the query string.
         * 
         * Test urls: http://localhost:8182/, http://localhost:8182/?name and
         * http://localhost:8182/?name=Foo
         */

        Matcher matcher = Pattern.compile("([^&=]+)=?([^&]*)").matcher(
                parameterString);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            List<String> list = parameters.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                parameters.put(key, list);
            }
            list.add(value);
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        ensureParamsRead();
        return parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        ensureParamsRead();
        return parameters.keys();
    }

    @Override
    public String[] getParameterValues(String parameter) {
        ensureParamsRead();
        return parameters.get(parameter);
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            if (headers.isEmpty()) {
                throw new RuntimeException(
                        "Can't read request payload before header reading has been implemented.");
            }

            reader = createReader();
        }

        return reader;
    }

    private BufferedReader createReader() throws IOException {
        /*
         * Step 15, part 1 / 2
         * 
         * Set the bytes remaining for the input stream based on
         * getContentLenght() so that the returned reader doesn't attempt to
         * read more data than what the browser has sent.
         * 
         * Create a BufferedReader that reads characters from the input stream
         * using the charset defined by getEffectiveCharacterEncoding().
         * 
         * This step continues in getEffectiveCharacterEncoding()
         */
        throw new UnsupportedOperationException("Implement in step 15");
    }

    private String getEffectiveCharacterEncoding() {
        /*
         * Step 15, part 2 / 2
         * 
         * Find the effective character encoding. If there is a Content-Type
         * header and it defines a charset, use that. Else, default to
         * "ISO-8859-1"
         */
        throw new UnsupportedOperationException("Implement in step 15");
    }

    @Override
    public String getRemoteAddr() {
        SocketAddress socketAddress = connection.getRemoteSocketAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetAddress = (InetSocketAddress) socketAddress;
            return inetAddress.getAddress().getHostAddress();
        } else {
            throw new UnsupportedOperationException(socketAddress.getClass()
                    .getName() + " not supported");
        }
    }

    @Override
    public String getServerName() {
        String hostHeader = getHeader("Host");
        if (hostHeader != null) {
            // Remove port if present
            return hostHeader.replaceAll(":.*", "");
        } else {
            return connection.getLocalAddress().getHostAddress();
        }
    }

    @Override
    public int getServerPort() {
        String hostHeader = getHeader("Host");
        if (hostHeader != null) {
            int portStart = hostHeader.indexOf(':');
            if (portStart > 0) {
                String portString = hostHeader.substring(portStart + 1);
                return Integer.parseInt(portString);
            }
        }

        return connection.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return attributes.keys();
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        } else {
            attributes.put(name, value);
        }
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public Cookie[] getCookies() {
        if (cookies == null) {
            List<Cookie> cookieList = new ArrayList<Cookie>();

            readCookies(headers, cookieList);

            cookies = cookieList.toArray(new Cookie[cookieList.size()]);
        }

        if (cookies.length == 0) {
            return null;
        } else {
            return cookies;
        }
    }

    @Override
    public String getHeader(String name) {
        ensureHeadersRead();
        Enumeration<String> enumeration = getHeaders(name);
        if (enumeration.hasMoreElements()) {
            return enumeration.nextElement();
        } else {
            return null;
        }
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        ensureHeadersRead();
        return headers.keys();
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        ensureHeadersRead();
        Vector<String> vector = headers.get(name);
        if (vector == null) {
            // Collections.emptyEnumeration() introduced in Java 7
            return new Vector<String>().elements();
        } else {
            return vector.elements();
        }
    }

    private void ensureHeadersRead() {
        if (headers.isEmpty()) {
            try {
                readHeaders(stream, headers);
            } catch (IOException e) {
                throw new RuntimeException();
            }

            if (headers.isEmpty()) {
                throw new RuntimeException(
                        "Are you sure there are _no_ headers?"
                                + " With HTTP/1.1, there should at least be a Host header.");
            }

            for (Entry<String, Vector<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (!key.trim().equals(key)) {
                    throw new RuntimeException("Header name \"" + key
                            + "\" contains additonal white space");
                }
                Vector<String> value = entry.getValue();
                for (String string : value) {
                    if (!string.trim().equals(string)) {
                        throw new RuntimeException("Header value \"" + string
                                + "\" for header " + key
                                + " contains additonal white space");
                    }
                }
            }
        }
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRequestURI() {
        return contextPath + servletPath + pathInfo;
    }

    @Override
    public String getRequestedSessionId() {
        if (requestedSessionId == null) {
            requestedSessionId = findRequestedSessionId(getCookies());
            if (requestedSessionId == null) {
                requestedSessionId = "";
            }
        }
        if (requestedSessionId.isEmpty()) {
            return null;
        }
        return requestedSessionId;
    }

    private String findRequestedSessionId(Cookie[] cookies) {
        /*
         * Step 12, part 2 / 3
         * 
         * Find the session id from the JSESSIONID cookie, or return null if no
         * session id can be found.
         * 
         * This step continues in MyServletResponse.checkForSession()
         */

        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (!sessionLookupDone) {
            sessionLookupDone = true;

            this.session = findSession(create);
        }

        return this.session;
    }

    private MyHttpSession findSession(boolean create) {
        /*
         * Step 12, part 1 / 3
         * 
         * Use getRequestedSessionId() and context.getSession(String) to find an
         * existing session. If no session is found and create == true, create a
         * new MyHttpSession instance and save it using
         * context.saveSession(MyHttpSession)
         * 
         * This step continues in findRequestedSessionId()
         */

        MyHttpSession session = null;

        // First try to find based on requested session id
        String requestedSessionId = getRequestedSessionId();
        if (requestedSessionId != null) {
            session = context.getSession(requestedSessionId);
        }

        // If not found, create and register a new session if needed
        if (session == null && create) {
            session = new MyHttpSession(context);
            context.saveSession(session);
        }

        return session;
    }

    private static final void specialMethod() {
        /*
         * A special method that is here just to keep some classes that might be
         * needed later on imported even when there's a save action that removes
         * unused imports.
         */

        Class<?>[] classes = { ArrayList.class, Pattern.class, Matcher.class,
                InputStreamReader.class, Entry.class };
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharacterEncoding(String arg0)
            throws UnsupportedEncodingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1)
            throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean authenticate(HttpServletResponse arg0) throws IOException,
            ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalAddr() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUserInRole(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(String arg0, String arg1) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public String getRealPath(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRemotePort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScheme() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncStarted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncSupported() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDateHeader(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIntHeader(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Part getPart(String arg0) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPathTranslated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringBuffer getRequestURL() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException();
    }
}
