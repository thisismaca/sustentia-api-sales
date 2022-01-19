package cl.sustentia.apisales;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping(path = "api-sales/v1/subscription")
public class SubscriptionController {

    private final String FLOW_PREFIX = System.getenv("FLOW_URL_PREFIX");//"https://sandbox.flow.cl/";//https://www.flow.cl/

    private final RestTemplate restTemplate;
    private final FirestoreSubscriptionRepository firestoreSubscriptionRepository;

    private final List<Plan> plans = Arrays.asList(
            new Plan("itata40", 40, 3),
            new Plan("diguillin100", 100, 5),
            new Plan("punilla150", 150, 7),
            new Plan("nevados", 7000, 1000),
            new Plan("premium", 7000, 1000));

    @Autowired
    public SubscriptionController(RestTemplate restTemplate, FirestoreSubscriptionRepository firestoreSubscriptionRepository) {
        this.restTemplate = restTemplate;
        this.firestoreSubscriptionRepository = firestoreSubscriptionRepository;
    }

    @PostMapping("/saveSustainable/{storeId}")
    public ResponseEntity<SubscriptionRecord> saveSustainable(@PathVariable String storeId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Santiago"));
        SubscriptionRecord subscriptionRecord = new SubscriptionRecord(storeId, "", "", "sustentable", true, null, "", now.toString());
        return ResponseEntity.status(HttpStatus.OK).body(firestoreSubscriptionRepository.save(subscriptionRecord));
    }

