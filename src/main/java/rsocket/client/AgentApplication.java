package rsocket.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = new SpringApplicationBuilder().sources(AgentApplication.class).profiles("client").run(args);
        System.out.println("Started 1");
        Thread.sleep(30_000L);
        context.close();
        System.out.println("Stopped 1");

        Thread.getAllStackTraces().keySet()
            .forEach(t -> {
                System.out.println("*** " + t.getName());
            });

        Thread.sleep(30_000L);
    }

}
