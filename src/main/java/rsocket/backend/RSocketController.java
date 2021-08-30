package rsocket.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class RSocketController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ConnectMapping("/backend/connect")
    public Mono<Void> connect(RSocketRequester requester, @Payload String id) {
        requester.rsocket()
            .onClose()
            .doFirst(() -> {
                logger.info("Connected {}", id);
            })
            .doOnError(error -> logger.warn("Error {}: {}" + id, error.getMessage()))
            .doFinally(consumer -> {
                logger.info("Disconnected {}", id);
            })
            .subscribe();

        return Mono.empty();
    }

    @MessageMapping("/backend/telemetry")
    public Mono<Void> telemetry(String message) {
        logger.info("Telemetry received: " + message);
        return Mono.empty();
    }

}
