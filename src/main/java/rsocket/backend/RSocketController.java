package rsocket.backend;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
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
    public Mono<Void> telemetry(Flux<String> messages) {
        return messages.doOnNext(message -> logger.info("Telemetry received: " + message)).then();
    }

    @MessageMapping("/backend/fileUpload")
    public Flux<String> fileUpload(Flux<DataBuffer> bufferFlux) throws IOException {
        Path file = Files.createTempFile("", ".pdf");
        logger.info("Writing to {}", file.toAbsolutePath());
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        AsynchronousFileChannel a = null;
        return DataBufferUtils.write(bufferFlux, channel)
                .map(b -> "CHUNK_COMPLETED");
    }

}
