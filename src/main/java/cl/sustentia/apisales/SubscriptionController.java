package cl.sustentia.apisales;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.SortedSet;
import java.util.TreeSet;

@RestController
@RequestMapping(path = "api-sales/v1/subscription")
public class SubscriptionController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<FlowSubscription> register(@RequestBody Subscription subscription) {
        ResponseEntity<Customer> customerResponseEntity = addCustomer(subscription);
        return subscribe(customerResponseEntity.getBody().getCustomerId(), subscription.getPlanId());
    }

//    @RequestMapping(value = "/register/result", method = RequestMethod.POST)
//    public String registerResult(@RequestBody String token) {
//        return token;
//    }

    ResponseEntity<FlowSubscription> subscribe(String customerId, String planId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> subscription = new LinkedMultiValueMap<>();
        subscription.add("apiKey", System.getenv("FLOW-API-KEY"));
        subscription.add("planId", planId);
        subscription.add("customerId", customerId);
        try {
            subscription.add("s", sign(buildMessage(subscription)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(subscription, headers);
        return restTemplate.postForEntity(
                "https://sandbox.flow.cl/api/subscription/create", request , FlowSubscription.class);
    }



//    ResponseEntity<CreditRegistrationURL> registerCard(String customerId) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        MultiValueMap<String, String> customerCardRegistration = new LinkedMultiValueMap<>();
//        customerCardRegistration.add("apiKey", System.getenv("FLOW-API-KEY"));
//        customerCardRegistration.add("customerId", customerId);
//        customerCardRegistration.add("url_return", "https://sustentia-gateway-ds0d28il.uc.gateway.dev/api-sales/v1/subscription/register/result");
//        try {
//            customerCardRegistration.add("s", sign(buildMessage(customerCardRegistration)));
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(customerCardRegistration, headers);
//        return restTemplate.postForEntity(
//                "https://sandbox.flow.cl/api/customer/register", request , CreditRegistrationURL.class);
//    }

    ResponseEntity<Customer> addCustomer(Subscription subscription) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> customerMap= new LinkedMultiValueMap<>();
        customerMap.add("apiKey", System.getenv("FLOW-API-KEY"));
        customerMap.add("email", subscription.getEmail());
        customerMap.add("externalId", subscription.getExternalId());
        customerMap.add("name", subscription.getName());
        try {
            customerMap.add("s", sign(buildMessage(customerMap)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(customerMap, headers);
        return restTemplate.postForEntity(
                "https://sandbox.flow.cl/api/customer/create", request , Customer.class);
    }

    private String buildMessage(MultiValueMap<String, String> map) {
        SortedSet<String> keys = new TreeSet<>(map.keySet());
        StringBuilder message = new StringBuilder();
        for (String key : keys) {
            message.append(key).append(map.getFirst(key));
        }
        return message.toString();
    }

    private String sign(String message) throws UnsupportedEncodingException {
        return String.format("%064x", new BigInteger(1, HMAC.calcHmacSha256(System.getenv("FLOW-SECRET-KEY").getBytes("UTF-8"), message.getBytes("UTF-8"))));
    }
}
