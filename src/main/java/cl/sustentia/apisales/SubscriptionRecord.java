package cl.sustentia.apisales;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "subscriptions")
public class SubscriptionRecord {

    @MongoId
    private final String storeId;
    private final String flowCustomerId;
    private final String subscriptionId;
    private final String planId;
    private boolean paid;
    private String paymentLink;

    public SubscriptionRecord(String storeId, String flowCustomerId, String subscriptionId, String planId, boolean paid, String paymentLink) {
        this.storeId = storeId;
        this.flowCustomerId = flowCustomerId;
        this.subscriptionId = subscriptionId;
        this.paid = paid;
        this.planId = planId;
        this.paymentLink = paymentLink;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getFlowCustomerId() {
        return flowCustomerId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getPlanId() {
        return planId;
    }

    public boolean isPaid() {
        return paid;
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }
}
