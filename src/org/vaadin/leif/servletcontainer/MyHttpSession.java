package org.vaadin.leif.servletcontainer;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;

public class MyHttpSession implements HttpSession {

    private final Hashtable<String, Object> attributes = new Hashtable<String, Object>();

    private final String id = UUID.randomUUID().toString();
    private final long creationTime = System.currentTimeMillis();

    private final MyServletContext context;

    private long lastAccessedTime = creationTime;
    private int maxInactiveInterval = 60 * 60 * 1000;

    MyHttpSession(MyServletContext context) {
        this.context = context;
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
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    @Deprecated
    @Override
    public HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public String[] getValueNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNew() {
        return lastAccessedTime == creationTime;
    }

    @Deprecated
    @Override
    public void putValue(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttribute(String name) {
        Object removed = attributes.remove(name);
        if (removed instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener listener = (HttpSessionBindingListener) removed;
            listener.valueUnbound(new HttpSessionBindingEvent(this, name,
                    removed));
        }
    }

    @Deprecated
    @Override
    public void removeValue(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == getAttribute(name)) {
            // Nothing to do
            return;
        }
        removeAttribute(name);

        if (value != null) {
            attributes.put(name, value);
            if (value instanceof HttpSessionBindingListener) {
                HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
                listener.valueBound(new HttpSessionBindingEvent(this, name,
                        value));
            }
        }
    }

    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    void updateLastAccessedTime() {
        lastAccessedTime = System.currentTimeMillis();
    }
}
