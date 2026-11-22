package com.yvette.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yvette.api.recommend.RecommendationJob;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@ApiIntegrationTest
class RecommendationApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    RecommendationJob job;

    private static final String FOR_YOU = "/api/v1/catalog/recommendations/for-you";

    @Test
    void newVisitorsGetPopularInStockProducts() throws Exception {
        mvc.perform(get(FOR_YOU))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalised").value(false))
                .andExpect(jsonPath("$.basedOnSearches", empty()))
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.items[*].soldOut", everyItem(org.hamcrest.Matchers.is(false))));
    }

    @Test
    void recentSearchesDriveTheList() throws Exception {
        String visitor = UUID.randomUUID().toString();
        track(visitor, "search", "{\"query\":\"straw\",\"results\":1}", 0);

        mvc.perform(get(FOR_YOU).param("visitor", visitor).param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalised").value(true))
                .andExpect(jsonPath("$.basedOnSearches", contains("straw")))
                .andExpect(jsonPath("$.items[0].slug").value("woven-straw-tote"))
                .andExpect(jsonPath("$.items", hasSize(4)));   // topped up with popular products
    }

    @Test
    void newerSearchesOutrankOlderOnes() throws Exception {
        String visitor = UUID.randomUUID().toString();
        track(visitor, "search", "{\"query\":\"poplin shirt\"}", 20);
        track(visitor, "search", "{\"query\":\"straw tote\"}", 0);

        mvc.perform(get(FOR_YOU).param("visitor", visitor))
                .andExpect(jsonPath("$.items[0].slug").value("woven-straw-tote"))
                .andExpect(jsonPath("$.items[1].slug").value("cotton-poplin-shirt"))
                .andExpect(jsonPath("$.basedOnSearches", contains("straw tote", "poplin shirt")));
    }

    @Test
    void viewedProductsBringSimilarOnesAndAreLeftOut() throws Exception {
        String visitor = UUID.randomUUID().toString();
        track(visitor, "product_view", "{\"productId\":" + productId("linen-wrap-dress") + "}", 0);

        mvc.perform(get(FOR_YOU).param("visitor", visitor))
                .andExpect(jsonPath("$.personalised").value(true))
                // Same category and a shared style tag.
                .andExpect(jsonPath("$.items[0].slug").value("silk-slip-dress"))
                .andExpect(jsonPath("$.items[*].slug", not(hasItem("linen-wrap-dress"))));
    }

    @Test
    void excludesTheProductOnScreenAndIgnoresJunkProperties() throws Exception {
        String visitor = UUID.randomUUID().toString();
        track(visitor, "product_view", "{\"productId\":\"1; DROP TABLE products\"}", 0);
        track(visitor, "add_to_cart", "{\"variantId\":\"abc\"}", 0);

        mvc.perform(get(FOR_YOU).param("visitor", visitor).param("exclude", String.valueOf(productId("woven-straw-tote"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalised").value(false))
                .andExpect(jsonPath("$.items[*].slug", not(hasItem("woven-straw-tote"))));
    }

    @Test
    void alsoViewedNeedsMoreThanOneShopper() throws Exception {
        long tote = productId("woven-straw-tote");
        long silk = productId("silk-slip-dress");

        String first = UUID.randomUUID().toString();
        track(first, "product_view", "{\"productId\":" + tote + "}", 0);
        track(first, "product_view", "{\"productId\":" + silk + "}", 0);
        job.rebuild();
        // One shopper's browsing never shows as "customers also viewed".
        mvc.perform(get("/api/v1/catalog/products/woven-straw-tote/also-viewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basedOn").value("similar"));

        String second = UUID.randomUUID().toString();
        track(second, "product_view", "{\"productId\":" + silk + "}", 1);
        track(second, "product_view", "{\"productId\":" + tote + "}", 1);
        job.rebuild();
        mvc.perform(get("/api/v1/catalog/products/woven-straw-tote/also-viewed"))
                .andExpect(jsonPath("$.basedOn").value("customers"))
                .andExpect(jsonPath("$.items[0].slug").value("silk-slip-dress"));

        // Hand-picked "Complete the look" items (linen dress → tote) aren't repeated.
        mvc.perform(get("/api/v1/catalog/products/linen-wrap-dress/also-viewed"))
                .andExpect(jsonPath("$.items[*].slug", not(hasItem("woven-straw-tote"))));
        mvc.perform(get("/api/v1/catalog/products/no-such-product/also-viewed"))
                .andExpect(status().isNotFound());
    }

    private void track(String visitor, String name, String properties, int daysAgo) throws Exception {
        String body = """
                {"eventId":"%s","name":"%s","anonymousId":"%s","properties":%s,"occurredAt":"%s"}"""
                .formatted(UUID.randomUUID(), name, visitor, properties,
                        Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        mvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());
    }

    private long productId(String slug) {
        return jdbc.sql("SELECT id FROM products WHERE slug = :s").param("s", slug).query(Long.class).single();
    }
}
