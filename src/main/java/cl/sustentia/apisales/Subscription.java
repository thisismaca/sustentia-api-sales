package cl.sustentia.apisales;

public class Subscription {
    private final String name;
    private final String email;
    private final String externalId;
    private final String planId;

    public Subscription(String name, String email, String externalId, String planId) {
        this.name = name;
        this.email = email;
        this.externalId = externalId;
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getPlanId() {
        return planId;
    }
}
