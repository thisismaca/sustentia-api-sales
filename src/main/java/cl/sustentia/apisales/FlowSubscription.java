package cl.sustentia.apisales;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FlowSubscription {

    @JsonProperty("subscriptionId")
    private String subscriptionId;
    @JsonProperty("customerId")
    private String customerId;
    @JsonProperty("status")
    private int status;
    @JsonProperty("morose")
    private int morose;
    @JsonProperty("invoices")
    private List<FlowInvoice> invoices;


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

    public List<FlowInvoice> getInvoices() {
        return invoices;
    }
}
