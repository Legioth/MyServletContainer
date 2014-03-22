package org.vaadin.leif.servletcontainer.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/foo", "/baz/*" }, initParams = { @WebInitParam(name = "myParam", value = "myValue") })
public class MySecondServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        PrintWriter writer = resp.getWriter();

        writer.println("This is the second servlet");
        writer.println("Initialized with " + getInitParameter("myParam"));
    }
}
