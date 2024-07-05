package AuthAttempt.dto;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class RequestInfo {
    private int count;
    private long timestamp;
    private Set<AuthRequest> requests;

    public RequestInfo() {
        this.count = 1;
        this.timestamp = System.currentTimeMillis();
        this.requests = new HashSet<>();
    }
}
