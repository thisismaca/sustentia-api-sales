package cl.sustentia.apisales;

public class Customer {

    private final String customerId;
    private final String created;
    private final String email;
    private final String name;
    private final String pay_mode;
    private final String creditCardType;
    private final String last4CardDigits;
    private final String externalId;
    private final String status;
    private final String registerDate;

    public Customer(String customerId, String created, String email, String name, String pay_mode, String creditCardType, String last4CardDigits, String externalId, String status, String registerDate) {
        this.customerId = customerId;
        this.created = created;
        this.email = email;
        this.name = name;
        this.pay_mode = pay_mode;
        this.creditCardType = creditCardType;
        this.last4CardDigits = last4CardDigits;
        this.externalId = externalId;
        this.status = status;
        this.registerDate = registerDate;
    }

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
