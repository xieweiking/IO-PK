package example.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Poller implements Runnable {

    static final Logger LOGGER = Logger.getLogger(Poller.class.getName());

    private Endpoint endpoint;
    Selector selector;
    ConcurrentLinkedQueue<SocketChannel> waitToReg = new ConcurrentLinkedQueue<SocketChannel>();

    private volatile boolean running = true;

    public Poller(final Endpoint endpoint) throws IOException {
        this.endpoint = endpoint;
        synchronized (Selector.class) {
            this.selector = Selector.open();
        }
    }

    public void run() {
        try {
            while (this.selector.isOpen() && this.running) {
                for (final Iterator<SocketChannel> itr = this.waitToReg.iterator();
                        itr.hasNext(); itr.remove()) {
                    itr.next().register(this.selector, SelectionKey.OP_READ, ByteBuffer.allocateDirect(1024));
                }
                if (this.selector.keys().isEmpty()) {
                    synchronized (this) {
                        try { this.wait(200); }
                        catch (final InterruptedException ignored) {}
                    }
                    continue;
                }
                if (this.selector.select(20) > 0 && this.selector.isOpen() && this.running) {
                    for (final Iterator<SelectionKey> itr = this.selector.selectedKeys().iterator();
                            itr.hasNext(); itr.remove()) {
                        final SelectionKey key = itr.next();
                        if (key.isValid()) {
                            final SelectableChannel channel = key.channel();
                            final Object attachment = key.attachment();
                            if (channel instanceof SocketChannel
                                    && attachment instanceof ByteBuffer) {
                                key.attach(null);
                                final SocketChannel clientSocket = (SocketChannel) channel;
                                final ByteBuffer buffer = (ByteBuffer) attachment;
                                if (key.isReadable()) {
                                    this.endpoint.readHandler.process(clientSocket, buffer, key);
                                }
                                else if (key.isWritable()) {
                                    this.endpoint.writeHandler.process(clientSocket, buffer, key);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, "Select error.", ex);
        }
    }

    public void stop() throws IOException {
        this.running = false;
        this.selector.close();
        this.selector.wakeup();
        synchronized (this) {
            this.notify();
        }
    }

}
