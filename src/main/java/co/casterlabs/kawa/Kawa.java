package co.casterlabs.kawa;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.esotericsoftware.kryonet.Server;

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

    static {
        Kawa.class.getClassLoader().setDefaultAssertionStatus(true);
    }

    public static Server startListening(String thisAddress, int port) throws IOException {
        assert password != null : "You must set the password before listening.";
        Kawa.thisAddress = thisAddress;

        Server server = new Server();
        KawaNetwork.setupKryo(server.getKryo());
        server.addListener(KawaNetwork.listenKryo(password));
        server.start();
        server.bind(port);
        return server;
    }

    public Line getResource(String resourceId) {
        List<ResourceOffer> offers = ResourceOffer.sort(db.findResource(resourceId));

        // TODO

        return null;
    }

    public void offerResource(String resourceId) {
        assert !Kawa.isClientOnlyMode() : "Clients cannot offer resources.";
        db.offerResource(resourceId);
    }

    public void unofferResource(String resourceId) {
        db.unofferResource(resourceId);
    }

    public static boolean isClientOnlyMode() {
        return thisAddress == null;
    }

}
