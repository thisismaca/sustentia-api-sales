package cl.sustentia.apisales;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreditRegistrationURL {

    @JsonProperty("url")
    private String url;
    @JsonProperty("token")
    private String token;

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }
}
