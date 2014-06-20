package example.util;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.OS;

public final class AprTcNativeLibraryLoader {

    public static final String SYS_PROP_SERVER_HOME = "example.server.home";

    private AprTcNativeLibraryLoader() {}

    public static void load(final Logger logger){
        try {
            final String homeStr = System.getProperty(SYS_PROP_SERVER_HOME);
            final File homeDir = new File(homeStr == null || homeStr.isEmpty() ? "./" : homeStr);
            addLibraryPath(new File(homeDir, "lib/" + getOsArch()).getCanonicalPath());
            Library.initialize(null);
            logger.info("Loaded JNI Library: [tc-native," + Library.versionString() + "] [APR," + Library.aprVersionString() + "]");

            final long [] inf = new long[16];
            OS.info(inf);
            logger.info("OS Info: " + 
                    "\n  Physical      " + inf[0] +
                    "\n  Avail         " + inf[1] +
                    "\n  Swap          " + inf[2] +
                    "\n  Swap free     " + inf[3] +
                    "\n  Shared        " + inf[4] +
                    "\n  Buffers size  " + inf[5] +
                    "\n  Load          " + inf[6] +
                    "\n  Idle          " + inf[7] +
                    "\n  Kernel        " + inf[8] +
                    "\n  User          " + inf[9] +
                    "\n  Proc creation " + inf[10] +
                    "\n  Proc kernel   " + inf[11] +
                    "\n  Proc user     " + inf[12] +
                    "\n  Curr working  " + inf[13] +
                    "\n  Peak working  " + inf[14] +
                    "\n  Page faults   " + inf[15]);
        }
        catch (final Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    public static String getOsArch() {
        final String arch = System.getProperty("os.arch");
        if (Arrays.asList("x86", "i386", "i586", "i686").contains(arch)) {
            return "x86";
        }
        else if (Arrays.asList("x86_64", "x64", "amd64").contains(arch)) {
            return "x64";
        }
        else {
            return arch;
        }
    }

    public static void addLibraryPath(final String pathToAdd) throws Exception{
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[]) usrPathsField.get(null);

        //check if the path to add is already present
        for(final String path : paths) {
            if(path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length+1);
        newPaths[newPaths.length-1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }

}
