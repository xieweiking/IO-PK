package example.nio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Acceptor implements Runnable {

    static final Logger LOGGER = Logger.getLogger(Acceptor.class.getName());

    private Endpoint endpoint;
    Selector selector;

    private volatile boolean running = true;

    public Acceptor(final Endpoint endpoint) throws IOException {
        this.endpoint = endpoint;
        synchronized (Selector.class) {
            this.selector = Selector.open();
        }
        endpoint.serverSocket.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    public void run() {
        try {
            while (this.selector.select() > 0 && this.selector.isOpen() && this.running) {
                for (final Iterator<SelectionKey> itr = this.selector.selectedKeys().iterator();
                        itr.hasNext(); itr.remove()) {
                    final SelectionKey key = itr.next();
                    if (key.isValid()) {
                        final SelectableChannel channel = key.channel();
                        if (channel instanceof ServerSocketChannel && key.isAcceptable()) {
                            this.endpoint.processSocketWithOptions(((ServerSocketChannel) channel).accept());
                        }
                    }
                }
            }
        }
        catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, "Accept Error.", ex);
        }
    }

    public void stop() throws IOException {
        this.running = false;
        this.selector.close();
        this.selector.wakeup();
    }

}
