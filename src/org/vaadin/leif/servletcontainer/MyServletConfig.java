package org.vaadin.leif.servletcontainer;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

public class MyServletConfig implements ServletConfig {

    private final MyServletContext context;
    private final Hashtable<String, String> initParameters;
    private final String name;
    private boolean read = false;

    public MyServletConfig(Class<? extends HttpServlet> servletClass,
            MyServletContext context) {
        this.context = context;

        WebServlet webServlet = servletClass.getAnnotation(WebServlet.class);
        if (webServlet == null) {
            throw new RuntimeException("Class " + servletClass.getName()
                    + " has no @WebServlet annotation");
        }

        String name = webServlet.name();
        if (name.isEmpty()) {
            name = servletClass.getName();
        }
        this.name = name;

        this.initParameters = new Hashtable<String, String>();
    }

    public MyServletConfig(String name, MyServletContext context,
            Hashtable<String, String> initParameters) {
        this.name = name;
        this.context = context;
        this.initParameters = initParameters;
    }

    void setInitParameter(String name, String value) {
        if (read) {
            throw new IllegalStateException(
                    "Cannot modify init parameters after this config object has been used");
        }
        initParameters.put(name, value);
    }

    @Override
    public String getInitParameter(String name) {
        read = true;
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        read = true;
        return initParameters.keys();
    }

    @Override
    public ServletContext getServletContext() {
        read = true;
        return context;
    }

    @Override
    public String getServletName() {
        read = true;
        return name;
    }
}
