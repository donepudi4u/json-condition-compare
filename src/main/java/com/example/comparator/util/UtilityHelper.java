package com.example.comparator.util;

public class UtilityHelper {

    @ExtendWith(MockitoExtension.class)
    class SendNotificationClientTest {

        @Mock
        private WebClient webClient;

        @Mock
        private WebClient.RequestBodyUriSpec requestBodyUriSpec;

        @Mock
        private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

        @Mock
        private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

        @Mock
        private WebClient.ResponseSpec responseSpec;

        @InjectMocks
        private SendNotificationClient sendNotificationClient;

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
        void shouldReturnErrorWhenWebhookTargetIsBlank() {
            Mono<Void> result = sendNotificationClient.sendNotification("  ", payload);
            StepVerifier.create(result)
                    .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                            ex.getMessage().equals("Missing Webhook URI"))
                    .verify();
        }

        @Test
        void shouldReturnErrorWhenWebhookTargetIsInvalidURI() {
            Mono<Void> result = sendNotificationClient.sendNotification("ht@tp://bad uri", payload);
            StepVerifier.create(result)
                    .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                            ex.getMessage().equals("Invalid Webhook URI"))
                    .verify();
        }

        @Test
        void shouldSendNotificationSuccessfully() {
            String validUrl = "http://localhost:8080/notify?X-Header1=value1";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

            Mono<Void> result = sendNotificationClient.sendNotification(validUrl, payload);

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
        void shouldHandleErrorFromWebClient() {
            String validUrl = "http://localhost:8080/notify";

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.error(new RuntimeException("Webhook failed")));

            Mono<Void> result = sendNotificationClient.sendNotification(validUrl, payload);

            StepVerifier.create(result)
                    .expectErrorMatches(ex -> ex instanceof RuntimeException &&
                            ex.getMessage().equals("Webhook failed"))
                    .verify();
        }
    }

}
