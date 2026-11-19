package dociq_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = "dociq_backend")
@EnableAsync
@EnableScheduling
@EnableJpaAuditing // <--- ADD THIS LINE HERE
public class DociqBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(DociqBackendApplication.class, args);
	}
}