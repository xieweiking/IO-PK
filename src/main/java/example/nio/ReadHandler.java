package example.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import example.util.ByteBufferCleaner;

public class ReadHandler {

    final Endpoint endpoint;

    public ReadHandler(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void process(final SocketChannel clientSocket, final ByteBuffer buffer, final SelectionKey key) {
        try {
            final int count = clientSocket.read(buffer);
            if (count >= 0) {
                buffer.flip();
                if (buffer.hasRemaining()) {
                    switch (buffer.get(0)) {
                    case '?':
                        this.close(clientSocket, buffer, key);
                        break;
                    case '!':
                        try {
                            this.endpoint.stop();
                        }
                        catch (final Exception ex) {
                            ex.printStackTrace();
                        }
                        break;
                    default:
                        key.attach(buffer);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }
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
