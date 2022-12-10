package co.casterlabs.kawa.databases;

public class ResourceOffer {
    public String resourceId;
    public String address;
    public long offeredAt;
    public int numberOfClients;
    public boolean isSaturated = false;

    public boolean isExpired() {
        return (System.currentTimeMillis() - this.offeredAt) > KawaDB.OFFER_TIMEOUT;
    }

}
