package co.casterlabs.kawa;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kawa.databases.KawaDB;
import co.casterlabs.kawa.databases.ResourceOffer;
import co.casterlabs.kawa.networking.KawaNetwork;
import co.casterlabs.kawa.networking.Line;
import lombok.Getter;
import lombok.Setter;

public class Kawa {
    private static @Getter @Nullable String thisAddress;
    private static @Setter String password;
    private static @Setter KawaDB db;

    // Note that maxClients isn't actually enforced, this is intentional.
    private static @Setter @Getter int maxNumberOfClients = Integer.MAX_VALUE;

    private static final Map<String, KawaResource> resourceProviders = new HashMap<>();

    static {
        Kawa.class.getClassLoader().setDefaultAssertionStatus(true);
    }

    public static void startListening(String thisAddress) throws IOException {
        assert password != null : "You must set the password before listening.";
        Kawa.thisAddress = thisAddress;

        KawaNetwork.startServer(thisAddress, password);
    }

    public Line getResource(String resourceId) {
        List<ResourceOffer> offers = ResourceOffer.sort(db.findResource(resourceId));

        // TODO

        return null;
    }

    public void offerResource(String resourceId, KawaResource resourceProvider) {
        assert !Kawa.isClientOnlyMode() : "Clients cannot offer resources.";
        db.offerResource(resourceId);
        resourceProviders.put(resourceId, resourceProvider);
    }

    public void unofferResource(String resourceId) {
        resourceProviders.remove(resourceId);
        db.unofferResource(resourceId);
    }

    public static boolean isClientOnlyMode() {
        return thisAddress == null;
    }

}
