package AuthAttempt.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import AuthAttempt.dto.AuthRequest;
import AuthAttempt.dto.RequestInfo;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/*
 * The InspectService processes authentication requests and tracks request counts
 * for each unique client IP and resource URL combination. It blocks IP addresses
 * if they exceed a predefined number of requests to the same resource within a specified time period.
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PropertySource(value = "count.properties")
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
        String ip = request.getCheckIp();
        String url = request.getClientUrl();

        requestMap.computeIfAbsent(ip, k -> new ConcurrentHashMap<>())
                  .compute(url, (key, requestInfo) -> {
                      if (requestInfo == null) {
                          requestInfo = new RequestInfo();
                      } else {
                          requestInfo.setCount(requestInfo.getCount() + 1);
                          requestInfo.getRequests().add(request);
                      }
                      return requestInfo;
                  });
        
        checkAndSendToDB(ip, url);
    }

    private void checkAndSendToDB(String ip, String url) {
        RequestInfo requestInfo = requestMap.get(ip).get(url);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - requestInfo.getTimestamp();

        if (requestInfo.getCount() >= maxAttempts) {
            sendToDB(requestInfo.getRequests());
            resetIpData(ip, url, currentTime);
        } else if (elapsedTime >= measurePeriodSec * 1000) {
            resetIpData(ip, url, currentTime);
        }
    }

    private void resetIpData(String ip, String url, long currentTime) {
        RequestInfo requestInfo = requestMap.get(ip).get(url);
        requestInfo.setTimestamp(currentTime);
        requestInfo.getRequests().clear();
        requestInfo.setCount(0);
    }

    private void sendToDB(Set<AuthRequest> requests) {
        requests.forEach(request -> streamBridge.send("sensor-data-out", request));
    }
}
