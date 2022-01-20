package rsocket.client;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class BackendController {

    @MessageMapping("ping")
    public Mono<String> ping(String data, RSocketRequester requester) {
        return Mono.just("Response " + data);
    }
}
