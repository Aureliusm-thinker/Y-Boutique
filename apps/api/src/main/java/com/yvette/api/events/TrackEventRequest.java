package com.yvette.api.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A behavioural event sent by the storefront. Names follow docs/04-ai-ml-strategy.md §3.1.
 * The storefront only sends events after the visitor has consented to analytics.
 */
public record TrackEventRequest(
        @NotNull UUID eventId,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$") String name,
        @NotBlank @Size(max = 64) String anonymousId,
        @Size(max = 64) String sessionId,
        Map<String, Object> properties,
        @NotNull Instant occurredAt) {}
