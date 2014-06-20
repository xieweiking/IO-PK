package example.apr_blocking;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Socket;

import example.apr.SocketWrapper;

public class SocketWithOptionsProcessor implements Runnable {

    static final Logger LOGGER = Logger.getLogger(SocketWithOptionsProcessor.class.getName());

    static final int SO_READ_BUFFER_SIZE = 1024,
                     SO_WRITE_BUFFER_SIZE = 1024;

    private Endpoint endpoint;
    private SocketWrapper socket;

    public SocketWithOptionsProcessor(final Endpoint endpoint, final SocketWrapper clientSocket) {
        this.endpoint = endpoint;
        this.socket = clientSocket;
    }

    public void run() {
        /* set other options if you need. */
        Socket.optSet(this.socket.pointer, Socket.APR_SO_KEEPALIVE, 1);
        Socket.optSet(this.socket.pointer, Socket.APR_SO_LINGER, 1);
        Socket.optSet(this.socket.pointer, Socket.APR_SO_REUSEADDR, 1);
        Socket.optSet(this.socket.pointer, Socket.APR_SO_RCVBUF, SO_READ_BUFFER_SIZE);
        Socket.optSet(this.socket.pointer, Socket.APR_SO_SNDBUF, SO_WRITE_BUFFER_SIZE);
        Socket.optSet(this.socket.pointer, Socket.APR_SO_NONBLOCK, 0);
        Socket.timeoutSet(this.socket.pointer, -1);
        final ByteBuffer buffer = this.socket.createBuffer(SO_READ_BUFFER_SIZE);
        this.socket.setBuffer(buffer);

        this.endpoint.poller.add(this.socket);
    }

}
