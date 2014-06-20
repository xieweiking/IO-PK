package example.bio;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import example.util.ThreadPoolFactory;

public class Endpoint {

    static final Logger LOGGER = Logger.getLogger(Endpoint.class.getName());

    final String host;
    final int port, backlog;

    ServerSocket serverSocket;
    Acceptor acceptor;
    Poller poller;

    ThreadPoolExecutor ioThreadPool = null, dispatchThreadPool = null;

    Endpoint(final String host, final int port, final int backlog) {
        this.host = host;
        this.port = port;
        this.backlog = backlog;
    }

    public void processSocketWithOptions(final Socket clientSocket) {
        if (this.dispatchThreadPool != null) {
            this.dispatchThreadPool.execute(new SocketWithOptionsProcessor(this, clientSocket));
        }
    }

    public void processSocket(final Socket clientSocket) {
        if (this.ioThreadPool != null) {
            this.ioThreadPool.execute(new EchoWorker(this, clientSocket));
        }
    }

    public void start() throws Exception {
        LOGGER.info("Starting up BIO TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.serverSocket = new ServerSocket(this.port, this.backlog,
                new InetSocketAddress(this.host, port).getAddress());

        this.ioThreadPool = ThreadPoolFactory.create("IO-Thread", false);
        this.dispatchThreadPool = ThreadPoolFactory.create("Dispatcher", true);

        this.acceptor = new Acceptor(this);
        ThreadPoolFactory.runDaemon(this.acceptor, "Acceptor");

        this.poller = new Poller(this);
        ThreadPoolFactory.run(this.poller, "Poller");

        LOGGER.info("It's up.");
    }

    public void stop() throws Exception {
        LOGGER.info("Shutting down BIO TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        ThreadPoolFactory.shutdown(this.ioThreadPool);
        this.ioThreadPool = null;
        ThreadPoolFactory.shutdown(this.dispatchThreadPool);
        this.dispatchThreadPool = null;

        this.acceptor.stop();
        this.acceptor = null;

        this.serverSocket.close();
        this.serverSocket = null;

        this.poller.stop();
        this.poller = null;

        LOGGER.info("It's gone.");
    }

    public static Endpoint listen(final String host, final int port, final int backlog) {
        return new Endpoint(host, port, backlog);
    }

}
