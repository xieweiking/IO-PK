package example.nio;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import example.util.ThreadPoolFactory;

public class Endpoint {

    static final Logger LOGGER = Logger.getLogger(Endpoint.class.getName());

    final String host;
    final int port, backlog;

    ServerSocketChannel serverSocket;
    Acceptor acceptor;
    Poller[] pollers;
    AtomicInteger next = new AtomicInteger(0);
    ReadHandler readHandler;
    WriteHandler writeHandler;

    ThreadPoolExecutor dispatcherPool = null;

    Endpoint(final String host, final int port, final int backlog) {
        this.host = host;
        this.port = port;
        this.backlog = backlog;
    }

    public void processSocketWithOptions(final SocketChannel clientSocket) {
        if (this.dispatcherPool != null) {
            this.dispatcherPool.execute(new SocketWithOptionsProcessor(this, clientSocket));
        }
    }

    public void start() throws Exception {
        LOGGER.info("Starting up NIO TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.serverSocket = ServerSocketChannel.open();
        this.serverSocket.socket().bind(new InetSocketAddress(this.host, port), this.backlog);
        this.serverSocket.configureBlocking(false);
        this.serverSocket.socket().setSoTimeout(60*60*1000);

        this.dispatcherPool = ThreadPoolFactory.create("Dispatcher", true);
        this.readHandler = new ReadHandler(this);
        this.writeHandler = new WriteHandler();

        this.acceptor = new Acceptor(this);
        ThreadPoolFactory.runDaemon(this.acceptor, "Acceptor");

        this.pollers = new Poller[2*ThreadPoolFactory.CPUs];
        for (int i = 0; i < this.pollers.length; ++i) {
            final Poller poller = new Poller(this);
            this.pollers[i] = poller;
            ThreadPoolFactory.run(poller, "IO-Poller-" + i);
        }

        LOGGER.info("It's up.");
    }

    public void stop() throws Exception {
        LOGGER.info("Shutting down NIO TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        ThreadPoolFactory.shutdown(this.dispatcherPool);
        this.dispatcherPool = null;

        this.serverSocket.close();
        this.serverSocket = null;

        this.acceptor.stop();
        this.acceptor = null;

        for (int i = 0; i < this.pollers.length; ++i) {
            this.pollers[i].stop();
            this.pollers[i] = null;
        }

        LOGGER.info("It's gone.");
    }

    public static Endpoint listen(final String host, final int port, final int backlog) {
        return new Endpoint(host, port, backlog);
    }

}
