package co.casterlabs.kawa;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import com.esotericsoftware.kryonet.Server;

import co.casterlabs.kawa.databases.KawaDB;
import co.casterlabs.kawa.networking.KawaNetwork;
import lombok.Getter;
import lombok.Setter;

public class Kawa {
    private static @Getter @Nullable String thisAddress;
    private static @Setter String password;
    private static @Setter KawaDB db;

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

//    public ActiveLine getResource(String resourceId) {
//        List<String> addresses
//    }

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
