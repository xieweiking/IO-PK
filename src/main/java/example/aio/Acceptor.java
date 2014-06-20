package example.aio;

public class Acceptor implements Runnable {

    private final Endpoint endpoint;
    volatile boolean running = true;

    public Acceptor(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void run() {
        this.endpoint.serverSocket.accept(this.endpoint.serverSocket, new AcceptHandler(this.endpoint));
        while (this.running) {
            try { Thread.sleep(200); }
            catch (final InterruptedException ignored) {}
        }
    }

    public void stop() {
        this.running = false;
    }

}
