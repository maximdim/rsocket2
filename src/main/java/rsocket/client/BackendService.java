package rsocket.client;

import io.rsocket.RSocket;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class BackendService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String id = "test1";
    private final String backendUrl = "ws://localhost:9898";

    private final RSocketRequester requester;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    public BackendService(RSocketRequester.Builder builder,
            RSocketStrategies strategies,
            ReactorResourceFactory httpResourceFactory,
            BackendController backendController) {
        logger.info("id: {}, backend URL: {}", id, backendUrl);

        requester = builder
                .setupRoute("/backend/connect")
                .setupData(id)
                .rsocketConnector(connector -> connector
                        .reconnect(Retry.fixedDelay(Integer.MAX_VALUE, Duration.ofSeconds(5)))
                        .acceptor(RSocketMessageHandler.responder(strategies, backendController))
                )
                .dataMimeType(MediaType.APPLICATION_JSON)
                .transport(() -> {
                    // httpClient is immutable - baseUrl() creates a copy
                    ClientTransport t = WebsocketClientTransport.create(
                            HttpClient.create().baseUrl(backendUrl), // httpClient.compress(true),
                            "/rsocket")
                            .webSocketSpec(configurer -> configurer
                                    .compress(true)
                                    .maxFramePayloadLength(65536))
                            ;
                    return t.connect()
                            .doOnNext(connection -> {
                                logger.warn("Connected");
                                connected.set(true);
                            })
                            .doOnError(error -> {
                                logger.warn("Disconnected");
                                connected.set(false);
                            });
                });

        // https://github.com/rsocket/rsocket-java/issues/987
        requester.rsocketClient().source()
                        .flatMap(RSocket::onClose)
                        .repeat()
                        .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
                        .subscribe();

        Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                 .onBackpressureLatest()
                 .map(tick -> IntStream.range(1, 100)
                        .mapToObj(i -> "Telemetry from " + id + " tick " + tick + " " + i)
                        .collect(Collectors.joining("; "))
                 )
                 .flatMap(msg -> requester.route("/backend/telemetry")
                         .data(msg).retrieveMono(String.class), 1)
                 .doOnNext(res -> System.err.println(res))
                 .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
                 .subscribe();

    }

}
