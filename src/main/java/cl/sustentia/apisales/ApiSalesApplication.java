package cl.sustentia.apisales;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;


@SpringBootApplication
public class ApiSalesApplication {

    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiSalesApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/get").allowedOrigins("http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl").allowedMethods("GET", "POST");
                registry.addMapping("/register").allowedOrigins("http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl").allowedMethods("GET", "POST");
                registry.addMapping("/upgrade").allowedOrigins("http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl").allowedMethods("GET", "POST");
                registry.addMapping("/cancel").allowedOrigins("http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl").allowedMethods("GET", "POST");
                registry.addMapping("/delete").allowedOrigins("http://localhost:23930", "https://sustentia.cl", "https://www.sustentia.cl").allowedMethods("POST");
            }
        };
    }

    @Bean
    public static Firestore initFirebaseFirestore() {
        try {
            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId(FirestoreOptions.getDefaultProjectId())
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
            return firestoreOptions.getService();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}














