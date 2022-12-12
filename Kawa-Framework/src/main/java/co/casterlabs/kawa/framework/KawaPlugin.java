package co.casterlabs.kawa.framework;

import java.util.ServiceLoader;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public abstract class KawaPlugin {
    private final @Getter FastLogger logger = new FastLogger(this.getName());

    @SuppressWarnings("unused")
    private ServiceLoader<java.sql.Driver> sqlDrivers;
    private ClassLoader classLoader;

    public abstract void onLoad();

    public @Nullable String getVersion() {
        return null;
    }

    public @Nullable String getAuthor() {
        return null;
    }

    public abstract @NonNull String getName();

    public abstract @NonNull String getId();

    /* ---------------- */
    /* Getters          */
    /* ---------------- */

    public final @Nullable ClassLoader getClassLoader() {
        return this.classLoader;
    }

}
