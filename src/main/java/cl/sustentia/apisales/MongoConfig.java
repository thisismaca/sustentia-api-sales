package cl.sustentia.apisales;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;


@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "sustentia-db";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {

        String username = System.getenv("MDB-STORE-USR");
        String password = System.getenv("MDB-STORE-PASS");
        ConnectionString connString = new ConnectionString(
                "mongodb+srv://"+username+":"+password+"@cluster0.kbccf.mongodb.net/sustentia-db?ssl=true&w=majority"
        );
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .retryWrites(true)
                .retryReads(true)
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

}