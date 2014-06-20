package example.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ChannelFactory extends ChannelInitializer<SocketChannel> {

    private final Endpoint endpoint;

    public ChannelFactory(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    protected void initChannel(final SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new EchoServerHandler(this.endpoint));
    }

}
