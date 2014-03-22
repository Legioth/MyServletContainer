package org.vaadin.leif.servletcontainer;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServlet;

public class MyServletContext implements ServletContext {
    public static class MappingResult {
        private final HttpServlet servlet;
        private final String servletPath;

        public MappingResult(HttpServlet servlet, String servletPath) {
            this.servlet = servlet;
            this.servletPath = servletPath;
        }

        public HttpServlet getServlet() {
            return servlet;
        }

        public String getServletPath() {
            return servletPath;
        }
    }

    private final String path;

    private final Map<String, HttpServlet> exactMappings = new HashMap<String, HttpServlet>();
    private final Map<String, HttpServlet> wildcardEndMappings = new HashMap<String, HttpServlet>();

    private final Map<String, MyHttpSession> sessions = new HashMap<String, MyHttpSession>();

    public MyServletContext(String path) {
        this.path = path;
    }

    @Override
    public String getContextPath() {
        return path;
    }

    void addServlet(HttpServlet servlet) throws ServletException {
        String[] mappings = getServletMappings(servlet);

        /*
         * Step 6
         * 
         * Dynamically register and initialize a servlet instance.
         * 
         * To register the servlet, look through the mappings array and populate
         * the exactMappings and wildcardEndMappings maps accordingly. Mappings
         * following some other pattern can be ignored in this example.
         * 
         * After registering the mappings, create a MyServletConfig instance and
         * set its init parameters based on the data in the @WebServlet
         * annotation. Finally initialize the servlet using the config object.
         */
        throw new UnsupportedOperationException("Implement in step 6");
    }

    MappingResult findSerlvet(String path) {

        // 1. Exact match
        if (exactMappings.containsKey(path)) {
            return new MappingResult(exactMappings.get(path), path);
        }

        /*
         * Step 7, part 2 / 2
         * 
         * Try to find a servlet for the give path. Try to find the longest
         * possible wildcard end match by using PathIterator to remove one path
         * segment at a time from the requested path.
         */
        // 2. Longest path-prefix
        if (true) {
            throw new UnsupportedOperationException("Implement in step 7");
        }

        // 3. By extension
        // XXX Ignored in this implementation

        // 4. Default servlet
        // XXX Ignored in this implementation

        return null;
    }

    private static String[] getServletMappings(HttpServlet servlet) {
        WebServlet webServlet = servlet.getClass().getAnnotation(
                WebServlet.class);
        String[] value = webServlet.value();
        String[] urlPatterns = webServlet.urlPatterns();
        if (value.length != 0 && urlPatterns.length != 0) {
            throw new RuntimeException(servlet.getClass().getName()
                    + " has both value and urlPatterns defined in @WebServlet");
        } else if (value.length != 0) {
            return value;
        } else if (urlPatterns.length != 0) {
            return urlPatterns;
        } else {
            throw new RuntimeException(
                    servlet.getClass().getName()
                            + " doesn't have value nor urlPatterns defined in @WebServlet");
        }
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return new Vector<String>().elements();
    }

    private static final Map<String, String> mimeTypes = new HashMap<String, String>();
    static {
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
    }

    @Override
    public String getMimeType(String file) {
        int lastDot = file.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        String extension = file.substring(lastDot + 1);
        if (extension.indexOf('/') > 0) {
            return null;
        }

        return mimeTypes.get(extension);
    }

    MyHttpSession getSession(String id) {
        MyHttpSession session = sessions.get(id);
        if (session != null) {
            session.updateLastAccessedTime();
        }
        return session;
    }

    void saveSession(MyHttpSession session) {
        sessions.put(session.getId(), session);
    }

    private static final void specialMethod() {
        /*
         * A special method that is here just to keep some classes that might be
         * needed later on imported even when there's a save action that removes
         * unused imports.
         */

        Class<?>[] classes = { Hashtable.class, WebInitParam.class, Entry.class };
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String arg0, Filter arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String arg0,
            Class<? extends Filter> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> void addListener(T arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(Class<? extends EventListener> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String arg0,
            Class<? extends Servlet> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> arg0)
            throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> arg0)
            throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> arg0)
            throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void declareRoles(String... arg0) {
        throw new UnsupportedOperationException();

    }

    @Override
    public Object getAttribute(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public ServletContext getContext(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getEffectiveMajorVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getEffectiveMinorVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration getFilterRegistration(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMajorVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getResource(String name) throws MalformedURLException {
        return getClassLoader().getResource(name);
    }

    @Override
    public int getMinorVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRealPath(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getResourceAsStream(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getResourcePaths(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServerInfo() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Servlet getServlet(String arg0) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServletContextName() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Enumeration<String> getServletNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration getServletRegistration(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Enumeration<Servlet> getServlets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void log(String message) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void log(Exception arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void log(String message, Throwable throwable) {
        throw new UnsupportedOperationException();
    }
}
