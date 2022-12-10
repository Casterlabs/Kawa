package co.casterlabs.kawa;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kawa.networking.Line;

public interface KawaResource {

    /**
     * @return false if not accepted.
     */
    public @Nullable Line.Listener accept(String resourceId);

}
