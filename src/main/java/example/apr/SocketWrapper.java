package example.apr;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tomcat.jni.Buffer;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;

public class SocketWrapper {

    public static enum Status { OPEN_READ, OPEN_WRITE, ERROR, CLOSE; }

    static final ConcurrentHashMap<Long, SocketWrapper> CACHE =
            new ConcurrentHashMap<Long, SocketWrapper>();

    public final long pointer, pool;
    private volatile boolean closed;
    private ByteBuffer readBuffer, writeBuffer;

    private SocketWrapper(final long socket) {
        this.pointer = socket;
        try {
            this.pool = Socket.pool(socket);
        }
        catch (final Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    public void close() {
        if (this.isClosed()) {
            return;
        }
        this.closed = true;
        CACHE.remove(this.pointer);
        Pool.destroy(this.pool);
        Socket.close(this.pointer);
        Socket.destroy(this.pointer);
    }

    public boolean isClosed() {
        return this.closed;
    }

    public ByteBuffer createBuffer(final int size) {
        return Buffer.palloc(this.pool, size);
    }

    public void setReadBuffer(final ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
        Socket.setrbb(this.pointer, readBuffer);
    }
    public ByteBuffer getReadBuffer() {
        return this.readBuffer;
    }

    public void setWriteBuffer(final ByteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
        Socket.setsbb(this.pointer, writeBuffer);
    }
    public ByteBuffer getWriteBuffer() {
        return this.writeBuffer;
    }

    public void setBuffer(final ByteBuffer buffer) {
        this.setReadBuffer(buffer);
        this.setWriteBuffer(buffer);
    }

    public static SocketWrapper wrap(final long socket) {
        SocketWrapper wrapper = CACHE.get(socket);
        if (wrapper == null) {
            wrapper = new SocketWrapper(socket);
            final SocketWrapper old = CACHE.putIfAbsent(socket, wrapper);
            if (old != null) {
                wrapper = old;
                System.out.println("?");
            }
        }
        return wrapper;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (pointer ^ (pointer >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SocketWrapper other = (SocketWrapper) obj;
        if (pointer != other.pointer)
            return false;
        return true;
    }

}
