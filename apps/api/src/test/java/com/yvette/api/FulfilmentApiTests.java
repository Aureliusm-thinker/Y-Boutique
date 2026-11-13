package com.yvette.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@ApiIntegrationTest
class FulfilmentApiTests {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    TestShop shop;

    @BeforeEach
    void setUp() {
        shop = new TestShop(mvc, jdbc);
    }

    @Test
    void fulfilmentShipsAndDeliversFromTheQueue() throws Exception {
        TestShop.PlacedOrder order = shop.buyAndPay("ship@example.com", null, "LWD-SAND-L", 1);
        String fulfilment = shop.staff("FULFILMENT");

        shop.as(fulfilment, get("/api/v1/admin/orders").param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].number").value(hasItem(order.number())));
        shop.as(fulfilment, post("/api/v1/admin/orders/" + order.id() + "/deliver")).andExpect(status().isBadRequest());
        shop.as(fulfilment, post("/api/v1/admin/orders/" + order.id() + "/ship").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrier\":\"G4S\",\"trackingNumber\":\"G4S123\",\"trackingUrl\":\"https://track.example/G4S123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("SHIPPED"))
                .andExpect(jsonPath("$.order.trackingNumber").value("G4S123"));
        shop.as(fulfilment, post("/api/v1/admin/orders/" + order.id() + "/deliver"))
                .andExpect(jsonPath("$.order.status").value("DELIVERED"));
        mvc.perform(get("/api/v1/orders/" + order.number()).param("token", order.token()))
                .andExpect(jsonPath("$.timeline[*].type").value(hasItem("SHIPPED")))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Fulfilment can't refund; Marketing can't see orders.
        shop.as(fulfilment, post("/api/v1/admin/orders/" + order.id() + "/refund").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100,\"reason\":\"test\"}")).andExpect(status().isForbidden());
        shop.as(shop.staff("MARKETING"), get("/api/v1/admin/orders")).andExpect(status().isForbidden());
    }

    @Test
    void managerRefundsPartlyWithRestockAndSupportHasALimit() throws Exception {
        int before = shop.onHand("CPS-WHT-M");
        TestShop.PlacedOrder order = shop.buyAndPay("refund@example.com", null, "CPS-WHT-M", 2);
        assertThat(shop.onHand("CPS-WHT-M")).isEqualTo(before - 2);
        String detail = shop.as(shop.owner(), get("/api/v1/admin/orders/" + order.id()))
                .andExpect(jsonPath("$.refundable").value(2 * 7900)) // free delivery over KSh 10,000
                .andReturn().getResponse().getContentAsString();
        Integer lineId = JsonPath.read(detail, "$.order.lines[0].id");

        // Support is limited to KSh 5,000 per refund.
        shop.as(shop.staff("SUPPORT"), post("/api/v1/admin/orders/" + order.id() + "/refund").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":7900,\"reason\":\"Too big\"}"))
                .andExpect(status().isBadRequest());

        shop.as(shop.staff("MANAGER"), post("/api/v1/admin/orders/" + order.id() + "/refund").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":7900,\"reason\":\"One shirt returned\",\"restock\":[{\"lineId\":" + lineId + ",\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.order.paymentStatus").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.order.refundable").value(7900))
                .andExpect(jsonPath("$.order.order.lines[0].returnedQuantity").value(1));
        assertThat(shop.onHand("CPS-WHT-M")).isEqualTo(before - 1);

        shop.as(shop.owner(), post("/api/v1/admin/orders/" + order.id() + "/refund").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":99999,\"reason\":\"Too much\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancellingAPaidOrderRefundsAndRestocksEverything() throws Exception {
        int before = shop.onHand("WST-NAT-OS");
        TestShop.PlacedOrder order = shop.buyAndPay("cancel@example.com", null, "WST-NAT-OS", 1);
        shop.as(shop.staff("MANAGER"), post("/api/v1/admin/orders/" + order.id() + "/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Customer changed their mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.order.status").value("CANCELLED"))
                .andExpect(jsonPath("$.order.order.paymentStatus").value("REFUNDED"));
        assertThat(shop.onHand("WST-NAT-OS")).isEqualTo(before);
    }

    @Test
    void supportFindsCustomersAndAddsNotes() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/customer/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"findme@example.com\",\"password\":\"a-good-password\",\"fullName\":\"Findme Kiprono\"}"))
                .andReturn().getResponse().getContentAsString();
        String customer = JsonPath.read(body, "$.accessToken");
        TestShop.PlacedOrder order = shop.buyAndPay("findme@example.com", customer, "LWD-SAND-M", 1);
        String support = shop.staff("SUPPORT");

        for (String q : new String[] {"kiprono", "FINDME@", order.number()}) {
            shop.as(support, get("/api/v1/admin/customers").param("q", q))
                    .andExpect(jsonPath("$.items[0].email").value("findme@example.com"));
        }
        Integer id = JsonPath.read(shop.as(support, get("/api/v1/admin/customers").param("q", "kiprono"))
                .andReturn().getResponse().getContentAsString(), "$.items[0].id");
        shop.as(support, post("/api/v1/admin/customers/" + id + "/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Prefers calls after 5pm\"}"))
                .andExpect(jsonPath("$.notes[0].note").value("Prefers calls after 5pm"))
                .andExpect(jsonPath("$.orders[0].number").value(order.number()));
        shop.as(support, post("/api/v1/admin/orders/" + order.id() + "/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Called to confirm address\"}"))
                .andExpect(jsonPath("$.events[*].message").value(hasItem("Called to confirm address")));
        // Notes are internal: not on the customer's view.
        mvc.perform(get("/api/v1/orders/" + order.number()).param("token", order.token()))
                .andExpect(jsonPath("$.timeline[*].type").value(org.hamcrest.Matchers.not(hasItem("NOTE"))));
        shop.as(shop.staff("FULFILMENT"), get("/api/v1/admin/customers")).andExpect(status().isForbidden());
    }

    @Test
    void guestCanLookUpOrderByNumberAndEmail() throws Exception {
        TestShop.PlacedOrder order = shop.buyAndPay("lookup@example.com", null, "WST-NAT-OS", 1);
        mvc.perform(post("/api/v1/orders/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"" + order.number().toLowerCase() + "\",\"email\":\"LOOKUP@example.com\"}"))
                .andExpect(jsonPath("$.accessToken").value(order.token()));
        mvc.perform(post("/api/v1/orders/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"" + order.number() + "\",\"email\":\"someone-else@example.com\"}"))
                .andExpect(status().isNotFound());
    }
}
