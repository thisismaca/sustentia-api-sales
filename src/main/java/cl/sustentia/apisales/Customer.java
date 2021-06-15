package cl.sustentia.apisales;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {

    @JsonProperty("customerId")
    private String customerId;
    @JsonProperty("created")
    private String created;
    @JsonProperty("email")
    private String email;
    @JsonProperty("name")
    private String name;
    @JsonProperty("pay_mode")
    private String pay_mode;
    @JsonProperty("creditCardType")
    private String creditCardType;
    @JsonProperty("last4CardDigits")
    private String last4CardDigits;
    @JsonProperty("externalId")
    private String externalId;
    @JsonProperty("status")
    private String status;
    @JsonProperty("registerDate")
    private String registerDate;

    public String getCustomerId() {
        return customerId;
    }

    public String getCreated() {
        return created;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPay_mode() {
        return pay_mode;
    }

    public String getCreditCardType() {
        return creditCardType;
    }

    public String getLast4CardDigits() {
        return last4CardDigits;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getStatus() {
        return status;
    }

    public String getRegisterDate() {
        return registerDate;
    }
}
