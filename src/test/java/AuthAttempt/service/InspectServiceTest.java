package authattempt.service;

import authattempt.dto.AuthRequest;
import authattempt.dto.RequestInfo;
import java.lang.reflect.Method;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class InspectServiceTest {

    @MockBean
    private StreamBridge streamBridge;

    @Autowired
    private InspectService inspectService;

    private String checkIp, requestId, url;
    private AuthRequest request;

    @BeforeEach
    public void setUp() {
    	checkIp = "192.168.0.1";
        url = "255.255.130.44";
        requestId = "001";
        request = new AuthRequest(requestId, checkIp, url);
        inspectService.getRequestMap().clear();
    }

    @Test
    @DisplayName("data add correct")
    public void testProcessRequestNewIPNewURL() {
        final String url2 = "001.001.001.34";
        final AuthRequest request2 = new AuthRequest("002", checkIp, url2);
        inspectService.processRequest(request);
        inspectService.processRequest(request2);
        final RequestInfo request1Info = inspectService.getRequestMap().get(checkIp).get(url);
        final RequestInfo request2Info = inspectService.getRequestMap().get(checkIp).get(url2);

        assertNotNull(request1Info);
        assertNotNull(request2Info);
        
        assertEquals(1, request1Info.getCount());
        assertEquals(1, request2Info.getCount());

        System.out.println("Request1Info: " + request1Info);
        System.out.println("Request2Info: " + request2Info);
       
        assertTrue(request1Info.getRequests().contains(request));
        assertTrue(request2Info.getRequests().contains(request2));

    }
    
    @Test
    @DisplayName("less then max attempts doesn't call sent notification")
    public void testProcessRequestLessThenMaxAttempts() {
        for (int i = 0; i < 9; i++) {
            inspectService.processRequest(request);
        }
        verify(streamBridge, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Max attempts calls sent notification")
    public void testProcessRequestExceedingMaxAttempts() {
        for (int i = 0; i < 11; i++) {
            inspectService.processRequest(request);
        }
        verify(streamBridge, times(1)).send(anyString(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Time period less")
    public void testCheckAndSendToDBWithinTimePeriod() {
        inspectService.processRequest(request);

        final RequestInfo requestInfo = inspectService.getRequestMap().get(checkIp).get(url);
        requestInfo.setTimestamp(System.currentTimeMillis()
        		- (inspectService.getMeasurePeriodSec() * 1000) + 1000);

        final Method method = InspectService.class.getDeclaredMethod("checkAndSendToDB", String.class, String.class);
        method.setAccessible(true);
        method.invoke(inspectService, checkIp, url);

        assertEquals(1, requestInfo.getCount());
    }

    @Test
    @SneakyThrows
    @DisplayName("Time period more")
    public void testCheckAndSendToDBExceedingTimePeriod() {
        inspectService.processRequest(request);

        final RequestInfo requestInfo = inspectService.getRequestMap().get(checkIp).get(url);
        requestInfo.setTimestamp(System.currentTimeMillis() 
        		- (inspectService.getMeasurePeriodSec() * 1000) - 1000);

        final Method method = InspectService.class.getDeclaredMethod("checkAndSendToDB", String.class, String.class);
        method.setAccessible(true);
        method.invoke(inspectService, checkIp, url);

        assertEquals(0, requestInfo.getCount());
    }
}
