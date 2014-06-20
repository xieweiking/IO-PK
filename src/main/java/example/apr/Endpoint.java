package example.apr;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;

import example.util.AprTcNativeLibraryLoader;
import example.util.ThreadPoolFactory;

public class Endpoint {

    static final Logger LOGGER = Logger.getLogger(Endpoint.class.getName());

    String host;
    int port, backlog;

    long rootPool = 0, serverSocket = 0;
    Acceptor[] acceptors = null;
    Poller poller = null;

    ThreadPoolExecutor ioThreadPool = null;

    static {
        AprTcNativeLibraryLoader.load(LOGGER);
    }

    public static Endpoint listen(final String host, final int port, final int backlog) {
        return new Endpoint().setHost(host).setPort(port).setBacklog(backlog);
    }

    public Endpoint setHost(final String host) {
        this.host = host;
        return this;
    }
    public String getHost() {
        return this.host;
    }

    public Endpoint setPort(final int port) {
        this.port = port;
        return this;
    }
    public int getPort() {
        return this.port;
    }

    public Endpoint setBacklog(final int backlog) {
        this.backlog = backlog;
        return this;
    }
    public int getBacklog() {
        return this.backlog;
    }

    void bindAndListenSocket() throws Exception {
        final long inetAddress = Address.info(this.host, Socket.APR_INET, this.port, 0, this.rootPool);
        this.serverSocket = Socket.create(Socket.APR_INET, Socket.SOCK_STREAM, Socket.APR_PROTO_TCP, this.rootPool);
        final int rc = Socket.bind(this.serverSocket, inetAddress);
        if (rc != 0) {
          throw new Exception("Acceptor can NOT bind Socket: " + Error.strerror(rc));
        }
        Socket.listen(this.serverSocket, this.backlog);
    }

    public void start() throws Exception {
        LOGGER.info("Starting up APR TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.rootPool = Pool.create(0);
        this.bindAndListenSocket();

        this.ioThreadPool = ThreadPoolFactory.create("IO-Thread", true);

        this.acceptors = new Acceptor[ThreadPoolFactory.CPUs];
        for (int i = 1; i <= this.acceptors.length; ++i) {
            final Acceptor acceptor = new Acceptor(this);
            ThreadPoolFactory.runDaemon(acceptor, "Acceptor-" + i);
            this.acceptors[i-1] = acceptor;
        }

        this.poller = new Poller(this);
        ThreadPoolFactory.run(this.poller, "Poller");

        LOGGER.info("It's up.");
    }

    public void stop() throws Exception {
        LOGGER.info("Shutting down APR TCP Echo Server with on [" + this.host + ":" + this.port + "]... ");

        ThreadPoolFactory.shutdown(this.ioThreadPool);
        this.ioThreadPool = null;

        for (final SocketWrapper socket : SocketWrapper.CACHE.values()) {
            socket.close();
        }
        SocketWrapper.CACHE.clear();

        for (final Acceptor acceptor : this.acceptors) {
            acceptor.stop();
        }
        this.acceptors = null;

        Socket.close(this.serverSocket);
        Socket.destroy(this.serverSocket);
        this.serverSocket = 0;

        this.poller.stop();
        this.poller = null;
        Pool.destroy(this.rootPool);

        LOGGER.info("It's gone.");
    }

    public void processSocket(final long clientSocket, final SocketWrapper.Status status) {
        if (this.ioThreadPool == null) {
            return;
        }
        this.ioThreadPool.execute(new SocketProcessor(this, SocketWrapper.wrap(clientSocket), status));
    }

    public void processSocketWithOptions(final long clientSocket) {
        if (this.ioThreadPool == null) {
            return;
        }
        this.ioThreadPool.execute(new SocketWithOptionsProcessor(this, SocketWrapper.wrap(clientSocket)));
    }

}
