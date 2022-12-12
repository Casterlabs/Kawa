package co.casterlabs.kawa.databases;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import lombok.ToString;

@ToString
public class ResourceOffer {
    private static final Comparator<ResourceOffer> comparator = (o1, o2) -> Integer.compare(o1.numberOfClients, o2.numberOfClients);

    public String resourceId;
    public String address;
    public long offeredAt;
    public int numberOfClients;
    public int maxNumberOfClients;
    public boolean isSaturated = false;

    public boolean isExpired() {
        return (System.currentTimeMillis() - this.offeredAt) > KawaDB.OFFER_TIMEOUT;
    }

    public static List<ResourceOffer> sort(List<ResourceOffer> offers) {
        List<ResourceOffer> unsaturated = new LinkedList<>();
        List<ResourceOffer> saturated = new LinkedList<>();

        for (ResourceOffer offer : offers) {
            if (offer.isSaturated) {
                saturated.add(offer);
            } else {
                unsaturated.add(offer);
            }
        }

        unsaturated.sort(comparator);
        saturated.sort(comparator);

        // Janky way of sorting by numberOfClients AND by isSaturated.
        ArrayList<ResourceOffer> sorted = new ArrayList<>(unsaturated.size() + saturated.size());
        offers.addAll(unsaturated);
        offers.addAll(saturated);

        return sorted;
    }

}
