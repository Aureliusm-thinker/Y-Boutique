package com.yvette.api.events;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class EventService {

    /** Events accepted from the storefront. Anything else is rejected to keep the data clean. */
    static final Set<String> ALLOWED_EVENTS = Set.of(
            "page_view", "product_view", "search",
            "add_to_cart", "remove_from_cart",
            "wishlist_add", "wishlist_remove",
            "begin_checkout",
            "recommendation_impression", "recommendation_click");

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public EventService(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Records a fact the server knows for sure (purchase, return_requested, review_submitted), which the
     * browser is not allowed to send. Never throws: analytics must not break a checkout.
     */
    public void recordServerEvent(String name, String anonymousId, Long userId, Map<String, Object> properties) {
        try {
            jdbc.sql("""
                    INSERT INTO events (event_id, name, anonymous_id, user_id, properties, occurred_at)
                    VALUES (:eventId, :name, :anonymousId, :userId, CAST(:properties AS jsonb), now())
                    """)
                    .param("eventId", java.util.UUID.randomUUID())
                    .param("name", name)
                    .param("anonymousId", anonymousId)
                    .param("userId", userId)
                    .param("properties", json.writeValueAsString(properties))
                    .update();
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(EventService.class).warn("Could not record {} event: {}", name, e.getMessage());
        }
    }

    /**
     * Stores the event. Duplicate event ids are ignored so client retries are safe.
     * Server-side facts (purchase, return_requested, review_submitted) are recorded by their
     * own modules, not accepted from the browser.
     */
    public void track(TrackEventRequest event, Long userId) {
        if (!ALLOWED_EVENTS.contains(event.name())) {
            throw new IllegalArgumentException("Unsupported event: " + event.name());
        }
        Map<String, Object> props = event.properties() == null ? Map.of() : event.properties();
        jdbc.sql("""
                INSERT INTO events (event_id, name, anonymous_id, user_id, session_id, properties, occurred_at)
                VALUES (:eventId, :name, :anonymousId, :userId, :sessionId, CAST(:properties AS jsonb), :occurredAt)
                ON CONFLICT (event_id) DO NOTHING
                """)
                .param("eventId", event.eventId())
                .param("name", event.name())
                .param("anonymousId", event.anonymousId())
                .param("userId", userId)
                .param("sessionId", event.sessionId())
                .param("properties", json.writeValueAsString(props))
                .param("occurredAt", Timestamp.from(event.occurredAt()))
                .update();
    }
}
