package com.yvette.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@ApiIntegrationTest
class ProductCsvApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    TestShop shop;

    @BeforeEach
    void setUp() {
        shop = new TestShop(mvc, jdbc);
    }

    private static final String HEADER =
            "slug,title,category,description,style_tags,image_urls,sku,size,colour,price,compare_at_price,stock_on_hand\r\n";

    private ResultActions importCsv(String token, String csv, boolean dryRun) throws Exception {
        return shop.as(token, post("/api/v1/admin/products/import").param("dryRun", String.valueOf(dryRun))
                .contentType("text/csv").content(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void exportIsASpreadsheetOfEveryVariant() throws Exception {
        String csv = shop.as(shop.owner(), get("/api/v1/admin/products/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("﻿slug,title,status,category"), csv.substring(0, 60));
        assertTrue(csv.contains("linen-wrap-dress,Linen Wrap Dress,active,dresses"));

        // Re-importing an untouched export changes nothing.
        importCsv(shop.owner(), csv, true)
                .andExpect(jsonPath("$.errors", empty()))
                .andExpect(jsonPath("$.productsCreated").value(0))
                .andExpect(jsonPath("$.productsUpdated").value(0));
    }

    @Test
    void previewThenImportCreatesDraftProducts() throws Exception {
        String owner = shop.owner();
        String csv = HEADER
                + ",Kitenge Midi Skirt,Dresses,\"Bold print, cotton\",boho | print,https://placehold.co/800x1000.png,CSV-KIT-S,S,Orange,\"KSh 3,500\",,4\r\n"
                + ",Kitenge Midi Skirt,,,,,CSV-KIT-M,M,Orange,3500,4200,6\r\n";

        importCsv(owner, csv, true)
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.errors", empty()))
                .andExpect(jsonPath("$.productsCreated").value(1))
                .andExpect(jsonPath("$.variantsCreated").value(2))
                .andExpect(jsonPath("$.notes", hasItem(containsString("drafts"))));
        assertEquals(0, count("SELECT count(*) FROM product_variants WHERE sku LIKE 'CSV-KIT-%'"));

        importCsv(owner, csv, false).andExpect(jsonPath("$.applied").value(true));
        assertEquals("DRAFT", jdbc.sql("SELECT status FROM products WHERE slug = 'kitenge-midi-skirt'")
                .query(String.class).single());
        assertEquals(6, shop.onHand("CSV-KIT-M"));
        assertEquals(0, new java.math.BigDecimal("3500.00").compareTo(jdbc.sql(
                "SELECT price FROM product_variants WHERE sku = 'CSV-KIT-S'").query(java.math.BigDecimal.class).single()));

        // Change one price and add a size; stock for the existing SKU is left to Inventory.
        String update = "slug,sku,size,price,stock_on_hand\r\n"
                + "kitenge-midi-skirt,CSV-KIT-S,S,3200,99\r\n"
                + "kitenge-midi-skirt,CSV-KIT-L,L,3500,2\r\n";
        importCsv(owner, update, false)
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.productsUpdated").value(1))
                .andExpect(jsonPath("$.variantsUpdated").value(1))
                .andExpect(jsonPath("$.variantsCreated").value(1))
                .andExpect(jsonPath("$.notes", hasItem(containsString("Inventory"))));
        assertEquals(4, shop.onHand("CSV-KIT-S"));
        assertEquals(2, shop.onHand("CSV-KIT-L"));
        // Columns not in the file (description, colour, images) were kept.
        assertEquals("Bold print, cotton", jdbc.sql("SELECT description FROM products WHERE slug = 'kitenge-midi-skirt'")
                .query(String.class).single());
        assertEquals(3, count("SELECT count(*) FROM product_variants v JOIN products p ON p.id = v.product_id "
                + "WHERE p.slug = 'kitenge-midi-skirt'"));
    }

    @Test
    void anyBadRowStopsTheWholeImportWithLineNumbers() throws Exception {
        String csv = HEADER
                + ",Good Product,dresses,,,,CSV-GOOD-1,,,1000,,1\r\n"
                + ",Bad Category,no-such-category,,,,CSV-BAD-1,,,1000,,1\r\n"
                + ",Bad Price,dresses,,,,CSV-BAD-2,,,abc,,1\r\n"
                + ",Duplicate,dresses,,,,CSV-GOOD-1,,,1000,,1\r\n"
                + ",Taken SKU,dresses,,,,LWD-SAND-S,,,1000,,1\r\n"
                + ",Cheap Sale,dresses,,,,CSV-BAD-3,,,1000,900,1\r\n";
        importCsv(shop.owner(), csv, false)
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.errors", hasSize(5)))
                .andExpect(jsonPath("$.errors[0].line").value(3))
                .andExpect(jsonPath("$.errors[0].message", containsString("Unknown category")))
                .andExpect(jsonPath("$.errors[1].message", containsString("not a valid amount")))
                .andExpect(jsonPath("$.errors[2].message", containsString("already on line 2")))
                .andExpect(jsonPath("$.errors[3].message", containsString("another product")))
                .andExpect(jsonPath("$.errors[4].message", containsString("compare_at_price")));
        assertEquals(0, count("SELECT count(*) FROM product_variants WHERE sku = 'CSV-GOOD-1'"));

        importCsv(shop.owner(), "name,sku\r\nx,y\r\n", true)
                .andExpect(jsonPath("$.errors[0].message", containsString("Unknown column \"name\"")));
    }

    @Test
    void onlyCatalogueEditorsCanImport() throws Exception {
        String marketing = shop.staff("MARKETING");
        shop.as(marketing, get("/api/v1/admin/products/export")).andExpect(status().isOk());
        importCsv(marketing, HEADER, true).andExpect(status().isForbidden());
    }

    private int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }
}
