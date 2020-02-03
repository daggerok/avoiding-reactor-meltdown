package daggerok;

import lombok.Value;
import lombok.With;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.ReactorBlockHoundIntegration;
import reactor.tools.agent.ReactorDebugAgent;

import java.util.function.Supplier;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        BlockHound.install();
        ReactorDebugAgent.init();
        SpringApplication.run(Main.class, args);
    }
}

@With
@Value
@Document
// @AllArgsConstructor(onConstructor_ = @PersistenceConstructor)
class Name {

    @Id
    private final String id;
    private final String value;

    public static Name of(String value) {
        return new Name(null, value);
    }
}

interface NameRepository extends ReactiveMongoRepository<Name, String> { }

@Log4j2
@Configuration
class NameTestData {

    @Bean
    Supplier<Flux<Name>> names() {
        return () -> Flux.just("Maksimko", "Maks", "Max", "Daggerok")
                         .map(Name::of);
    }

    @Bean
    InitializingBean postConstruct(NameRepository nameRepository) {
        // return () -> nameRepository.deleteAll()
        //                            .thenMany(nameRepository.saveAll(names().get()))
        //                            .thenMany(nameRepository.findAll())
        //                            .subscribe(log::info);
        return () -> nameRepository.deleteAll()
                                   .thenMany(names().get().concatMap(nameRepository::save))
                                   .thenMany(nameRepository.findAll())
                                   .subscribe(log::info);
    }
}
