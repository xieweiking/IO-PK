package example.apr_blocking;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Socket;

import example.apr.SocketWrapper;

public class EchoWorker implements Runnable {

    static final Logger LOGGER = Logger.getLogger(EchoWorker.class.getName());
    private SocketWrapper socket;
    private Endpoint endpoint;

    public EchoWorker(final Endpoint endpoint, final SocketWrapper socket) {
        this.endpoint = endpoint;
        this.socket = socket;
    }

    public void run() {
        try {
            final ByteBuffer readBuffer = this.socket.getReadBuffer();
            readBuffer.clear();
            for (int len = Socket.recvbb(this.socket.pointer, 0, readBuffer.capacity());
                    len > 0;
                    readBuffer.clear(), len = Socket.recvbb(this.socket.pointer, 0, readBuffer.capacity())) {
                readBuffer.position(0).limit(len);
                switch (readBuffer.get(0)) {
                case '?':
                    this.socket.close();
                    return;
                case '!':
                    this.socket.close();
                    this.endpoint.stop();
                    return;
                default:
                    Socket.sendbb(this.socket.pointer, 0, len);
                }
            }
        }
        catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error occured when handling socket.", ex);
        }
    }

}
