package cl.sustentia.apisales.utils;

public class SubscriptionStatus {

    private boolean restricted;
    private int maxProducts;
    private int maxAnnouncements;

    public SubscriptionStatus(boolean restricted, int maxProducts, int maxAnnouncements) {
        this.restricted = restricted;
        this.maxProducts = maxProducts;
        this.maxAnnouncements = maxAnnouncements;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public int getMaxProducts() {
        return maxProducts;
    }

    public void setMaxProducts(int maxProducts) {
        this.maxProducts = maxProducts;
    }

    public int getMaxAnnouncements() {
        return maxAnnouncements;
    }

    public void setMaxAnnouncements(int maxAnnouncements) {
        this.maxAnnouncements = maxAnnouncements;
    }
}
