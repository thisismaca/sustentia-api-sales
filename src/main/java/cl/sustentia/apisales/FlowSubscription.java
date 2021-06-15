package cl.sustentia.apisales;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FlowSubscription {

    @JsonProperty("subscriptionId")
    private String subscriptionId;
    @JsonProperty("customerId")
    private String customerId;
    @JsonProperty("status")
    private String status;
    @JsonProperty("morose")
    private String morose;


    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public String getMorose() {
        return morose;
    }
}
