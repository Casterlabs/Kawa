package co.casterlabs.kawa;

import co.casterlabs.kawa.networking.Line;

public interface KawaResource {

    /**
     * @return false if not accepted.
     */
    default boolean canAccept(String resourceId) {
        return true;
    }

    public Line.Listener accept(String resourceId);

}
