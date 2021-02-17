package rsocket.client;

import java.time.Duration;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Component;

import io.rsocket.transport.netty.client.WebsocketClientTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Component
public class BackendService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String id = "test1";
    private final String backendUrl = "ws://localhost:9898";

    private final RSocketRequester requester;
    private final Disposable telemetrySubscriber;

    @PreDestroy
    public void destroy() {
        telemetrySubscriber.dispose();
        requester.rsocketClient().dispose();
    }

    public BackendService(RSocketRequester.Builder builder, RSocketStrategies strategies) {
        logger.info("id: {}, backend URL: {}", id, backendUrl);

        HttpClient httpClient = HttpClient.create()
                .baseUrl(backendUrl);

        requester = builder
                .setupRoute("/backend/connect")
                .setupData(id)
                .rsocketConnector(connector -> connector.reconnect(Retry.fixedDelay(Integer.MAX_VALUE, Duration.ofSeconds(1))))
                .dataMimeType(MediaType.APPLICATION_JSON)
                .transport(WebsocketClientTransport.create(httpClient, "/rsocket"));

        telemetrySubscriber = Flux.interval(Duration.ofSeconds(10))
            .log()
            .onBackpressureDrop()
            .map(tick -> "Telemetry from " + id + " tick " + tick)
            .as(flux -> requester.route("/backend/telemetry").data(flux).retrieveFlux(Void.class))
            .subscribe();

    }

}
