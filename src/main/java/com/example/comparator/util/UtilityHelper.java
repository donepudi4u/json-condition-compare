@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SendNotificationClientImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private MaskingUtil maskingUtil;

    @InjectMocks
    private SendNotificationClientImpl sendNotificationClient;

    private final WebhookEventResponse payload = new WebhookEventResponse();

    @BeforeEach
    void setup() {
        MDC.put("transactionId", "test-tx-id");
    }

    @AfterEach
    void cleanup() {
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
    void shouldSendNotificationSuccessfully() {
        String url = "http://localhost:8080/notify?X-Header1=value1&X-Header2=value2";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://localhost:8080/notify")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(payload)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        when(maskingUtil.mask("value1", '*')).thenReturn("*****");
        when(maskingUtil.mask("value2", '*')).thenReturn("*****");

        Mono<Void> result = sendNotificationClient.sendNotification(url, payload);

        StepVerifier.create(result)
                .verifyComplete();

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("http://localhost:8080/notify");
        verify(requestBodyUriSpec).bodyValue(payload);
        verify(requestHeadersSpec).headers(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).onStatus(any(), any());
        verify(responseSpec).bodyToMono(Void.class);
    }

    @Test
    void shouldHandleWebClientError() {
        String url = "http://localhost:8080/notify";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(url)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(payload)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.error(new RuntimeException("Webhook failed")));

        Mono<Void> result = sendNotificationClient.sendNotification(url, payload);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof RuntimeException);
                    assertEquals("Webhook failed", ex.getMessage());
                })
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