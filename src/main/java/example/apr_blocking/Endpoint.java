package example.apr_blocking;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;

import example.apr.SocketWrapper;
import example.util.AprTcNativeLibraryLoader;
import example.util.ThreadPoolFactory;

public class Endpoint {

    public static final String SYS_PROP_SERVER_HOME = "example.server.home";
    static final Logger LOGGER = Logger.getLogger(Endpoint.class.getName());

    final String host;
    final int port, backlog;

    long rootPool = 0, serverSocket = 0;
    Acceptor[] acceptors;
    Poller poller;

    ThreadPoolExecutor ioThreadPool = null, dispatchThreadPool = null;

    static {
        AprTcNativeLibraryLoader.load(LOGGER);
    }

    Endpoint(final String host, final int port, final int backlog) {
        this.host = host;
        this.port = port;
        this.backlog = backlog;
    }

    public void processSocketWithOptions(final long clientSocket) {
        if (this.dispatchThreadPool != null) {
            this.dispatchThreadPool.execute(new SocketWithOptionsProcessor(this, SocketWrapper.wrap(clientSocket)));
        }
    }

    public void processSocket(final SocketWrapper clientSocket) {
        if (this.ioThreadPool != null) {
            this.ioThreadPool.execute(new EchoWorker(this, clientSocket));
        }
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
        LOGGER.info("Starting up APR blocking TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.rootPool = Pool.create(0);
        this.bindAndListenSocket();

        this.ioThreadPool = ThreadPoolFactory.create("IO-Thread", false);
        this.dispatchThreadPool = ThreadPoolFactory.create("Dispatcher", true);

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
        LOGGER.info("Shutting down APR blocking TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        ThreadPoolFactory.shutdown(this.ioThreadPool);
        this.ioThreadPool = null;
        ThreadPoolFactory.shutdown(this.dispatchThreadPool);
        this.dispatchThreadPool = null;

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

    public static Endpoint listen(final String host, final int port, final int backlog) {
        return new Endpoint(host, port, backlog);
    }

}
