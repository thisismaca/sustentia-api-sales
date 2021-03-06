package cl.sustentia.apisales;

import com.google.api.client.util.Key;

public class SubscriptionStatus {

    @Key
    private final String storeId;
    @Key
    private final boolean restricted;
    @Key
    private final int maxProducts;
    @Key
    private final int maxAnnouncements;

    public SubscriptionStatus(String storeId, boolean restricted, int maxProducts, int maxAnnouncements) {
        this.storeId = storeId;
        this.restricted = restricted;
        this.maxProducts = maxProducts;
        this.maxAnnouncements = maxAnnouncements;
    }

    public String getStoreId() {
        return storeId;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public int getMaxProducts() {
        return maxProducts;
    }

    public int getMaxAnnouncements() {
        return maxAnnouncements;
    }
}
