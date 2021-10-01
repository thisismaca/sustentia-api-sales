package cl.sustentia.apisales;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class SubscriptionRepositoryImpl implements FirestoreSubscriptionRepository {

    private final Firestore db;

    @Autowired
    public SubscriptionRepositoryImpl(Firestore db) {
        this.db = db;
    }

    @Override
    public SubscriptionRecord save(SubscriptionRecord subscriptionRecord) {
        ApiFuture<WriteResult> future = db.collection("subscriptions").document(subscriptionRecord.getStoreId()).set(subscriptionRecord);
        if (future.isCancelled()) return null;
        else return subscriptionRecord;
    }

    @Override
    public SubscriptionRecord get(String storeId) {
        SubscriptionRecord subscription = null;
        try {
            DocumentReference documentReference = db.collection("subscriptions").document(storeId);
            ApiFuture<DocumentSnapshot> future = documentReference.get();
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                subscription = document.toObject(SubscriptionRecord.class);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return subscription;
    }

    @Override
    public List<SubscriptionRecord> getAll() {
        List<SubscriptionRecord> subscriptions = new ArrayList<>();
        try {
            ApiFuture<QuerySnapshot> future = db.collection("subscriptions").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot document : documents) {
                subscriptions.add(document.toObject(SubscriptionRecord.class));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return subscriptions;
    }

    @Override
    public boolean delete(String storeId) {
        ApiFuture<WriteResult> writeResult = db.collection("subscriptions").document(storeId).delete();
        return !writeResult.isCancelled();
    }
}