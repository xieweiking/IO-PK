package example.bio;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketWithOptionsProcessor implements Runnable {

    static final Logger LOGGER = Logger.getLogger(SocketWithOptionsProcessor.class.getName());

    static final int SO_READ_BUFFER_SIZE = 1024,
                     SO_WRITE_BUFFER_SIZE = 1024;

    private Endpoint endpoint;
    private Socket socket;

    public SocketWithOptionsProcessor(final Endpoint endpoint, final Socket clientSocket) {
        this.endpoint = endpoint;
        this.socket = clientSocket;
    }

    public void run() {
        try {
            /* set other options if you need. */
            this.socket.setKeepAlive(true);
            this.socket.setReuseAddress(true);
            this.socket.setReceiveBufferSize(SO_READ_BUFFER_SIZE);
            this.socket.setSendBufferSize(SO_WRITE_BUFFER_SIZE);
            this.socket.setSoLinger(true, 0);
            this.socket.setTcpNoDelay(true);
    
            this.endpoint.poller.add(this.socket);
        }
        catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error occured when processing socket optoins.", ex);
        }
    }

}
