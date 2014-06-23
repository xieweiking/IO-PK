package example;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import example.util.ArgsSupport;
import example.util.ThreadPoolFactory;

public class EchoClient extends ArgsSupport implements Runnable {

    static int THREADS = 100;
    static long TIME_LEN = 5 * 60 * 1000;
    static String OUT_FILE = "reports.csv", TITLE = "TITLE";

    static EchoClient[] clients;
    static final AtomicLong recvCount = new AtomicLong(0);

    public static void main(final String... args) throws Exception {
        if (args != null && args.length >= 6) {
            THREADS = Integer.parseInt(args[2]);
            TIME_LEN = Integer.parseInt(args[3]) * 60 * 1000;
            OUT_FILE = args[4];
            TITLE = args[5];
        }
        clients = new EchoClient[THREADS];
        for (int i = 0; i < THREADS; ++i) {
            clients[i] = new EchoClient(parseHost(args), parsePort(args), i);
            ThreadPoolFactory.run(clients[i], "Test-Client-" + i);
        }
        ThreadPoolFactory.run(new Reporter(), "Reporter");
    }

    private final Socket socket;
    final int threadId;
    int round = 0, size, pos, lastCheckIdx = 0, checkedCount = 0;
    boolean exit;

    EchoClient(final String host, final int port, final int i) throws Exception {
        this.socket = new Socket(host, port);
        this.socket.setKeepAlive(true);
        this.socket.setReuseAddress(true);
        this.socket.setSoLinger(true, 0);
        this.socket.setTcpNoDelay(true);
        this.socket.setReceiveBufferSize(1024);
        this.socket.setSendBufferSize(1024);
        this.threadId = i;
    }

    public void run() {
        OutputStream out = null;
        InputStream in = null;
        final long end = System.currentTimeMillis() + TIME_LEN;
        try {
            out = this.socket.getOutputStream();
            in = this.socket.getInputStream();
            for (; System.currentTimeMillis() < end; ++round) {
                final String x = createRandomString();
                final byte[] buf = x.getBytes();
                this.size = buf.length;
                this.pos = 0;
                while (this.pos < this.size) {
                    int len = 1024;
                    if (this.pos + len > this.size) {
                        len = this.size - this.pos;
                    }
                    out.write(buf, this.pos, len);
                    out.flush();
                    in.read(buf, this.pos, len);
                    this.pos += len;
                    recvCount.addAndGet(len);
                }
                final String y = new String(buf);
                if (!x.equals(y)) {
                    System.err.println("Bad Echo! @Test-Client-" + this.threadId + ", round-" + round);
                    System.err.println("x: " + x);
                    System.err.println("y: " + y);
                    System.exit(1);
                }
                try { Thread.sleep(1L+RAND.nextInt(19)); }
                catch (final InterruptedException ignored) {}
            }
            out.write((byte)-1);
            out.flush();
        }
        catch (final Exception ex) {
            System.err.println("Error @Test-Client-" + this.threadId + ", round-" + round);
            ex.printStackTrace();
        }
        finally {
            exit = true;
            if (in != null) {
                try {
                    in.close();
                }
                catch (final Exception ignored) {}
            }
            if (out != null) {
                try {
                    out.close();
                }
                catch (final Exception ignored) {}
            }
            try {
                this.socket.close();
            }
            catch (final Exception ignored) {}
        }
    }

    static final char[] CHARS;
    static {
        CHARS = new char[('Z'-'A'+1) + ('z'-'a'+1) + ('9'-'0'+1)];
        for (int i = 0; i < CHARS.length;) {
            for (char c = 'A'; c <= 'Z'; ++c) {
                CHARS[i++] = c;
            }
            for (char c = 'a'; c <= 'z'; ++c) {
                CHARS[i++] = c;
            }
            for (char c = '0'; c <= '9'; ++c) {
                CHARS[i++] = c;
            }
        }
    }
    static final Random RAND = new Random();

    static String createRandomString() {
        int len = 5000 + RAND.nextInt(20000);
        final StringBuffer buffer = new StringBuffer(len);
        for (int i = 0; i < len; ++i) {
            buffer.append(CHARS[RAND.nextInt(CHARS.length)]);
        }
        return buffer.toString();
    }

}
