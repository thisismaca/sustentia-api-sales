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

@RestController
@RequestMapping(path = "api-sales/v1/subscription")
public class SubscriptionController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResponseEntity<String> createProducts(@RequestBody Subscription subscription) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        Customer customer = new Customer(subscription.getName(), subscription.getEmail(), subscription.getExternalId());
        map.add("apiKey", System.getenv("FLOW-API-KEY"));
        map.add("name", subscription.getName());
        map.add("email", subscription.getEmail());
        map.add("externalId", subscription.getExternalId());
        map.add("s", customer.getS());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
//        HttpEntity<Customer> customerHttpEntity = new HttpEntity<Customer>(customer);
//        ResponseEntity customerResponse = restTemplate.exchange("https://sandbox.flow.cl/api/plans/create", HttpMethod.POST, customerHttpEntity, String.class);
//        return restTemplate.exchange("https://sandbox.flow.cl/api/customer/create", HttpMethod.POST, customerHttpEntity, Customer.class).getBody();
        return restTemplate.postForEntity(
                "https://sandbox.flow.cl/api/customer/create", request , String.class);
    }

}
