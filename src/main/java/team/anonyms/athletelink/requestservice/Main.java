package team.anonyms.athletelink.requestservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Request Service API",
        version = "v1",
        description = "Create and manage activity requests before a match starts. Joining is approved asynchronously through Game Service. After a request starts, Game Service owns rounds and results; this API exposes the mirrored request status."
))
public class Main {
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
