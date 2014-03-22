package org.vaadin.leif.servletcontainer;

import java.util.Iterator;

public class PathIterator implements Iterator<String> {

    private String path;
    private boolean hasNext = true;

    public PathIterator(String path) {
        this.path = path;
        advance();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public String next() {
        String next = path;

        advance();

        return next;
    }

    private void advance() {
        // Remove the last path segment
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            hasNext = false;
            return;
        }
        this.path = path.substring(0, lastSlash);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
