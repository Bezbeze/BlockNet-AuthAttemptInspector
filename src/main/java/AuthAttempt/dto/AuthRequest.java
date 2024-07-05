package AuthAttempt.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class AuthRequest {
	
	String requestId;
    String checkIp;
    String clientUrl;

}
