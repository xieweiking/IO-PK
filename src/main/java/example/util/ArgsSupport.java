package example.util;

public abstract class ArgsSupport {

    static final String DEFAULT_HOST = "127.0.0.1";
    static final int DEFAULT_PORT = 8888, DEFAULT_BACKLOG = 100;

    public static String parseHost(final String... args) {
        if (args != null && args.length >= 1) {
            return args[0];
        }
        return DEFAULT_HOST;
    }

    public static int parsePort(final String... args) {
        if (args != null && args.length >= 2) {
            return Integer.parseInt(args[1]);
        }
        return DEFAULT_PORT;
    }

    public static int parseBacklog(final String... args) {
        if (args != null && args.length >= 3) {
            return Integer.parseInt(args[2]);
        }
        return DEFAULT_BACKLOG;
    }

}

