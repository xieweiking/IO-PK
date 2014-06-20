package example.util;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public final class ByteBufferCleaner {

    private ByteBufferCleaner() {}

    public static void clean(final ByteBuffer buffer) {
        try {
            final Field cleanerField = buffer.getClass().getDeclaredField("cleaner");
            cleanerField.setAccessible(true);
            final Object cleaner = cleanerField.get(buffer);
            cleaner.getClass().getDeclaredMethod("clean").invoke(cleaner);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

}
