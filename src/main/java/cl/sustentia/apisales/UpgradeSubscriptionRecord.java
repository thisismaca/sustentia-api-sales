package cl.sustentia.apisales;

public class UpgradeSubscriptionRecord extends SubscriptionRecord{

    private final String couponId;

    public UpgradeSubscriptionRecord(String storeId, String flowCustomerId, String subscriptionId, String planId, boolean paid, String end_date, String paymentLink, String timestamp, String couponId) {
        super(storeId, flowCustomerId, subscriptionId, planId, paid, end_date, paymentLink, timestamp);
        this.couponId = couponId;
    }

    public String getCouponId() {
        return couponId;
    }
}
