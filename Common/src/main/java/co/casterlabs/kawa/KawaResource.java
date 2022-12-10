package co.casterlabs.kawa;

import co.casterlabs.kawa.networking.ActiveLine;

public interface KawaResource {

    public void accept(String resourceId, ActiveLine line);

}
