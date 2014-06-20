package example.aio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import example.util.ByteBufferCleaner;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

    private final Endpoint endpoint;

    public AcceptHandler(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void completed(final AsynchronousSocketChannel clientSocket,
            final AsynchronousServerSocketChannel serverSocket) {
        serverSocket.accept(serverSocket, this);
        final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        try {
            clientSocket.read(buffer, buffer, new ReadHandler(this.endpoint, clientSocket));
        }
        catch (final Exception ex) {
            ByteBufferCleaner.clean(buffer);
            try { clientSocket.close(); }
            catch (final IOException ignored) {}
        }
    }

    public void failed(final Throwable t, final AsynchronousServerSocketChannel serverSocket) {
        t.printStackTrace();
    }

}
