package cl.sustentia.apisales;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FlowInvoice {

    @JsonProperty("id")
    private String id;
    @JsonProperty("status")
    private int status;

    public String getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }
}
