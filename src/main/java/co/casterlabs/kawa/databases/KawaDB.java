package co.casterlabs.kawa.databases;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface KawaDB extends Closeable {
    public static final long OFFER_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

    /**
     * @return A list of addresses, pick one to connect to. Empty = no offers.
     */
    public List<String> findResource(String resourceId);

    public void offerResource(String resourceId);

    public void unofferResource(String resourceId);

    public static class ResourceOffer {
        public String resourceId;
        public String address;
        public long offeredAt;
        public int numberOfClients;

        public boolean isExpired() {
            return (System.currentTimeMillis() - this.offeredAt) > OFFER_TIMEOUT;
        }

    }

}
