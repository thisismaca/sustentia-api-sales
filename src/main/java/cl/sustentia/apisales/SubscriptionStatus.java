package cl.sustentia.apisales;

public class SubscriptionStatus {

    private final String storeId;
    private final boolean restricted;
    private final int maxProducts;
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
