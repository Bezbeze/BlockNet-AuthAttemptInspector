package authattempt.service;

import authattempt.dto.AuthRequest;
import authattempt.dto.RequestInfo;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

/*
 * The InspectService processes authentication requests and tracks request counts
 * for each unique client IP and resource URL combination. It blocks IP addresses
 * if they exceed a predefined number of requests to the same resource within a specified time period.
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PropertySource(value = "count.properties")
@Getter
public class InspectService {
    
    StreamBridge streamBridge;
    
    int maxAttempts;
    int measurePeriodSec;
    
    ConcurrentHashMap<String, ConcurrentHashMap<String, RequestInfo>> requestMap = new ConcurrentHashMap<>();
    

    public InspectService(@Autowired StreamBridge streamBridge,
            @Value("${max.attempts:10}") int maxAttempts,
            @Value("${measure.period.sec:60}") int measurePeriodSec) {
        this.streamBridge = streamBridge;
        this.maxAttempts = maxAttempts;
        this.measurePeriodSec = measurePeriodSec;
    }

    public void processRequest(AuthRequest request) {
    	final String ip = request.getCheckIp();
        final String url = request.getClientUrl();

        requestMap.computeIfAbsent(ip, k -> new ConcurrentHashMap<>())
                  .compute(url, (key, requestInfo) -> {
                      if (requestInfo == null) {
                          requestInfo = new RequestInfo();
                          requestInfo.setCount(1);
                          requestInfo.getRequests().add(request);
                      } else {
                          requestInfo.setCount(requestInfo.getCount() + 1);
                          requestInfo.getRequests().add(request);
                      }
                    return requestInfo;
                  });

        checkAndSendToDB(ip, url);
    }

    private void checkAndSendToDB(String ip, String url) {
    	final RequestInfo requestInfo = requestMap.get(ip).get(url);
    	final long currentTime = System.currentTimeMillis();
        final long elapsedTime = currentTime - requestInfo.getTimestamp();

        if (requestInfo.getCount() >= maxAttempts) {
            sendToDB(requestInfo.getRequests());
            resetIpData(ip, url, currentTime);
        } else if (elapsedTime >= measurePeriodSec * 1000) {
            resetIpData(ip, url, currentTime);
        }
    }

    private void resetIpData(String ip, String url, long currentTime) {
    	final RequestInfo requestInfo = requestMap.get(ip).get(url);
        requestInfo.setTimestamp(currentTime);
        requestInfo.getRequests().clear();
        requestInfo.setCount(0);
    }

    private void sendToDB(Set<AuthRequest> requests) {
        requests.forEach(request -> streamBridge.send("sensor-data-out", request));
    }
}
