package example.aio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import example.util.ByteBufferCleaner;

public class WriteHandler implements CompletionHandler<Integer, ByteBuffer> {

    private final Endpoint endpoint;
    private final AsynchronousSocketChannel clientSocket;

    public WriteHandler(final Endpoint endpoint, final AsynchronousSocketChannel clientSocket) {
        this.endpoint = endpoint;
        this.clientSocket = clientSocket;
    }

    public void completed(final Integer count, final ByteBuffer buffer) {
        if (count > 0 && this.clientSocket.isOpen()) {
            buffer.clear();
            try {
                this.clientSocket.read(buffer, buffer, new ReadHandler(this.endpoint, this.clientSocket));
            }
            catch (final Exception ex) {
                this.failed(ex, buffer);
            }
        }
        else {
            this.failed(null, buffer);
        }
    }

    public void failed(final Throwable t, final ByteBuffer buffer) {
        ByteBufferCleaner.clean(buffer);
        try { this.clientSocket.shutdownOutput(); }
        catch (final IOException ignored) {}
    }

}
