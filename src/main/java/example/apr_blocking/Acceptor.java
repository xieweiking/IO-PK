package example.apr_blocking;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Socket;

public class Acceptor implements Runnable {

    static final Logger LOGGER = Logger.getLogger(Acceptor.class.getName());
    private Endpoint endpoint;
    private volatile boolean running = true;

    public Acceptor(final Endpoint endpoint) throws Exception {
        this.endpoint = endpoint;
    }

    public void run() {
        while (this.running) {
            try {
                final long clientSocket = Socket.accept(this.endpoint.serverSocket);
                this.endpoint.processSocketWithOptions(clientSocket);
            }
            catch(final Exception ex) {
                if (this.running) {
                    LOGGER.log(Level.SEVERE, "Error occured when accepting socket.", ex);
                }
            }
        }
    }

    public void stop() throws Exception {
        if (!this.running) {
            return;
        }
        this.running = false;
        this.endpoint = null;
    }

}
