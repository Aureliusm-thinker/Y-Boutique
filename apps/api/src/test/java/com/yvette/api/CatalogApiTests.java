package com.yvette.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

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
    void searchIsTypoTolerantAndMatchesPartialWords() throws Exception {
        // "dres" (partial) and "dreses" (typo) both find the dresses.
        for (String q : new String[] {"dres", "dreses", "silk dres"}) {
            mvc.perform(get("/api/v1/catalog/products").param("q", q))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[*].slug").value(hasItem("silk-slip-dress")));
        }
        mvc.perform(get("/api/v1/catalog/products").param("q", "silk dres"))
                .andExpect(jsonPath("$.items[*].slug").value(not(hasItem("cotton-poplin-shirt"))));
        mvc.perform(get("/api/v1/catalog/products").param("q", "zzqqxx"))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/v1/catalog/suggest").param("q", "dres"))
                .andExpect(jsonPath("$.categories[*].slug").value(hasItem("dresses")))
                .andExpect(jsonPath("$.products[*].slug").value(hasItem("linen-wrap-dress")));
    }

    @Test
    void filtersBySizeColourPriceAndStockAndSorts() throws Exception {
        // Silk slip dress in S is sold out, so "S + in stock" excludes it but "S" alone includes it.
        mvc.perform(get("/api/v1/catalog/products").param("sizes", "S"))
                .andExpect(jsonPath("$.items[*].slug").value(hasItem("silk-slip-dress")));
        mvc.perform(get("/api/v1/catalog/products").param("sizes", "S").param("inStock", "true"))
                .andExpect(jsonPath("$.items[*].slug").value(not(hasItem("silk-slip-dress"))))
                .andExpect(jsonPath("$.items[*].slug").value(hasItem("linen-wrap-dress")));
        mvc.perform(get("/api/v1/catalog/products").param("colours", "black,natural"))
                .andExpect(jsonPath("$.items[*].slug").value(hasItem("woven-straw-tote")))
                .andExpect(jsonPath("$.items[*].slug").value(not(hasItem("linen-wrap-dress"))));
        mvc.perform(get("/api/v1/catalog/products").param("category", "dresses").param("maxPrice", "15000"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("linen-wrap-dress"));
        mvc.perform(get("/api/v1/catalog/products").param("category", "dresses").param("sort", "price_desc"))
                .andExpect(jsonPath("$.items[0].slug").value("silk-slip-dress"))
                .andExpect(jsonPath("$.items[0].currency").value("KES"));
        mvc.perform(get("/api/v1/catalog/products").param("sort", "cheapest"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalog/facets").param("category", "dresses"))
                .andExpect(jsonPath("$.sizes[0]").value("S"))
                .andExpect(jsonPath("$.colours").value(hasItem("Black")))
                .andExpect(jsonPath("$.minPrice").value(12900));
    }

    @Test
    void productPageHasCompleteTheLookAndSitemapListsIt() throws Exception {
        mvc.perform(get("/api/v1/catalog/products/linen-wrap-dress"))
                .andExpect(jsonPath("$.completeTheLook[0].slug").value("woven-straw-tote"));
        mvc.perform(get("/api/v1/catalog/sitemap"))
                .andExpect(jsonPath("$[*].path").value(hasItem("/products/linen-wrap-dress")));
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
