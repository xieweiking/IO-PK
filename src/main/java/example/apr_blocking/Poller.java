package example.apr_blocking;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import example.apr.SocketWrapper;

public class Poller implements Runnable {

    static final Logger LOGGER = Logger.getLogger(Poller.class.getName());
    static final long POLL_TIMEOUT = 25;

    private Endpoint endpoint;

    private volatile boolean running = true;
    private LinkedBlockingQueue<SocketWrapper> waitToAdd = new LinkedBlockingQueue<SocketWrapper>();

    public Poller(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void add(final SocketWrapper socket) {
        if (this.waitToAdd != null) {
            this.waitToAdd.offer(socket);
        }
    }

    public void run() {
        while (this.running) {
            SocketWrapper socket = null;
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
        for (final SocketWrapper socket : this.waitToAdd) {
            socket.close();
        }
        this.waitToAdd.clear();
        this.waitToAdd = null;
    }

}
