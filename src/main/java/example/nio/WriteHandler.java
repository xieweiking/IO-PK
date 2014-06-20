package example.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import example.util.ByteBufferCleaner;

public class WriteHandler {

    public void process(final SocketChannel clientSocket, final ByteBuffer buffer, final SelectionKey key) {
        try {
            final int count = clientSocket.write(buffer);
            if (count >= 0) {
                buffer.clear();
                key.attach(buffer);
                key.interestOps(SelectionKey.OP_READ);
            }
            else if (count < 0) {
                this.close(clientSocket, buffer, key);
            }
        }
        catch (final IOException ignored) {
            this.close(clientSocket, buffer, key);
        }
    }

    void close(final SocketChannel clientSocket, final ByteBuffer buffer, final SelectionKey key) {
        key.attach(null);
        key.cancel();
        try { clientSocket.close(); }
        catch (final IOException ignored) {}
        ByteBufferCleaner.clean(buffer);
    }

}
