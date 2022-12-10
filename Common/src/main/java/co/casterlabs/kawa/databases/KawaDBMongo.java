package co.casterlabs.kawa.databases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jongo.Jongo;
import org.jongo.MongoCursor;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.kawa.Kawa;
import lombok.NonNull;

public class KawaDBMongo implements KawaDB {
    private Map<String, AsyncTask> resourceKA = new HashMap<>(); // When we offer resources we must refresh them periodically.
    private MongoClient mongo;
    private Jongo jongo;

    @SuppressWarnings("deprecation")
    public KawaDBMongo(@NonNull String mongoUri) {
        this.mongo = new MongoClient(new MongoClientURI(mongoUri));
        this.jongo = new Jongo(this.mongo.getDB("kawa"));
    }

    public List<String> findResource(String resourceId) {
        try (MongoCursor<ResourceOffer> cursor = this.jongo.getCollection("resources")
            .find("{ resourceId: # }", resourceId)
            .as(ResourceOffer.class)) {
            ArrayList<String> addresses = new ArrayList<>(cursor.count());

            while (cursor.hasNext()) {
                ResourceOffer offer = cursor.next();
                if (offer.isExpired()) continue;

                addresses.add(offer.address);
            }

            return addresses;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void offerResource(String resourceId) {
        assert !Kawa.isClientOnlyMode() : "Clients cannot offer resources.";
        assert !this.resourceKA.containsKey(resourceId) : "Duplicate resource for this instance.";

        this.resourceKA.put(
            resourceId,
            AsyncTask.create(() -> {
                while (true) {
                    this.jongo
                        .getCollection("resources")
                        .update("{ resourceId: #, address: # }", resourceId, Kawa.getThisAddress())
                        .upsert()
                        .with("{ offeredAt: # }", System.currentTimeMillis());

                    try {
                        Thread.sleep(OFFER_TIMEOUT / 2);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            })
        );
    }

    public void unofferResource(String resourceId) {
        AsyncTask ka = this.resourceKA.remove(resourceId);
        if (ka != null) ka.cancel();
    }

    @Override
    public void close() {
        this.mongo.close();
    }

}
