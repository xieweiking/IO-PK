package example.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Endpoint {

    static final Logger LOGGER = Logger.getLogger(Endpoint.class.getName());

    String host;
    int port, backlog;

    ServerBootstrap boot;
    NioEventLoopGroup bossLoop, workerLoop;
    ChannelFuture cf;

    private Endpoint(final String host, final int port, final int backlog) {
        this.host = host;
        this.port = port;
        this.backlog = backlog;
    }

    public void start() throws Exception {
        LOGGER.info("Starting up Netty TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.bossLoop = new NioEventLoopGroup();
        this.workerLoop = new NioEventLoopGroup();
        this.boot = new ServerBootstrap()
                .group(this.bossLoop, this.workerLoop)
                .channel(NioServerSocketChannel.class)
                .localAddress(this.host, this.port)
                .option(ChannelOption.SO_BACKLOG, this.backlog)
                .option(ChannelOption.SO_TIMEOUT, 60 * 60 * 1000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_RCVBUF, 1024)
                .childOption(ChannelOption.SO_SNDBUF, 1024)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelFactory(this));
        this.cf = this.boot.bind().sync();

        LOGGER.info("It's up.");
    }

    public void stop() throws Exception {
        LOGGER.info("Shutting down Netty TCP Echo Server on [" + this.host + ":" + this.port + "]... ");

        this.workerLoop.shutdownGracefully(500, 500, TimeUnit.MILLISECONDS);
        this.bossLoop.shutdownGracefully(500, 500, TimeUnit.MILLISECONDS);
        this.cf.channel().closeFuture().sync();

        LOGGER.info("It's gone.");
    }

    public static Endpoint listen(final String host, final int port, final int backlog) {
        return new Endpoint(host, port, backlog);
    }

}
