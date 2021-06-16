package cl.sustentia.apisales;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FlowInvoice {

    @JsonProperty("id")
    private String id;
    @JsonProperty("status")
    private int status;
    @JsonProperty("paymentLink")
    private String paymentLink;

    public String getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public String getPaymentLink() {
        return paymentLink;
    }
}
