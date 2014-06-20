package example.nio;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketWithOptionsProcessor implements Runnable {

    static final Logger LOGGER = Logger.getLogger(SocketWithOptionsProcessor.class.getName());

    static final int SO_READ_BUFFER_SIZE = 1024,
                     SO_WRITE_BUFFER_SIZE = 1024;

    private Endpoint endpoint;
    private SocketChannel socket;

    public SocketWithOptionsProcessor(final Endpoint endpoint, final SocketChannel clientSocket) {
        this.endpoint = endpoint;
        this.socket = clientSocket;
    }

    public void run() {
        try {
            /* set other options if you need. */
            this.socket.configureBlocking(false);
            final Socket sock = this.socket.socket();
            sock.setKeepAlive(true);
            sock.setReuseAddress(true);
            sock.setReceiveBufferSize(SO_READ_BUFFER_SIZE);
            sock.setSendBufferSize(SO_WRITE_BUFFER_SIZE);
            sock.setSoLinger(true, 0);
            sock.setTcpNoDelay(true);

            final int idx = this.endpoint.next.getAndIncrement() % this.endpoint.pollers.length;
            this.endpoint.pollers[idx].waitToReg.add(this.socket);
        }
        catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error occured when processing socket optoins.", ex);
        }
    }

}
