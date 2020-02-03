package mongo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
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
