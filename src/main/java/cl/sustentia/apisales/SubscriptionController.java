package cl.sustentia.apisales;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;
import java.util.TreeSet;

@RestController
@RequestMapping(path = "api-sales/v1/subscription")
public class SubscriptionController {

    @Autowired
    private RestTemplate restTemplate;

    private final SubscriptionRecordRepository subscriptionRecordRepository;

    @Autowired
    public SubscriptionController(SubscriptionRecordRepository subscriptionRecordRepository) {
        this.subscriptionRecordRepository = subscriptionRecordRepository;
    }

    @CrossOrigin(origins = "http://localhost:23930")
    @PostMapping("/get")
    public ResponseEntity<SubscriptionRecord> getSubscription(@RequestBody SubscriptionRecord subscriptionRecord) {
        var localSubscription = subscriptionRecordRepository.findById(subscriptionRecord.getStoreId());
        if(localSubscription.isEmpty()) return ResponseEntity.status(HttpStatus.OK).build();

        var flowSubscription = getFlowSubscription(localSubscription.get().getSubscriptionId());
        if(!flowSubscription.getStatusCode().is2xxSuccessful()) return ResponseEntity.status(flowSubscription.getStatusCode()).build();

        if(flowSubscription.getBody() == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        boolean isPaidInFlow = flowSubscription.getBody().getInvoices().get(0).getStatus() == 1;

        if(localSubscription.get().isPaid() != isPaidInFlow) {
            localSubscription.get().setPaid(isPaidInFlow);
            if(!isPaidInFlow) localSubscription.get().setPaymentLink(flowSubscription.getBody().getInvoices().get(0).getPaymentLink());
            return ResponseEntity.status(HttpStatus.OK).body(subscriptionRecordRepository.save(localSubscription.get()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(localSubscription.get());
    }

    @CrossOrigin(origins = "http://localhost:23930")
    @PostMapping(value = "/register")
    public ResponseEntity<SubscriptionRecord> register(@RequestBody Subscription subscription) {
        ResponseEntity<Customer> customerResponseEntity = addCustomer(subscription);
        String customerId = "";

        if(!customerResponseEntity.getStatusCode().is2xxSuccessful()) { //Response is other than 200, client already exists is code 401
            return ResponseEntity.status(customerResponseEntity.getStatusCode()).build();
        }
        if (customerResponseEntity.getBody() != null) { //Response is 200 and has body
            customerId = customerResponseEntity.getBody().getCustomerId();
        }

        ResponseEntity<FlowSubscription> subscriptionResponse = subscribe(customerId, subscription.getPlanId());

        if(subscriptionResponse.getStatusCode().is2xxSuccessful() && subscriptionResponse.getBody() != null) {
            boolean paymentConfirmed = subscriptionResponse.getBody().getInvoices().get(0).getStatus() == 1;
            String invoiceId = subscriptionResponse.getBody().getInvoices().get(0).getId();
            String paymentLink = getInvoiceLink(invoiceId);
            if (paymentLink == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            SubscriptionRecord subscriptionRecord = new SubscriptionRecord(subscription.getStoreId(), customerId, subscriptionResponse.getBody().getSubscriptionId(), subscription.getPlanId(), paymentConfirmed, paymentLink);
            return ResponseEntity.status(HttpStatus.OK).body(subscriptionRecordRepository.save(subscriptionRecord));
        } else {
            return ResponseEntity.status(subscriptionResponse.getStatusCode()).build();
        }
    }

//    @RequestMapping(value = "/register/result", method = RequestMethod.POST)
//    public String registerResult(@RequestBody String token) {
//        return token;
//    }

    String getInvoiceLink(String invoiceId) {
        String paramsUrl = "";
        try {
            paramsUrl = "apiKey=" + System.getenv("FLOW-API-KEY") + "&invoiceId=" + invoiceId + "&s=" + sign("apiKey"+System.getenv("FLOW-API-KEY")+"invoiceId"+invoiceId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        var invoiceRequest = restTemplate.getForEntity(
                "https://sandbox.flow.cl/api/invoice/get?" + paramsUrl, FlowInvoice.class);
        if (invoiceRequest.getStatusCode().is2xxSuccessful() && invoiceRequest.getBody() != null)
        return invoiceRequest.getBody().getPaymentLink();
        else return null;
    }

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

    ResponseEntity<FlowSubscription> getFlowSubscription(String subscriptionId) {
        String paramsUrl = "";
        try {
            paramsUrl = "apiKey=" + System.getenv("FLOW-API-KEY") + "&subscriptionId=" + subscriptionId + "&s=" + sign("apiKey"+System.getenv("FLOW-API-KEY")+"subscriptionId"+subscriptionId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return restTemplate.getForEntity(
                "https://sandbox.flow.cl/api/subscription/get?" + paramsUrl, FlowSubscription.class);
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
        customerMap.add("externalId", subscription.getStoreId());
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
        return String.format("%064x", new BigInteger(1, HMAC.calcHmacSha256(System.getenv("FLOW-SECRET-KEY").getBytes(StandardCharsets.UTF_8), message.getBytes(StandardCharsets.UTF_8))));
    }
}
