package example.apr;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Status;

public class Poller implements Runnable {

    static final Logger LOGGER = Logger.getLogger(Poller.class.getName());
    static final long POLL_TIMEOUT = 10;
    static int POLLSET_SIZE = 4096;

    private Endpoint endpoint;
    long endpointPollset = 0;
    private long pollerPool = 0;
    private long[] desc;
    private volatile boolean running = true;
    private ConcurrentHashMap<Long, boolean[]> waitToAdd = new ConcurrentHashMap<Long, boolean[]>();

    public Poller(final Endpoint endpoint) throws Exception {
        this.endpoint = endpoint;
        this.pollerPool = Pool.create(endpoint.rootPool);
        while (this.endpointPollset == 0) {
            this.endpointPollset = Poll.create(POLLSET_SIZE, this.pollerPool, 0, -1);
            if (this.endpointPollset == 0) {
                if (POLLSET_SIZE < 64 ) {
                    throw new Exception("Can not create Poller above size [64].");
                }
                POLLSET_SIZE /= 2;
            }
        }
        this.desc = new long[POLLSET_SIZE * 2];
    }

    public void add(final long socket, final boolean read, final boolean write) {
        if (this.running) {
            if (this.waitToAdd != null) {
                this.waitToAdd.put(socket, new boolean[] {read, write});
            }
            synchronized (this) {
                this.notify();
            }
        }
    }

    public void remove(final long socket) {
        if (Poll.remove(this.endpointPollset, socket) != Status.APR_NOTFOUND && this.running) {
            this.waitToAdd.remove(socket);
            synchronized (this) {
                this.notify();
            }
        }
    }

    public void run() {
        LOGGER.info("Started Poller with size[" + POLLSET_SIZE + "].");
        while (this.running) {
            try {
                for (final Map.Entry<Long, boolean[]> entry : this.waitToAdd.entrySet()) {
                    final Long socket = entry.getKey();
                    final boolean[] flags = entry.getValue();
                    this.waitToAdd.remove(socket);
                    if (this.running) {
                        if (Poll.add(this.endpointPollset, socket,
                                (flags[0] ? Poll.APR_POLLIN : 0) | (flags[1] ? Poll.APR_POLLOUT : 0))
                                != Status.APR_SUCCESS) {
                            this.endpoint.processSocket(socket, SocketWrapper.Status.CLOSE);
                        }
                    }
                }
                if (this.running && Poll.pollset(this.endpointPollset, this.desc) == 0) {
                    synchronized (this) {
                        try { this.wait(POLL_TIMEOUT); }
                        catch (final InterruptedException ignored) {}
                    }
                    continue;
                }
                final long now = System.currentTimeMillis();
                int rv = Poll.poll(this.endpointPollset, POLL_TIMEOUT, this.desc, true);
                if (rv > 0) {
                    for (int n = 0; n < rv; n++) {
                        final int events = (int) this.desc[2*n];
                        final long clientSocket = this.desc[2*n+1];
                        if (isEvent(events, Poll.APR_POLLIN)) {
                            this.endpoint.processSocket(clientSocket, SocketWrapper.Status.OPEN_READ);
                        }
                        else if (isEvent(events, Poll.APR_POLLOUT)) {
                            this.endpoint.processSocket(clientSocket, SocketWrapper.Status.OPEN_WRITE);
                        }
                        else if (isEvent(events, Poll.APR_POLLHUP)) {
                            this.endpoint.processSocket(clientSocket, SocketWrapper.Status.CLOSE);
                        }
                        else if (isEvent(events, Poll.APR_POLLERR) || isEvent(events, Poll.APR_POLLNVAL)) {
                            this.endpoint.processSocket(clientSocket, SocketWrapper.Status.ERROR);
                        }
                        else {
                            SocketWrapper.wrap(clientSocket).close();
                        }
                    }
                }
                else if (rv < 0) {
                    int errorNum = -rv;
                    if (errorNum == Status.TIMEUP) {
                        final ThreadPoolExecutor ioThreadPool = this.endpoint.ioThreadPool;
                        if (ioThreadPool != null && ioThreadPool.getActiveCount() == 0 && this.waitToAdd.isEmpty()) {
                            final long waitTime = POLL_TIMEOUT - (System.currentTimeMillis() - now);
                            if (waitTime > 0 && this.running) {
                                synchronized (this) {
                                    try { this.wait(waitTime); }
                                    catch (final InterruptedException ignored) {}
                                }
                            }
                        }
                    }
                    else if (errorNum != Status.EINTR) {
                        // Any non timeup or interrupted error is critical
                        if (errorNum >  Status.APR_OS_START_USERERR) {
                            errorNum -=  Status.APR_OS_START_USERERR;
                        }
                        LOGGER.severe("Poll Error: " + Error.strerror(errorNum));
                        // rebuild pollset
                        final long oldPollset = this.endpointPollset;
                        this.endpointPollset = Poll.create(POLLSET_SIZE, this.pollerPool, 0, -1);
                        Poll.destroy(oldPollset);
                    }
                }
            }
            catch(final Exception ex) {
                LOGGER.log(Level.SEVERE, "Error occured when polling.", ex);
            }
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void stop() throws Exception {
        if (!this.running) {
            return;
        }
        this.running = false;
        try {
            synchronized (this) {
                this.notify();
                this.wait(POLL_TIMEOUT);
            }
        }
        catch (final InterruptedException ignored) {}

        int count = Poll.pollset(this.endpointPollset, this.desc);
        if (count > 0) {
            for (int i = 0; i < count; ++i) {
                SocketWrapper.wrap(this.desc[2*i+1]).close();
            }
        }
        for (final Iterator<Map.Entry<Long, boolean[]>> itr =
                this.waitToAdd.entrySet().iterator(); itr.hasNext(); itr.remove()) {
            SocketWrapper.wrap(itr.next().getKey()).close();
        }
        this.waitToAdd.clear();
        this.waitToAdd = null;
        this.endpoint = null;

        Poll.destroy(this.endpointPollset);
        this.endpointPollset = 0;

        Pool.destroy(this.pollerPool);
        this.pollerPool = 0;
    }

    static boolean isEvent(final int events, final int flag) {
        return (events & flag) == flag;
    }

}
