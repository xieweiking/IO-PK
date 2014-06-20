package example.apr;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Socket;

public class SocketProcessor implements Runnable {

    static final Logger LOGGER = Logger.getLogger(SocketProcessor.class.getName());
    private Endpoint endpoint;
    private SocketWrapper socket;
    private SocketWrapper.Status status;

    public SocketProcessor(final Endpoint endpoint, final SocketWrapper clientSocket, final SocketWrapper.Status status) {
        this.endpoint = endpoint;
        this.socket = clientSocket;
        this.status = status;
    }

    public void run() {
        switch (this.status) {
            case OPEN_READ:
                final ByteBuffer readBuffer = this.socket.getReadBuffer();
                final int n = Socket.recvbb(this.socket.pointer, 0, readBuffer.capacity());
                if (n > 0) {
                    readBuffer.position(0).limit(n);
                    switch (readBuffer.get(0)) {
                    case '?':
                        this.endpoint.poller.remove(this.socket.pointer);
                        this.socket.close();
                        return;
                    case '!':
                        try {
                            this.endpoint.stop();
                        }
                        catch (final Exception ex) {
                            ex.printStackTrace();
                        }
                        return;
                    default:
                        this.endpoint.poller.add(this.socket.pointer, false, true);
                    }
                }
                else {
                    this.endpoint.processSocket(this.socket.pointer, SocketWrapper.Status.CLOSE);
                }
                break;
            case OPEN_WRITE:
                final ByteBuffer writeBuffer = this.socket.getWriteBuffer();
                final int len = writeBuffer.limit();
                int m = 0;
                do {
                    m += Socket.sendbb(this.socket.pointer, m, len);
                }
                while (m >= 0 && m < len);
                if (m > 0) {
                    writeBuffer.clear();
                    this.endpoint.poller.add(this.socket.pointer, true, false);
                }
                else {
                    this.endpoint.processSocket(this.socket.pointer, SocketWrapper.Status.CLOSE);
                }
                break;
            case ERROR:
                LOGGER.warning("Process socket error.");
            case CLOSE:
                this.endpoint.poller.remove(this.socket.pointer);
                this.socket.close();
                break;
            default:
        }
    }

}
