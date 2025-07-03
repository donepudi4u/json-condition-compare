import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SendNotificationClientImplTest {

    private MockWebServer mockWebServer;
    private SendNotificationClientImpl sendNotificationClient;
    private MaskingUtil maskingUtil;

    private final WebhookEventResponse payload = new WebhookEventResponse();

    @BeforeEach
    void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        maskingUtil = mock(MaskingUtil.class);
        sendNotificationClient = new SendNotificationClientImpl();

        // Inject mocks via reflection
        Field webClientField = SendNotificationClientImpl.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(sendNotificationClient, webClient);

        Field maskingUtilField = SendNotificationClientImpl.class.getDeclaredField("maskingUtil");
        maskingUtilField.setAccessible(true);
        maskingUtilField.set(sendNotificationClient, maskingUtil);


        MDC.put("transactionId", "test-tx-id");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
        MDC.clear();
    }

    @Test
    void shouldFailWhenWebhookTargetIsBlank() {
        Mono<Void> result = sendNotificationClient.sendNotification("   ", payload);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof IllegalArgumentException);
                    assertEquals("Missing Webhook URI", ex.getMessage());
                })
                .verify();
    }

    @Test
    void shouldFailWhenWebhookTargetIsInvalidURI() {
        Mono<Void> result = sendNotificationClient.sendNotification("ht@tp://bad uri", payload);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof IllegalArgumentException);
                    assertEquals("Invalid Webhook URI", ex.getMessage());
                })
                .verify();
    }

    @Test
    void shouldSendNotificationSuccessfully() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        String url = mockWebServer.url("/notify?X-Header1=value1&X-Header2=value2").toString();

        when(maskingUtil.mask("value1", '*')).thenReturn("*****");
        when(maskingUtil.mask("value2", '*')).thenReturn("*****");

        Mono<Void> result = sendNotificationClient.sendNotification(url, payload);

        StepVerifier.create(result).verifyComplete();

        var request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().startsWith("/notify"));
    }

    @Test
    void shouldHandleServerError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        String url = mockWebServer.url("/notify").toString();

        Mono<Void> result = sendNotificationClient.sendNotification(url, payload);

        StepVerifier.create(result)
                .expectErrorMatches(ex -> ex instanceof RuntimeException &&
                        ex.getMessage().contains("500"))
                .verify();
    }

    @Test
    void shouldConvertQueryParamsToHeaders() throws Exception {
        URI uri = new URI("http://localhost:8080/notify?token=abc123&user=test%20user");

        Method method = SendNotificationClientImpl.class.getDeclaredMethod("convertQueryParamsToHeader", URI.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) method.invoke(sendNotificationClient, uri);

        assertEquals("abc123", headers.get("token"));
        assertEquals("test user", headers.get("user"));
    }

    @Test
    void shouldBuildBaseURIWithPort() throws Exception {
        URI uri = new URI("http://localhost:8080/notify?token=abc");

        Method method = SendNotificationClientImpl.class.getDeclaredMethod("buildBaseURI", URI.class);
        method.setAccessible(true);
        String result = (String) method.invoke(sendNotificationClient, uri);

        assertEquals("http://localhost:8080/notify", result);
    }

    @Test
    void shouldPrintMaskedHeaders() throws Exception {
        Map<String, String> headers = Map.of("Authorization", "secret", "X-Trace", "abc123");

        when(maskingUtil.mask("secret", '*')).thenReturn("******");
        when(maskingUtil.mask("abc123", '*')).thenReturn("******");

        Method method = SendNotificationClientImpl.class.getDeclaredMethod("printHeaders", Map.class);
        method.setAccessible(true);
        String result = (String) method.invoke(sendNotificationClient, headers);

        assertEquals("{Authorization=******,X-Trace=******}", result);
    }
}