    @GetMapping("/updateStatus")
    public ResponseEntity<List<SubscriptionStatus>> updateSubscriptionStatus() {
        var subscriptions = ResponseEntity.status(HttpStatus.OK).body(firestoreSubscriptionRepository.getAll());
        List<SubscriptionRecord> updatedSubscriptions = new LinkedList<>();
        if (subscriptions.getStatusCode().isError())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        for (SubscriptionRecord subscription : subscriptions.getBody()) {
            if  (subscription.getPlanId().equals("sustentable")) {
                updatedSubscriptions.add(subscription);
                continue;
            }
            var updatedSubscription = updateSubscription(subscription);
            if (updatedSubscription.hasBody() && updatedSubscription.getStatusCode().is2xxSuccessful())
                updatedSubscriptions.add(updatedSubscription.getBody());
            else updatedSubscriptions.add(subscription); //If requests fails add old subscription anyway
        }

        List<SubscriptionStatus> subscriptionStatuses = new LinkedList<>();

        for (SubscriptionRecord updatedRecord : updatedSubscriptions) {
            if (updatedRecord.getEnd_date() != null) {
                var endDate = LocalDate.parse(updatedRecord.getEnd_date(), DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"));
                if (endDate.isBefore(LocalDate.now())) {
                    delete(updatedRecord);
                    continue;
                }
            }
            int planMaxProducts = plans.stream().filter(plan -> plan.getId().equals(updatedRecord.getPlanId())).findFirst().get().getMaxProducts();
            int planMaxAnnouncements = plans.stream().filter(plan -> plan.getId().equals(updatedRecord.getPlanId())).findFirst().get().getMaxAnnouncements();
            if (updatedRecord.isPaid()) {
                subscriptionStatuses.add(new SubscriptionStatus(updatedRecord.getStoreId(), false, planMaxProducts, planMaxAnnouncements));
            } else {
                if (getPaymentHours(getTimestampAsDate(updatedRecord.getTimestamp())) >= 72) {
                    //subscriptionStatuses.add(new SubscriptionStatus(updatedRecord.getStoreId(), true, 20, 0));
                    delete(updatedRecord);
                } else { //Between 72 hour range
                    subscriptionStatuses.add(new SubscriptionStatus(updatedRecord.getStoreId(), true, planMaxProducts, planMaxAnnouncements));
                }
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(subscriptionStatuses);
    }

    long getPaymentHours(ZonedDateTime startDateTime) {
        var now = ZonedDateTime.now(ZoneId.of("America/Santiago"));
        var payDay = LocalDateTime.of(now.getYear(), now.getMonth(), startDateTime.getDayOfMonth() + 1, startDateTime.getHour(), startDateTime.getMinute(), startDateTime.getSecond());
        return ChronoUnit.HOURS.between(payDay, now);
    }

    ResponseEntity<SubscriptionRecord> updateSubscription(SubscriptionRecord subscriptionRecord) {
        SubscriptionRecord updatedRecord = subscriptionRecord;
        var flowSubscription = getFlowSubscription(subscriptionRecord.getSubscriptionId());
        if (!flowSubscription.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(flowSubscription.getStatusCode()).build();
        if (!flowSubscription.hasBody()) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        List<FlowInvoice> invoices = flowSubscription.getBody().getInvoices();
        FlowInvoice flowLastInvoice = invoices.get(invoices.size() - 1);
        boolean isPaidInFlow = flowSubscription.getBody().getStatus() == 1 && flowSubscription.getBody().getMorose() == 0 && flowLastInvoice.getStatus() == 1;
        String paymentLink = getInvoiceLink(flowLastInvoice.getId());
        if (paymentLink == null && !isPaidInFlow)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        updatedRecord.setPaid(isPaidInFlow);
        updatedRecord.setPaymentLink(paymentLink == null ? "" : paymentLink);
        return ResponseEntity.status(HttpStatus.OK).body(firestoreSubscriptionRepository.save(updatedRecord));
    }

    @CrossOrigin(origins = {"http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl"})
    @PostMapping("/get")
    public ResponseEntity<SubscriptionRecord> getSubscription(@RequestBody SubscriptionRecord subscriptionRecord) {
        var localSubscription = firestoreSubscriptionRepository.get(subscriptionRecord.getStoreId());
        if (localSubscription == null) return ResponseEntity.status(HttpStatus.OK).build();

        var updatedSubscription = updateSubscription(localSubscription);
        if(updatedSubscription.hasBody() && updatedSubscription.getStatusCode().is2xxSuccessful()) {
            updateStoreFrontStatus(updatedSubscription.getBody());
            //TODO: Find out what to do in case the previous request fails
        }
        return updatedSubscription;
    }

    public void updateStoreFrontStatus(SubscriptionRecord updatedSubscription) {
        int planMaxProducts = plans.stream().filter(plan -> plan.getId().equals(updatedSubscription.getPlanId())).findFirst().get().getMaxProducts();
        int planMaxAnnouncements = plans.stream().filter(plan -> plan.getId().equals(updatedSubscription.getPlanId())).findFirst().get().getMaxAnnouncements();

        if (updatedSubscription.isPaid()) {
            var newStatus = new SubscriptionStatus(updatedSubscription.getStoreId(), false, planMaxProducts, planMaxAnnouncements);
            try {
                makePostRequest("https://api-store-ftkdsvve7a-rj.a.run.app/api-store/v1/store-fronts/update/" + newStatus.getStoreId(), newStatus);
            } catch (IOException e) {
                System.out.println("Update status failed for " + newStatus.getStoreId());
            }
        }
    }

    @CrossOrigin(origins = {"http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl"})
    @PostMapping(value = "/register")
    public ResponseEntity<SubscriptionRecord> register(@RequestBody Subscription subscription) {
        ResponseEntity<FlowCustomer> customerResponseEntity = addCustomer(subscription);
        String customerId = "";

        if (!customerResponseEntity.getStatusCode().is2xxSuccessful() || !customerResponseEntity.hasBody()) { //Response is other than 200, client already exists is code 401
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } else {
            customerId = customerResponseEntity.getBody().getCustomerId();
        }

        ResponseEntity<FlowSubscription> subscriptionResponse = subscribe(customerId, subscription.getPlanId(), null);

        return storeSubscription(subscriptionResponse, subscription.getStoreId(), customerId, subscription.getPlanId());
    }

    @CrossOrigin(origins = {"http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl"})
    @PostMapping(value = "/upgrade")
    public ResponseEntity<SubscriptionRecord> upgrade(@RequestBody UpgradeSubscriptionRecord subscriptionRecord) {
        try {
            var cancelResponse = cancelSubscription(subscriptionRecord.getSubscriptionId(), true);
            if (!cancelResponse.getStatusCode().is2xxSuccessful())
                return ResponseEntity.status(cancelResponse.getStatusCode()).build();

            List<FlowInvoice> invoices = cancelResponse.getBody().getInvoices();
            for (FlowInvoice flowInvoice : invoices) {
                if (flowInvoice.getStatus() == 0) {
                    var invoiceId = flowInvoice.getId();
                    var cancelInvoiceResponse = cancelInvoiceRequest(invoiceId);
                    if (!cancelInvoiceResponse.getStatusCode().is2xxSuccessful())
                        return ResponseEntity.status(cancelInvoiceResponse.getStatusCode()).build();
                }
            }

            firestoreSubscriptionRepository.delete(subscriptionRecord.getStoreId());

            ResponseEntity<FlowSubscription> subscriptionResponse;
            if (subscriptionRecord.isPaid()) {
                subscriptionResponse = subscribe(subscriptionRecord.getFlowCustomerId(), subscriptionRecord.getPlanId(), subscriptionRecord.getCouponId());
            } else {
                subscriptionResponse = subscribe(subscriptionRecord.getFlowCustomerId(), subscriptionRecord.getPlanId(), null);
            }
            return storeSubscription(subscriptionResponse, subscriptionRecord.getStoreId(), subscriptionRecord.getFlowCustomerId(), subscriptionRecord.getPlanId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    //Cancellation by the end of the period. Only applies to paid subscriptions
    @CrossOrigin(origins = {"http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl"})
    @PostMapping(value = "/cancel")
    public ResponseEntity<SubscriptionRecord> cancel(@RequestBody SubscriptionRecord subscriptionRecord) {
        try {
            var cancelResponse = cancelSubscription(subscriptionRecord.getSubscriptionId(), false);
            if (!cancelResponse.getStatusCode().is2xxSuccessful() || !cancelResponse.hasBody())
                return ResponseEntity.status(cancelResponse.getStatusCode()).build();
            var updatedSubscription = subscriptionRecord;
            updatedSubscription.setEnd_date(cancelResponse.getBody().getPeriod_end());
            return ResponseEntity.status(HttpStatus.OK).body(firestoreSubscriptionRepository.save(updatedSubscription));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    //Instant cancellation and elimination of records.
    @CrossOrigin(origins = {"http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl"})
    @PostMapping(value = "/delete")
    public ResponseEntity<Boolean> delete(@RequestBody SubscriptionRecord subscriptionRecord) {
        try {
            var cancelSubscriptionResponse = cancelSubscription(subscriptionRecord.getSubscriptionId(), true);
            if (!cancelSubscriptionResponse.getStatusCode().is2xxSuccessful() || !cancelSubscriptionResponse.hasBody())
                return ResponseEntity.status(cancelSubscriptionResponse.getStatusCode()).build();

            List<FlowInvoice> invoices = cancelSubscriptionResponse.getBody().getInvoices();
            for (FlowInvoice flowInvoice : invoices) {
                if (flowInvoice.getStatus() == 0) { //Cancel only when invoice is pendant, or else it will throw 400. This statement should always be true in case of STA-76
                    var invoiceId = flowInvoice.getId();
                    var cancelInvoiceResponse = cancelInvoiceRequest(invoiceId);
                    if (!cancelInvoiceResponse.getStatusCode().is2xxSuccessful())
                        return ResponseEntity.status(cancelInvoiceResponse.getStatusCode()).build();
                }
            }
            var deleteCustomerResponse = deleteCustomerRequest(subscriptionRecord.getFlowCustomerId());
            if (!deleteCustomerResponse.getStatusCode().is2xxSuccessful() || !deleteCustomerResponse.hasBody())
                return ResponseEntity.status(deleteCustomerResponse.getStatusCode()).build();
            firestoreSubscriptionRepository.delete(subscriptionRecord.getStoreId());
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //Instant cancellation and elimination of records when deleting Store.
    @DeleteMapping(value = "/delete/{storeId}")
    public ResponseEntity<Boolean> delete(@PathVariable String storeId) {
        var subscriptionRecord = firestoreSubscriptionRepository.get(storeId);
        if(subscriptionRecord != null) {
            return delete(subscriptionRecord);
        } else {
            return ResponseEntity.ok(true);
        }
    }

    private ResponseEntity<SubscriptionRecord> storeSubscription(ResponseEntity<FlowSubscription> flowSubscription, String storeId, String customerId, String planId) {
        /*
            As soon as the subscription is created subscription status is "paid"
            (status = 1, morose = 0), which is not true. This case works fine when
            getting a subscription through the /get endpoint.
             */
        if (flowSubscription.getStatusCode().is2xxSuccessful() && flowSubscription.getBody() != null) {
            List<FlowInvoice> invoices = flowSubscription.getBody().getInvoices();
            FlowInvoice flowLastInvoice = invoices.get(invoices.size() - 1);
            String paymentLink = getInvoiceLink(flowLastInvoice.getId());
            if (paymentLink == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Santiago"));

            SubscriptionRecord subscriptionRecord = new SubscriptionRecord(storeId, customerId, flowSubscription.getBody().getSubscriptionId(), planId, false, null, paymentLink, now.toString());
            return ResponseEntity.status(HttpStatus.OK).body(firestoreSubscriptionRepository.save(subscriptionRecord));
        } else {
            return ResponseEntity.status(flowSubscription.getStatusCode()).build();
        }
    }

    private String getInvoiceLink(String invoiceId) {
        String paramsUrl = "";
        try {
            paramsUrl = "apiKey=" + System.getenv("FLOW_API_KEY") + "&invoiceId=" + invoiceId + "&s=" + sign("apiKey" + System.getenv("FLOW_API_KEY") + "invoiceId" + invoiceId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        var invoiceRequest = restTemplate.getForEntity(
                FLOW_PREFIX+ "api/invoice/get?" + paramsUrl, FlowInvoice.class);
        if (invoiceRequest.getStatusCode().is2xxSuccessful() && invoiceRequest.getBody() != null)
            return invoiceRequest.getBody().getPaymentLink();
        else return null;
    }

    ResponseEntity<FlowSubscription> subscribe(String customerId, String planId, String couponId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> subscription = new LinkedMultiValueMap<>();
        subscription.add("apiKey", System.getenv("FLOW_API_KEY"));
        subscription.add("planId", planId);
        subscription.add("customerId", customerId);
        if (couponId != null) subscription.add("couponId", couponId);
        try {
            subscription.add("s", sign(buildMessage(subscription)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(subscription, headers);
        return restTemplate.postForEntity(
                FLOW_PREFIX + "api/subscription/create", request, FlowSubscription.class);
    }

    ResponseEntity<FlowSubscription> getFlowSubscription(String subscriptionId) {
        String paramsUrl = "";
        try {
            paramsUrl = "apiKey=" + System.getenv("FLOW_API_KEY") + "&subscriptionId=" + subscriptionId + "&s=" + sign("apiKey" + System.getenv("FLOW_API_KEY") + "subscriptionId" + subscriptionId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return restTemplate.getForEntity(
                FLOW_PREFIX + "api/subscription/get?" + paramsUrl, FlowSubscription.class);
    }

    ResponseEntity<FlowCustomer> addCustomer(Subscription subscription) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> customerMap = new LinkedMultiValueMap<>();
        customerMap.add("apiKey", System.getenv("FLOW_API_KEY"));
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
                FLOW_PREFIX + "api/customer/create", request, FlowCustomer.class);
    }

    ResponseEntity<FlowSubscription> cancelSubscription(String subscriptionId, boolean cancelNow) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> cancelRequest = new LinkedMultiValueMap<>();
        cancelRequest.add("apiKey", System.getenv("FLOW_API_KEY"));
        cancelRequest.add("at_period_end", cancelNow ? "0" : "1");
        cancelRequest.add("subscriptionId", subscriptionId);
        try {
            cancelRequest.add("s", sign(buildMessage(cancelRequest)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(cancelRequest, headers);
        return restTemplate.postForEntity(
                FLOW_PREFIX + "api/subscription/cancel", request, FlowSubscription.class);
    }

    ResponseEntity<FlowInvoice> cancelInvoiceRequest(String invoiceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> cancelRequest = new LinkedMultiValueMap<>();
        cancelRequest.add("apiKey", System.getenv("FLOW_API_KEY"));
        cancelRequest.add("invoiceId", invoiceId);
        try {
            cancelRequest.add("s", sign(buildMessage(cancelRequest)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(cancelRequest, headers);
        return restTemplate.postForEntity(
                FLOW_PREFIX + "api/invoice/cancel", request, FlowInvoice.class);
    }

    ResponseEntity<FlowCustomer> deleteCustomerRequest(String customerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> deleteRequest = new LinkedMultiValueMap<>();
        deleteRequest.add("apiKey", System.getenv("FLOW_API_KEY"));
        deleteRequest.add("customerId", customerId);
        try {
            deleteRequest.add("s", sign(buildMessage(deleteRequest)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(deleteRequest, headers);
        return restTemplate.postForEntity(
                FLOW_PREFIX + "api/customer/delete", request, FlowCustomer.class);
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
        return String.format("%064x", new BigInteger(1, HMAC.calcHmacSha256(System.getenv("FLOW_SECRET_KEY").getBytes(StandardCharsets.UTF_8), message.getBytes(StandardCharsets.UTF_8))));
    }

    public static HttpResponse makePostRequest(String serviceUrl, SubscriptionStatus data) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        if (!(credentials instanceof IdTokenProvider)) {
            throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
        }
        IdTokenCredentials tokenCredential =
                IdTokenCredentials.newBuilder()
                        .setIdTokenProvider((IdTokenProvider) credentials)
                        .setTargetAudience(serviceUrl)
                        .build();

        HttpContent content = new JsonHttpContent(new GsonFactory(), data);
        GenericUrl genericUrl = new GenericUrl(serviceUrl);
        HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
        HttpTransport transport = new NetHttpTransport();
        com.google.api.client.http.HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
        return request.execute();
    }


    public ZonedDateTime getTimestampAsDate(String timestamp) {
        try {
            return ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_INSTANT);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
