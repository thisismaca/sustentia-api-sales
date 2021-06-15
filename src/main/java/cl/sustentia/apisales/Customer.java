package cl.sustentia.apisales;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

public class Customer {

    private String apiKey;
    private String name;
    private String email;
    private String externalId;
    private String s;

    public Customer(String name, String email, String externalId) {
        this.apiKey = System.getenv("FLOW-API-KEY");
        this.name = name;
        this.email = email;
        this.externalId = externalId;
        try {
            this.s = String.format("%064x", new BigInteger(1, HMAC.calcHmacSha256(System.getenv("FLOW-SECRET-KEY").getBytes("UTF-8"), getMessageToSign().getBytes("UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    String getMessageToSign() {
        return "apiKey"+System.getenv("FLOW-API-KEY")+"email"+email+"externalId"+externalId+"name"+name;
    }

    public String getApiKey() {
        return apiKey;
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

    public String getS() {
        return s;
    }
}
