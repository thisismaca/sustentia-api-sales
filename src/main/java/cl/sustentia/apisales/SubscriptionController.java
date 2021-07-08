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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
        boolean isPaidInFlow = flowSubscription.getBody().getStatus() == 1 && flowSubscription.getBody().getMorose() == 0;

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

        ResponseEntity<FlowSubscription> subscriptionResponse = subscribe(customerId, subscription.getPlanId(), null);

        return storeSubscription(subscriptionResponse, subscription.getStoreId(), customerId, subscription.getPlanId());
    }

    @CrossOrigin(origins = "http://localhost:23930")
    @PostMapping(value = "/upgrade")
    public ResponseEntity<SubscriptionRecord> upgrade(@RequestBody UpgradeSubscriptionRecord subscriptionRecord) {
        try {
            var cancelResponse = cancelSubscription(subscriptionRecord.getSubscriptionId(), true);
            if(!cancelResponse.getStatusCode().is2xxSuccessful()) return ResponseEntity.status(cancelResponse.getStatusCode()).build();
            subscriptionRecordRepository.deleteById(subscriptionRecord.getStoreId());
            ResponseEntity<FlowSubscription> subscriptionResponse;
            if(subscriptionRecord.isPaid()) {
                subscriptionResponse = subscribe(subscriptionRecord.getFlowCustomerId(), subscriptionRecord.getPlanId(), subscriptionRecord.getCouponId());
            } else {
                subscriptionResponse = subscribe(subscriptionRecord.getFlowCustomerId(), subscriptionRecord.getPlanId(), null);
            }
            return storeSubscription(subscriptionResponse, subscriptionRecord.getStoreId(), subscriptionRecord.getFlowCustomerId(), subscriptionRecord.getPlanId());
        } catch (Exception e) {
            return  ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private ResponseEntity<SubscriptionRecord> storeSubscription(ResponseEntity<FlowSubscription> flowSubscription, String storeId, String customerId, String planId) {
        /*
            As soon as the subscription is created subscription status is "paid"
            (status = 1, morose = 0), which is not true. This case works fine when
            getting a subscription through the /get endpoint.
            So, instead the real status

            boolean paymentConfirmed = subscriptionResponse.getBody().getInvoices().get(0).getStatus() == 1;
             */
        if (flowSubscription.getStatusCode().is2xxSuccessful() && flowSubscription.getBody() != null) {
            String invoiceId = flowSubscription.getBody().getInvoices().get(0).getId();
            String paymentLink = getInvoiceLink(invoiceId);
            if (paymentLink == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

            SubscriptionRecord subscriptionRecord = new SubscriptionRecord(storeId, customerId, flowSubscription.getBody().getSubscriptionId(), planId, false, paymentLink, now);
            return ResponseEntity.status(HttpStatus.OK).body(subscriptionRecordRepository.save(subscriptionRecord));
        } else {
            return ResponseEntity.status(flowSubscription.getStatusCode()).build();
        }
    }

    private String getInvoiceLink(String invoiceId) {
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

    ResponseEntity<FlowSubscription> subscribe(String customerId, String planId, String couponId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> subscription = new LinkedMultiValueMap<>();
        subscription.add("apiKey", System.getenv("FLOW-API-KEY"));
        subscription.add("planId", planId);
        subscription.add("customerId", customerId);
        if(couponId != null) subscription.add("couponId", couponId);
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

    ResponseEntity<FlowSubscription> cancelSubscription(String subscriptionId, boolean cancelNow) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> cancelRequest = new LinkedMultiValueMap<>();
        cancelRequest.add("apiKey", System.getenv("FLOW-API-KEY"));
        cancelRequest.add("at_period_end", cancelNow ? "0" : "1");
        cancelRequest.add("subscriptionId", subscriptionId);
        try {
            cancelRequest.add("s", sign(buildMessage(cancelRequest)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(cancelRequest, headers);
        return restTemplate.postForEntity(
                "https://sandbox.flow.cl/api/subscription/cancel", request , FlowSubscription.class);
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
