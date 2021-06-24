package rsocket.client;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Component;

import io.rsocket.transport.netty.client.WebsocketClientTransport;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Component
public class BackendService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String id = "test1";
    private final String backendUrl = "ws://localhost:9898";

    private final RSocketRequester requester;

    public BackendService(RSocketRequester.Builder builder, RSocketStrategies strategies) throws Exception {
        logger.info("id: {}, backend URL: {}", id, backendUrl);

        HttpClient httpClient = HttpClient.create()
                .baseUrl(backendUrl);

        requester = builder
                .setupRoute("/backend/connect")
                .setupData(id)
                .rsocketConnector(connector -> connector.reconnect(Retry.fixedDelay(Integer.MAX_VALUE, Duration.ofSeconds(1))))
                .dataMimeType(MediaType.APPLICATION_JSON)
                .transport(WebsocketClientTransport.create(httpClient, "/rsocket"));

        Flux.interval(Duration.ofSeconds(10))
                 //.onBackpressureLatest()
                 .map(tick -> "Telemetry from " + id + " tick " + tick)
                 .as(flux -> requester.route("/backend/telemetry").data(flux).retrieveFlux(Void.class))
                 .retry()
                 .subscribe();

        Resource resource = new UrlResource("https://freeclassicebooks.com/Tolstoy/War%20and%20Peace.pdf");
        Flux<DataBuffer> readFlux = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 4096)
                .doOnNext(s -> logger.info("Sent..."));
        requester.route("/backend/fileUpload")
                .data(readFlux)
                .retrieveFlux(String.class)
                .doOnNext(s -> logger.info("Upload Status : {}", s))
                .subscribe();
    }

}
