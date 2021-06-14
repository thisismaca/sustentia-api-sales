package cl.sustentia.apisales;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@RestController
@RequestMapping(path = "api-sales/v1/subscription")
public class SubscriptionController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping(value = "/sample/endpoint", method = RequestMethod.POST)
    public String createProducts(@RequestBody Subscription subscription) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        Customer customer = new Customer(subscription.getName(), subscription.getEmail(), subscription.getExternalId());
        HttpEntity<Customer> customerHttpEntity = new HttpEntity<Customer>(customer, headers);
        ResponseEntity customerResponse = restTemplate.exchange("https://sandbox.flow.cl/api/plans/create", HttpMethod.POST, customerHttpEntity, String.class);
        return null;
    }

}
