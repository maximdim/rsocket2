package rsocket.backend;

import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.WebsocketRouteTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyRouteProvider;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.server.WebsocketServerSpec;

import java.util.stream.Collectors;

@Configuration
public class BackendConfig {

    @Bean
    NettyRouteProvider rSocketWebsocketRouteProvider(
            RSocketProperties properties, RSocketMessageHandler messageHandler,
            ObjectProvider<RSocketServerCustomizer> customizers) {
        // Copy of RSocketWebSocketNettyRouteProvider to override WebsocketSpec.Builder.compress(true)
        return new NettyRouteProvider() {
            public HttpServerRoutes apply(HttpServerRoutes httpServerRoutes) {
                RSocketServer server = RSocketServer.create(messageHandler.responder());
                customizers.forEach((customizer) -> customizer.customize(server));

                return httpServerRoutes.ws("/rsocket",
                        WebsocketRouteTransport.newHandler(server.asConnectionAcceptor()),
                        WebsocketServerSpec.builder().compress(true).build());
            }
        };
    }

    // This is to force Netty as container, even if Jetty dependency is present
    // This is to replace what is in the ReactiveWebServerFactoryConfiguration
    @Bean
    NettyReactiveWebServerFactory nettyReactiveWebServerFactory(
            ReactorResourceFactory resourceFactory,
            ObjectProvider<NettyRouteProvider> routes,
            RSocketMessageHandler messageHandler,
            ObjectProvider<NettyServerCustomizer> serverCustomizers,
            ObjectProvider<RSocketServerCustomizer> customizers) {

        NettyReactiveWebServerFactory serverFactory = new NettyReactiveWebServerFactory();
        serverFactory.setResourceFactory(resourceFactory);

        routes.orderedStream().forEach(routeProviders -> serverFactory.addRouteProviders(routeProviders));

        serverFactory.getServerCustomizers().addAll(serverCustomizers.orderedStream().collect(Collectors.toList()));

        return serverFactory;
    }

}
