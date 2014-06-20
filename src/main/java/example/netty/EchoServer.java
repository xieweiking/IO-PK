package example.netty;

import example.util.ArgsSupport;

public class EchoServer extends ArgsSupport {

    public static void main(final String... args) throws Exception {
        Endpoint.listen(parseHost(args), parsePort(args), parseBacklog(args)).start();
    }

}
