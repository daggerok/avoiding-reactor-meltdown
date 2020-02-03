package mongo;

import lombok.Value;
import lombok.With;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootApplication
public class MongoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MongoApplication.class,
                              Stream.concat(Arrays.stream(args),
                                            Stream.of("--server.port=0",
                                                      "--spring.data.mongodb.port=27017",
                                                      "--spring.output.ansi.enabled=always"))
                                    .toArray(String[]::new));
    }
}

@With
@Value
@Document
class ServerData {

    @Id
    private final String id;
    private final Map<String, String> data;
}

interface ServerDataRepository extends ReactiveMongoRepository<ServerData, String> {
}

@Log4j2
@Configuration
class ServerInfoDataPopulation {

    @Bean
    InitializingBean init(Environment environment,
                          ServerDataRepository repository) {
        return () -> repository.deleteAll()
                               .then(Mono.just(Map.of("serverPort", environment.getProperty("server.port", "undefined"),
                                                      "activeProfiles", String.join(", ", environment.getActiveProfiles())))
                                         .map(data -> new ServerData(null, data))
                                         .flatMap(repository::save))
                               .thenMany(repository.findAll())
                               .subscribe(log::info);
    }
}
