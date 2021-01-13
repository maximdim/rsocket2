package rsocket.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class AgentApplication implements CommandLineRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder().sources(AgentApplication.class).profiles("client").run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        Thread.currentThread().join();
    }

}
