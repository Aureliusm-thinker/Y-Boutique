package com.yvette.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ApiIntegrationTest
class CatalogApiTests {

    @Autowired
    MockMvc mvc;

    @Test
    void listsActiveProducts() throws Exception {
        mvc.perform(get("/api/v1/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.items[*].slug").value(org.hamcrest.Matchers.hasItem("linen-wrap-dress")));
    }

    @Test
    void filtersByCategory() throws Exception {
        mvc.perform(get("/api/v1/catalog/products").param("category", "dresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void returnsProductDetailWithStock() throws Exception {
        mvc.perform(get("/api/v1/catalog/products/silk-slip-dress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Silk Slip Dress"))
                .andExpect(jsonPath("$.category.slug").value("dresses"))
                .andExpect(jsonPath("$.variants[0].size").value("S"))
                .andExpect(jsonPath("$.variants[0].inStock").value(false))
                .andExpect(jsonPath("$.variants[1].inStock").value(true));
    }

    @Test
    void unknownProductIs404() throws Exception {
        mvc.perform(get("/api/v1/catalog/products/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptsValidEventAndRejectsUnknownName() throws Exception {
        String valid = """
                {"eventId":"6f1c1c3e-2b8e-4a53-9a7e-0b7f9f1d2a11","name":"product_view",
                 "anonymousId":"anon-123","properties":{"productId":1,"source":"collection"},
                 "occurredAt":"2026-11-11T10:00:00Z"}
                """;
        mvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isAccepted());

        String invalid = valid.replace("product_view", "purchase");
        mvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isUnauthorized());
    }
}
