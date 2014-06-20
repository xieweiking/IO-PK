package example.aio;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.logging.Logger;

import example.util.ThreadPoolFactory;

public class Endpoint {

    static final Logger LOGGER = Logger.getLogger(Endpoint.class.getName());

    final String host;
    final int port, backlog;

    Acceptor acceptor;
    AsynchronousChannelGroup rootGroup;
    AsynchronousServerSocketChannel serverSocket;


    Endpoint(final String host, final int port, final int backlog) {
        this.host = host;
        this.port = port;
        this.backlog = backlog;
    }


    public void start() throws Exception {
        LOGGER.info("Starting up AIO TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.rootGroup = AsynchronousChannelGroup.withThreadPool(ThreadPoolFactory.create("AIO-Thread", true));
        this.serverSocket = AsynchronousServerSocketChannel.open(this.rootGroup).bind(new InetSocketAddress(this.host, port), this.backlog);

        this.acceptor = new Acceptor(this);
        ThreadPoolFactory.run(this.acceptor, "Acceptor");

        LOGGER.info("It's up.");
    }

    public void stop() throws Exception {
        LOGGER.info("Shutting down AIO TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.rootGroup.shutdownNow();
        this.rootGroup = null;

        this.serverSocket.close();
        this.serverSocket = null;

        this.acceptor.stop();
        this.acceptor = null;

        LOGGER.info("It's gone.");
    }

    public static Endpoint listen(final String host, final int port, final int backlog) {
        return new Endpoint(host, port, backlog);
    }

}
