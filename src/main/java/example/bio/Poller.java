package example.bio;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Poller implements Runnable {

    static final Logger LOGGER = Logger.getLogger(Poller.class.getName());
    static final long POLL_TIMEOUT = 25;

    private Endpoint endpoint;

    private volatile boolean running = true;
    private LinkedBlockingQueue<Socket> waitToAdd = new LinkedBlockingQueue<Socket>();

    public Poller(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void add(final Socket socket) {
        if (this.waitToAdd != null) {
            this.waitToAdd.offer(socket);
        }
    }

    public void run() {
        while (this.running) {
            Socket socket = null;
            try {
                socket = this.waitToAdd.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
            }
            catch (final InterruptedException ignored) {}
            if (!this.running) {
                return;
            }
            if (socket != null) {
                this.endpoint.processSocket(socket);
            }
        }
    }

    public void stop() {
        this.running = false;
        for (final Socket socket : this.waitToAdd) {
            try { socket.close(); }
            catch (final IOException ignored) {}
        }
        this.waitToAdd.clear();
        this.waitToAdd = null;
    }

}
