package rsocket.client;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Component;

import io.rsocket.RSocket;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Component
public class BackendService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String id = "test1";
    private final String backendUrl = "ws://localhost:9897";

    private final RSocketRequester requester;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    public BackendService(RSocketRequester.Builder builder, RSocketStrategies strategies) {
        logger.info("id: {}, backend URL: {}", id, backendUrl);

        HttpClient httpClient = HttpClient.create().baseUrl(backendUrl);

        requester = builder
                .setupRoute("/backend/connect")
                .setupData(id)
                .rsocketConnector(connector -> connector.reconnect(Retry.fixedDelay(Integer.MAX_VALUE, Duration.ofSeconds(5))))
                .dataMimeType(MediaType.APPLICATION_JSON)
                .transport(() -> {
                    // httpClient is immutable - baseUrl() creates a copy
                    ClientTransport t = WebsocketClientTransport.create(httpClient, "/rsocket");
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

         Flux.interval(Duration.ZERO, Duration.ofSeconds(60))
                 .onBackpressureLatest()
                 .map(tick -> "Telemetry from " + id + " tick " + tick)
                 .flatMap(msg -> requester.route("/backend/telemetry").data(msg).retrieveMono(Void.class), 1)
                 .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
                 .subscribe();

    }

}
