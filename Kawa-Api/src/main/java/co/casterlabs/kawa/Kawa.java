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
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class Kawa {
    public static final FastLogger LOGGER = new FastLogger();

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
        KawaNetwork.startServer(thisAddress, password, resourceProviders);
        LOGGER.debug("Listening on: %s:%d", thisAddress, KawaNetwork.KAWA_PORT_TCP);
    }

    public static Line getResource(String resourceId, Line.Listener listener, boolean tryNextIfFailed) throws IOException {
        LOGGER.debug("Attempting to retrieve resource: %s (tryNextIfFailed=%b)", resourceId, tryNextIfFailed);

        List<ResourceOffer> offers = ResourceOffer.sort(db.findResource(resourceId));
        IOException exception = new IOException("No offers.");

        for (ResourceOffer offer : offers) {
            LOGGER.debug("Trying resource: %s", offer);

            try {
                return KawaNetwork.openLine(offer.address, password, resourceId, listener);
            } catch (IOException e) {
                exception.addSuppressed(e);

                if (!tryNextIfFailed) break;
            }
        }
        throw exception;
    }

    public static void offerResource(String resourceId, KawaResource resourceProvider) {
        assert !Kawa.isClientOnlyMode() : "Clients cannot offer resources.";
        db.offerResource(resourceId);
        resourceProviders.put(resourceId, resourceProvider);
        LOGGER.debug("Offering: %s (with %s)", resourceId, resourceProvider);
    }

    public static void unofferResource(String resourceId) {
        resourceProviders.remove(resourceId);
        db.unofferResource(resourceId);
        LOGGER.debug("Un-offering: %s", resourceId);
    }

    public static boolean isClientOnlyMode() {
        return thisAddress == null;
    }

}
