package cl.sustentia.apisales;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FlowSubscription {

    @JsonProperty("subscriptionId")
    private String subscriptionId;
    @JsonProperty("customerId")
    private String customerId;
    @JsonProperty("status")
    private int status;
    @JsonProperty("morose")
    private int morose;


    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getStatus() {
        return status;
    }

    public int getMorose() {
        return morose;
    }
}
