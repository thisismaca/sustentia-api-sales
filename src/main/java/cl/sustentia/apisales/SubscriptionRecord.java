package cl.sustentia.apisales;

import com.google.cloud.firestore.annotation.DocumentId;

public class SubscriptionRecord {

    @DocumentId
    private String storeId;
    private String flowCustomerId;
    private String subscriptionId;
    private String planId;
    private boolean paid;
    private String end_date;
    private String paymentLink;
    private String timestamp;

    public SubscriptionRecord() {
    }

    public SubscriptionRecord(String storeId, String flowCustomerId, String subscriptionId, String planId, boolean paid, String end_date, String paymentLink, String timestamp) {
        this.storeId = storeId;
        this.flowCustomerId = flowCustomerId;
        this.subscriptionId = subscriptionId;
        this.paid = paid;
        this.planId = planId;
        this.end_date = end_date;
        this.paymentLink = paymentLink;
        this.timestamp = timestamp;
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

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
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

    public String getTimestamp() {
        return timestamp;
    }
}
