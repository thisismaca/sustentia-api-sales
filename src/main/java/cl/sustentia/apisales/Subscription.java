package cl.sustentia.apisales;

public class Subscription {
    private final String name;
    private final String email;
    private final String storeId;
    private final String planId;

    public Subscription(String name, String email, String storeId, String planId) {
        this.name = name;
        this.email = email;
        this.storeId = storeId;
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getPlanId() {
        return planId;
    }
}
