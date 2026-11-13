package com.yvette.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/** Returns, reviews, promotions, content and reports (docs/06 Sprint 5). */
@ApiIntegrationTest
class Sprint5ApiTests {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    TestShop shop;

    @BeforeEach
    void setUp() {
        shop = new TestShop(mvc, jdbc);
    }

    private TestShop.PlacedOrder deliveredOrder(String email, String sku, int qty) throws Exception {
        TestShop.PlacedOrder o = shop.buyAndPay(email, null, sku, qty);
        String f = shop.staff("FULFILMENT");
        shop.as(f, post("/api/v1/admin/orders/" + o.id() + "/ship").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        shop.as(f, post("/api/v1/admin/orders/" + o.id() + "/deliver")).andExpect(status().isOk());
        return o;
    }

    private long lineId(TestShop.PlacedOrder o) {
        return jdbc.sql("SELECT id FROM order_lines WHERE order_id = :o ORDER BY id LIMIT 1").param("o", o.id())
                .query(Long.class).single();
    }

    @Test
    void returnIsRequestedApprovedReceivedRestockedAndRefunded() throws Exception {
        long variant = shop.createLiveProduct("RET-1", 5);
        String sku = "RET-1";
        TestShop.PlacedOrder order = deliveredOrder("returner@example.com", sku, 2);
        assertThat(shop.onHand(sku)).isEqualTo(3);

        String body = """
                {"token":"%s","lines":[{"lineId":%d,"quantity":1,"reason":"TOO_SMALL"}],"comment":"Lovely but tight"}"""
                .formatted(order.token(), lineId(order));
        String created = mvc.perform(post("/api/v1/orders/" + order.number() + "/returns").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.itemsValue").value(1000))
                .andReturn().getResponse().getContentAsString();
        Integer returnId = JsonPath.read(created, "$.id");
        Integer returnLine = JsonPath.read(created, "$.lines[0].id");

        // Only 1 more can be returned while this one is open.
        mvc.perform(post("/api/v1/orders/" + order.number() + "/returns").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"quantity\":1", "\"quantity\":2")))
                .andExpect(status().isBadRequest());

        // Fulfilment can't approve; Support can.
        shop.as(shop.staff("FULFILMENT"), post("/api/v1/admin/returns/" + returnId + "/approve").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        shop.as(shop.staff("SUPPORT"), post("/api/v1/admin/returns/" + returnId + "/approve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Drop at the boutique\"}")).andExpect(jsonPath("$.status").value("APPROVED"));
        shop.as(shop.staff("FULFILMENT"), post("/api/v1/admin/returns/" + returnId + "/receive").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restockLineIds\":[" + returnLine + "]}"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.lines[0].restocked").value(true));
        assertThat(shop.onHand(sku)).isEqualTo(4);

        shop.as(shop.staff("MANAGER"), post("/api/v1/admin/returns/" + returnId + "/refund").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundAmount").value(1000));
        mvc.perform(get("/api/v1/orders/" + order.number()).param("token", order.token()))
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.lines[0].returnedQuantity").value(1));
        assertThat(variant).isPositive();
    }

    @Test
    void returnWindowIsEnforced() throws Exception {
        TestShop.PlacedOrder order = deliveredOrder("late@example.com", "WST-NAT-OS", 1);
        jdbc.sql("UPDATE orders SET delivered_at = now() - interval '20 days' WHERE id = :id").param("id", order.id()).update();
        mvc.perform(post("/api/v1/orders/" + order.number() + "/returns").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + order.token() + "\",\"lines\":[{\"lineId\":" + lineId(order)
                                + ",\"quantity\":1,\"reason\":\"CHANGED_MIND\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("14-day")));
    }

    @Test
    void verifiedBuyerReviewsAppearAfterModeration() throws Exception {
        shop.createLiveProduct("REV-1", 5);
        long productId = jdbc.sql("SELECT product_id FROM product_variants WHERE sku = 'REV-1'").query(Long.class).single();
        String slug = jdbc.sql("SELECT slug FROM products WHERE id = :p").param("p", productId).query(String.class).single();
        TestShop.PlacedOrder notDelivered = shop.buyAndPay("early@example.com", null, "REV-1", 1);
        String review = "{\"token\":\"%s\",\"productId\":" + productId + ",\"rating\":4,\"title\":\"Lovely\",\"body\":\"Soft fabric\",\"fit\":\"RUNS_SMALL\"}";
        mvc.perform(post("/api/v1/orders/" + notDelivered.number() + "/reviews").contentType(MediaType.APPLICATION_JSON)
                .content(review.formatted(notDelivered.token()))).andExpect(status().isBadRequest());

        TestShop.PlacedOrder order = deliveredOrder("reviewer@example.com", "REV-1", 1);
        String created = mvc.perform(post("/api/v1/orders/" + order.number() + "/reviews").contentType(MediaType.APPLICATION_JSON)
                        .content(review.formatted(order.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.authorName").value("Amina W."))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(created, "$.id");
        mvc.perform(post("/api/v1/orders/" + order.number() + "/reviews").contentType(MediaType.APPLICATION_JSON)
                .content(review.formatted(order.token()))).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/catalog/products/" + slug + "/reviews")).andExpect(jsonPath("$.count").value(0));
        shop.as(shop.staff("MARKETING"), get("/api/v1/admin/reviews"))
                .andExpect(jsonPath("$[*].id").value(hasItem(id)));
        shop.as(shop.staff("MARKETING"), post("/api/v1/admin/reviews/" + id + "/approve")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalog/products/" + slug + "/reviews"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.average").value(4.0))
                .andExpect(jsonPath("$.fit.runsSmall").value(1))
                .andExpect(jsonPath("$.reviews[0].verifiedBuyer").value(true));
    }

    @Test
    void marketingRunsCodesAndAutomaticSales() throws Exception {
        String marketing = shop.staff("MARKETING");
        shop.as(marketing, post("/api/v1/admin/promotions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"later20\",\"name\":\"Future\",\"type\":\"PERCENT\",\"value\":20,"
                                + "\"startsAt\":\"2099-01-01T00:00:00Z\",\"active\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LATER20"))
                .andExpect(jsonPath("$.state").value("Scheduled"));
        shop.as(marketing, post("/api/v1/admin/promotions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"LATER20\",\"name\":\"Dup\",\"type\":\"FIXED\",\"value\":100,\"active\":true}"))
                .andExpect(status().isConflict());
        shop.as(marketing, post("/api/v1/admin/promotions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bad\",\"type\":\"PERCENT\",\"value\":150,\"active\":true}"))
                .andExpect(status().isBadRequest());

        String cart = shop.newCart();
        shop.add(cart, "CPS-WHT-M", 1);
        mvc.perform(put("/api/v1/carts/" + cart + "/discount").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"LATER20\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This code isn't active yet"));

        // Automatic sale (no code), turned off again afterwards so other tests aren't affected.
        String sale = shop.as(marketing, post("/api/v1/admin/promotions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Weekend sale\",\"type\":\"FIXED\",\"value\":500,\"minSubtotal\":7000,\"active\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer saleId = JsonPath.read(sale, "$.id");
        mvc.perform(get("/api/v1/carts/" + cart))
                .andExpect(jsonPath("$.discount.name").value("Weekend sale"))
                .andExpect(jsonPath("$.discount.amount").value(500));
        shop.as(marketing, put("/api/v1/admin/promotions/" + saleId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Weekend sale\",\"type\":\"FIXED\",\"value\":500,\"active\":false}"))
                .andExpect(jsonPath("$.state").value("Off"));
        mvc.perform(get("/api/v1/carts/" + cart)).andExpect(jsonPath("$.discount").doesNotExist());

        shop.as(shop.staff("FULFILMENT"), get("/api/v1/admin/promotions")).andExpect(status().isForbidden());
    }

    @Test
    void contentIsDraftedPreviewedThenPublished() throws Exception {
        String marketing = shop.staff("MARKETING");
        mvc.perform(get("/api/v1/content/pages/returns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value(org.hamcrest.Matchers.containsString("14 days")));

        shop.as(marketing, put("/api/v1/admin/content/pages/returns").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Returns\",\"body\":\"Return within 30 days.\"}"))
                .andExpect(jsonPath("$.hasUnpublishedChanges").value(true));
        mvc.perform(get("/api/v1/content/pages/returns"))
                .andExpect(jsonPath("$.body").value(not("Return within 30 days.")));
        shop.as(marketing, post("/api/v1/admin/content/pages/returns/publish"))
                .andExpect(jsonPath("$.hasUnpublishedChanges").value(false));
        mvc.perform(get("/api/v1/content/pages/returns")).andExpect(jsonPath("$.body").value("Return within 30 days."));

        shop.as(marketing, put("/api/v1/admin/content/site/home").contentType(MediaType.APPLICATION_JSON)
                .content("{\"heroTitle\":\"Karibu\",\"heroText\":\"New in\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/content/site")).andExpect(jsonPath("$.home.heroTitle").value("Curated for you."));
        shop.as(marketing, post("/api/v1/admin/content/site/home/publish")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/content/site")).andExpect(jsonPath("$.home.heroTitle").value("Karibu"));

        shop.as(shop.staff("INVENTORY"), put("/api/v1/admin/content/pages/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"body\":\"y\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/content/pages/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void ownerSeesSalesReport() throws Exception {
        shop.buyAndPay("report@example.com", null, "LWD-SAND-M", 1);
        shop.as(shop.owner(), get("/api/v1/admin/reports/summary").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(greaterThan(0)))
                .andExpect(jsonPath("$.daily.length()").value(7))
                .andExpect(jsonPath("$.topProducts[0].revenue").value(greaterThan(0.0)));
        shop.as(shop.staff("FULFILMENT"), get("/api/v1/admin/reports/summary")).andExpect(status().isForbidden());
    }
}
