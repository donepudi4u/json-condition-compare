package com.example.comparator.util;

public class UtilityHelper {



    public Mono<Void> sendNotification(String webhookTarget, WebhookEventResponse payload) {
        return webClient.post()
                .uri(webhookTarget)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("No response body")
                                .flatMap(body -> {
                                    HttpStatus status = clientResponse.statusCode();
                                    String message = "Status: " + status.value() + ", Body: " + body;
                                    log.error("Notification failed - {}", message);
                                    return Mono.error(new EventsProcessorException(status, message));
                                })
                )
                .toBodilessEntity()
                .doOnSuccess(response ->
                        log.info("Notification delivered successfully with status: {}", response.getStatusCode())
                )
                .then(); // Return Mono<Void> to signal completion
    }

    /*
    sendNotification(webhookUrl, eventPayload)
    .doOnSuccess(v -> dbService.updateStatus("SUCCESS"))
    .doOnError(ex -> dbService.updateStatus("FAILURE: " + ex.getMessage()))
    .subscribe(); // Triggers the pipeline

     */



}
