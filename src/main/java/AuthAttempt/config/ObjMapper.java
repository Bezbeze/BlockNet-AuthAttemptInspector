package authattempt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjMapper {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
