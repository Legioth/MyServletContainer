package org.vaadin.leif.servletcontainer.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet({ "/*", "/baz/bar/*" })
public class MyServlet extends HttpServlet {

    private boolean useHeaders = true;

    private boolean useCookies = true;

    private boolean useSessions = true;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String name = getName(req);

        resp.setContentType("text/plain; charset=utf8");

        PrintWriter writer = resp.getWriter();
        writer.println("Hello, " + name);

        writer.println();
        writer.println("Context path: " + req.getContextPath());
        writer.println("Servlet path: " + req.getServletPath());
        writer.println("Path info:" + req.getPathInfo());

        if (useHeaders) {
            writer.println();
            writer.println("User agent: " + req.getHeader("User-Agent"));

            resp.setHeader("X-Powered-By", "Internet");
        }

        if (useCookies) {
            resp.addCookie(new Cookie("myCookie", "myValue"
                    + new Random().nextInt(1000)));

            Cookie[] cookies = req.getCookies();
            writer.println();
            if (cookies.length == 0) {
                writer.println("No cookies");
            } else {
                for (Cookie cookie : cookies) {
                    writer.println("Cookie " + cookie.getName() + " = "
                            + cookie.getValue());
                }
            }
        }

        if (useSessions) {
            HttpSession session = req.getSession();
            Integer sessionCounter = (Integer) session
                    .getAttribute("myCounter");
            if (sessionCounter == null) {
                sessionCounter = Integer.valueOf(0);
            }

            sessionCounter = Integer.valueOf(sessionCounter.intValue() + 1);
            session.setAttribute("myCounter", sessionCounter);

            writer.println();
            writer.println("Session counter: " + sessionCounter);
        }
    }

    private String getName(HttpServletRequest req) {
        String nameParam = req.getParameter("name");

        if (nameParam == null) {
            return "unknown";
        } else if (nameParam.trim().isEmpty()) {
            return "anonymous";
        } else {
            return nameParam;
        }
    }
}
