package org.vaadin.leif.servletcontainer;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

public class MyServletInputStream extends ServletInputStream {

    private final InputStream stream;
    private int bytesRemaining = -1;

    public MyServletInputStream(InputStream stream) {
        this.stream = stream;
    }

    @Override
    public int read() throws IOException {
        if (bytesRemaining == 0) {
            return -1;
        } else if (bytesRemaining > 0) {
            bytesRemaining--;
        }
        return stream.read();
    }

    void setBytesRemaining(int bytesRemaining) {
        this.bytesRemaining = bytesRemaining;
    }

    private final byte[] lineBuffer = new byte[10000];

    String readStringLine() throws IOException {
        int length = readLine(lineBuffer, 0, lineBuffer.length);
        if (length == -1) {
            throw new RuntimeException("Unexpected end of stream");
        }

        // -2 to ignore ending \r\n
        return new String(lineBuffer, 0, length - 2, "ISO-8859-1");
    }
}
