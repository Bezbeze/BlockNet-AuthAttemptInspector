package AuthAttempt.service;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import AuthAttempt.dto.AuthRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
@RequiredArgsConstructor
public class SensorData {
	ObjectMapper mapper;
	InspectService inspectService;
	
	@Bean
	Consumer<String> receiveSensorData(){
		return sensorData -> {
			try {
				AuthRequest authRequest = mapper.readValue(sensorData, AuthRequest.class);
				inspectService.processRequest(authRequest);
			} catch (JsonProcessingException e) {
				log.error("Failed to read value authRequest", e);
			}			
		};
	}

}
