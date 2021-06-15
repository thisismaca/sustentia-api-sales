package cl.sustentia.apisales;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRecordRepository extends MongoRepository<SubscriptionRecord, String> {
}
