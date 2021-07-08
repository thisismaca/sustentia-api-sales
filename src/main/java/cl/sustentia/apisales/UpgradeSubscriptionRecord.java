package cl.sustentia.apisales;

import java.time.ZonedDateTime;

public class UpgradeSubscriptionRecord extends SubscriptionRecord{

    private final String couponId;

    public UpgradeSubscriptionRecord(String storeId, String flowCustomerId, String subscriptionId, String planId, boolean paid, String paymentLink, ZonedDateTime timestamp, String couponId) {
        super(storeId, flowCustomerId, subscriptionId, planId, paid, paymentLink, timestamp);
        this.couponId = couponId;
    }

    public String getCouponId() {
        return couponId;
    }
}
