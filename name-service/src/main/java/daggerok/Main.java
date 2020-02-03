package daggerok;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
        // return () -> nameRepository.deleteAll().thenMany(nameRepository.saveAll(names().get()))
        //                            .thenMany(nameRepository.findAll()).subscribe(log::info);
        return () -> nameRepository.deleteAll()
                                   .thenMany(names().get().concatMap(nameRepository::save))
                                   .thenMany(nameRepository.findAll())
                                   .subscribe(log::info);
    }
}

/**
 * Let's assume we cannot change or refactor this service...
 */
@Log4j2
@Service
class BlockingService {
    String reverse(String input) {
        try { Thread.sleep(123); }
        catch (InterruptedException e) { log.warn("Yeah! Block me, Baby!", e); }
        return new StringBuilder(Objects.requireNonNull(input)).reverse().toString();
    }
}

/**
 * To handle blocking calls properly, we must create separate scheduler for that
 */
@Configuration
class BlockingCallsConfig {

    @Bean
    Scheduler blockingCallsScheduler() {
        return Schedulers.boundedElastic();
    }
}

/**
 * Now we can tell reactor to perform that blocking call on special scheduled thread.
 * For the simplicity of usage, we are created wrapped service around blocking one.
 */
@Service
@RequiredArgsConstructor
class SolutionByWrappingServiceWithSpecialScheduler {

    private final BlockingService blockingService;
    private final Scheduler blockingCallsScheduler;

    Mono<String> reverse(String input) {
        return Mono.fromCallable(() -> blockingService.reverse(input))
                   .subscribeOn(blockingCallsScheduler)
                   .log();
    }
}

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
class Resources {

    private final SolutionByWrappingServiceWithSpecialScheduler service;

    @GetMapping("/reverse/{string}")
    Mono<Map<String, String>> reverse(@PathVariable String string) {
        return service.reverse(string)
                      .map(s -> Map.of("reversed", s));
    }

    @RequestMapping("/**")
    Map<String, String> fallback(ServerWebExchange exchange) {
        var uri = exchange.getRequest().getURI();
        Function<String, String> url = path -> String.format("%s://%s%s", uri.getScheme(), uri.getAuthority(), path);
        return Map.of("_self", uri.toString(),
                      "reverse string GET", url.apply("/reverse/{string}"));
    }
}
