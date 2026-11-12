package com.yvette.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full application + real Postgres (Testcontainers) + demo seed data + dev owner account.
 * All test classes share one Spring context and one database container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "yvette.media.local-dir=target/test-media")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public @interface ApiIntegrationTest {}
