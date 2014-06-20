package example.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EchoWorker implements Runnable {

    static final Logger LOGGER = Logger.getLogger(EchoWorker.class.getName());
    private Socket socket;
    private Endpoint endpoint;

    public EchoWorker(final Endpoint endpoint, final Socket socket) {
        this.endpoint = endpoint;
        this.socket = socket;
    }

    public void run() {
        try {
            final byte[] buffer = new byte[1024];
            final InputStream input = this.socket.getInputStream();
            final OutputStream output = this.socket.getOutputStream();
            for (int len = input.read(buffer); len > 0; len = input.read(buffer)) {
                switch (buffer[0]) {
                case '?':
                    this.close(input, output);
                    return;
                case '!':
                    this.close(input, output);
                    this.endpoint.stop();
                    return;
                default:
                    output.write(buffer, 0, len);
                }
            }
        }
        catch (final IOException ignrd) {
            try { this.socket.close(); }
            catch (final IOException ignored) {}
        }
        catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error occured when handling socket.", ex);
        }
    }

    void close(final InputStream input, final OutputStream output) {
        if (input != null) {
            try { input.close(); }
            catch (final IOException ignored) {}
        }
        if (output != null) {
            try { output.close(); }
            catch (final IOException ignored) {}
        }
        try { this.socket.close(); }
        catch (final IOException ignored) {}
    }

}
