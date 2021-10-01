package cl.sustentia.apisales;

import java.util.List;

public interface FirestoreSubscriptionRepository {

    SubscriptionRecord save(SubscriptionRecord subscriptionRecord);
    SubscriptionRecord get(String storeId);
    List<SubscriptionRecord> getAll();
    boolean delete(String storeId);
}