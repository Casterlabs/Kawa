package co.casterlabs.kawa.framework;

import java.io.File;
import java.io.IOException;

import co.casterlabs.kawa.Kawa;
import co.casterlabs.kawa.databases.KawaDBMongo;
import co.casterlabs.kawa.framework.plugins.PluginLoader;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@Data
@Accessors(chain = true)
@Command(name = "kawa", mixinStandardHelpOptions = true, version = "Kawa", description = "Starts Kawa")
public class KawaLauncher implements Runnable {

    @Option(names = {
            "-m",
            "--mongo"
    }, description = "The connection URI for mongoDB.")
    private String mongoUri;

    @Option(names = {
            "-t",
            "--this"
    }, description = "The IP of this instance, must be publicly accessible.")
    private String thisAddress;

    @Option(names = {
            "-p",
            "--password"
    }, description = "The PSK.")
    private String password;

    @Option(names = {
            "-m",
            "--max-clients"
    }, description = "The maximum amount of clients before the instance is considered saturated.")
    private int maxClients = Short.MAX_VALUE;

    @Option(names = {
            "-d",
            "--debug"
    }, description = "Enables debug logging.")
    private boolean debug = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        new CommandLine(new KawaLauncher()).execute(args);
    }

    /**
     * This is used by the command line interface to start Kawa with plugins.
     */
    @SneakyThrows
    @Deprecated
    @Override
    public void run() {
        if (this.debug) {
            FastLoggingFramework.setDefaultLevel(LogLevel.ALL);
        }

        Kawa.setMaxNumberOfClients(10);
        Kawa.setPassword(this.password);
        Kawa.setDb(new KawaDBMongo(this.mongoUri));

        Kawa.startListening(this.thisAddress);

        File pluginsDir = new File("./plugins");
        pluginsDir.mkdir();

//        List<Thread> loadThreads = new ArrayList<>(pluginsDir.listFiles().length);

        for (File file : pluginsDir.listFiles()) {
            if (file.isFile()) {
//                Thread t = new Thread(() -> {
                try {
                    PluginLoader.loadFile(file);
                } catch (Throwable e) {
                    FastLogger.logStatic(LogLevel.SEVERE, "An error occured whilst loading plugin:");
                    FastLogger.logException(e);
                }
//                });
//
//                loadThreads.add(t);
//                t.setName("Sora Load Thread: " + file.getName());
//                t.start();
            }
        }

//        // Wait for all of the threads to finish.
//        for (Thread t : loadThreads) {
//            if (t.isAlive()) {
//                try {
//                    t.join();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

}
