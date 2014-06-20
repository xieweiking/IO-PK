package example.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    private final Endpoint endpoint;

    public EchoServerHandler(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg)
            throws Exception {
        if (msg instanceof ByteBuf) {
            final ByteBuf buf = (ByteBuf) msg;
            switch (buf.getByte(0)) {
            case '?':
                ctx.close().sync();
                break;
            case '!':
                this.endpoint.stop();
                break;
            default:
                ctx.writeAndFlush(buf);
            }
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
            throws Exception {
        ctx.close().sync();
    }

}
