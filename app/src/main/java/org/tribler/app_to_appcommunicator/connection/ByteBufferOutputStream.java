package org.tribler.app_to_appcommunicator.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * ByteBuffer implementation of an {@link OutputStream}.
 *
 * Created by jaap on 5/26/16.
 */
public class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer buffer;

    public ByteBufferOutputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }


    @Override
    public void write(int oneByte) throws IOException {
        buffer.put((byte) oneByte);
    }

    @Override
    public void write(byte[] bytes, int offset, int count) throws IOException {
        buffer.put(bytes, offset, count);
    }
}
