package com.yvette.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

@ApiIntegrationTest
class AdminApiTests {

    // Dev owner account created by StaffBootstrap from application.yml (dev profile).
    static final String OWNER_EMAIL = "owner@yvette.local";
    static final String OWNER_PASSWORD = "yvette-dev-owner";

    @Autowired
    MockMvc mvc;

    // ------------------------------------------------------------------ auth

    @Test
    void ownerCanLogInAndSeeThemselves() throws Exception {
        String token = login(OWNER_EMAIL, OWNER_PASSWORD);
        mvc.perform(get("/api/v1/admin/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OWNER_EMAIL))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void wrongPasswordIsRejectedAndRepeatedFailuresAreRateLimited() throws Exception {
        String body = "{\"email\":\"nobody@yvette.local\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/staff/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/staff/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ roles

    @Test
    void marketingCanViewProductsButNotEditThemOrManageStaff() throws Exception {
        String owner = login(OWNER_EMAIL, OWNER_PASSWORD);
        as(owner, post("/api/v1/admin/staff").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"Marketing@Yvette.local","fullName":"Mia Marketing","role":"MARKETING",
                 "password":"marketing-password-123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("marketing@yvette.local"));

        String marketing = login("marketing@yvette.local", "marketing-password-123");
        as(marketing, get("/api/v1/admin/products")).andExpect(status().isOk());
        as(marketing, post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Should Fail", null, "MKT-1", ""))).andExpect(status().isForbidden());
        as(marketing, get("/api/v1/admin/staff")).andExpect(status().isForbidden());
    }

    @Test
    void lastOwnerCannotBeDemoted() throws Exception {
        String owner = login(OWNER_EMAIL, OWNER_PASSWORD);
        String me = as(owner, get("/api/v1/admin/me")).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(me, "$.id");
        as(owner, put("/api/v1/admin/staff/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Store Owner\",\"role\":\"MANAGER\",\"active\":true}"))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ products, publishing, stock

    @Test
    void productLifecycleFromDraftToLiveWithStock() throws Exception {
        String owner = login(OWNER_EMAIL, OWNER_PASSWORD);

        // A category for this test only, so storefront counts in other tests are unaffected.
        String cat = as(owner, post("/api/v1/admin/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Knitwear Test\",\"position\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("knitwear-test"))
                .andReturn().getResponse().getContentAsString();
        Integer categoryId = JsonPath.read(cat, "$.id");

        // 1. Create a minimal draft: slug is generated, opening stock recorded.
        String created = as(owner, post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Merino Crew Jumper", null, "MCJ-OAT-M", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("merino-crew-jumper"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.variants[0].onHand").value(2))
                .andExpect(jsonPath("$.variants[0].low").value(true))
                .andExpect(jsonPath("$.publishProblems").value(hasItem("Choose a category")))
                .andReturn().getResponse().getContentAsString();
        Integer productId = JsonPath.read(created, "$.id");
        Integer variantId = JsonPath.read(created, "$.variants[0].id");

        // 2. Publishing an incomplete product is refused with the list of what's missing.
        as(owner, post("/api/v1/admin/products/" + productId + "/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").value(hasItem("Add at least one image")));

        // 3. Fill in the required attributes (keeping the existing variant) and publish.
        String complete = productJson("Merino Crew Jumper", categoryId, "MCJ-OAT-M", """
                ,"description":"Soft merino knit.","material":"Merino wool",
                "images":[{"url":"https://placehold.co/800x1000.png","altText":"Oat jumper"}]""")
                .replace("\"variants\":[{", "\"variants\":[{\"id\":" + variantId + ",");
        as(owner, put("/api/v1/admin/products/" + productId).contentType(MediaType.APPLICATION_JSON).content(complete))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishProblems").isEmpty())
                .andExpect(jsonPath("$.variants[0].id").value(variantId));
        as(owner, post("/api/v1/admin/products/" + productId + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 4. It is now visible on the storefront.
        mvc.perform(get("/api/v1/catalog/products").param("category", "knitwear-test"))
                .andExpect(jsonPath("$.items[0].slug").value("merino-crew-jumper"));

        // 5. Same SKU on another product is a conflict.
        as(owner, post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Another Jumper", null, "MCJ-OAT-M", "")))
                .andExpect(status().isConflict());

        // 6. Stock: shows as low, can't go negative, adjustments are recorded.
        as(owner, get("/api/v1/admin/inventory/low-stock"))
                .andExpect(jsonPath("$[*].sku").value(hasItem("MCJ-OAT-M")));
        as(owner, post("/api/v1/admin/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variantId + ",\"delta\":-5,\"reason\":\"DAMAGED\"}"))
                .andExpect(status().isBadRequest());
        as(owner, post("/api/v1/admin/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variantId + ",\"delta\":10,\"reason\":\"RECEIVED\",\"note\":\"PO-1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onHand").value(12))
                .andExpect(jsonPath("$.low").value(false));
        as(owner, get("/api/v1/admin/inventory/variants/" + variantId + "/adjustments"))
                .andExpect(jsonPath("$[0].delta").value(10))
                .andExpect(jsonPath("$[0].note").value("PO-1001"))
                .andExpect(jsonPath("$[1].reason").value("RECEIVED")); // opening stock

        // 7. Removing the image from a live product is refused.
        String noImages = complete.replaceAll("\"images\":\\[.*?]", "\"images\":[]");
        as(owner, put("/api/v1/admin/products/" + productId).contentType(MediaType.APPLICATION_JSON).content(noImages))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ media

    @Test
    void uploadsImagesButRejectsOtherFiles() throws Exception {
        String owner = login(OWNER_EMAIL, OWNER_PASSWORD);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        String result = as(owner, multipart("/api/v1/admin/media")
                .file(new MockMultipartFile("file", "photo.png", "image/png", png)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("http://localhost:8080/media/")))
                .andReturn().getResponse().getContentAsString();

        String path = ((String) JsonPath.read(result, "$.url")).replace("http://localhost:8080", "");
        mvc.perform(get(path)).andExpect(status().isOk());

        as(owner, multipart("/api/v1/admin/media")
                .file(new MockMultipartFile("file", "evil.png", "image/png", "<script>".getBytes())))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ helpers

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/staff/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions as(String token, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    /** Product JSON with one variant (2 in stock). {@code extra} is appended raw (start it with a comma). */
    private static String productJson(String title, Integer categoryId, String sku, String extra) {
        String images = extra.contains("\"images\"") ? "" : ",\"images\":[]";
        return "{\"title\":\"" + title + "\""
                + (categoryId == null ? "" : ",\"categoryId\":" + categoryId)
                + ",\"variants\":[{\"sku\":\"" + sku + "\",\"size\":\"M\",\"colour\":\"Oat\",\"price\":149.00,"
                + "\"openingStock\":2}]"
                + images + extra + "}";
    }
}
