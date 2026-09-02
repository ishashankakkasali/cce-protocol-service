package org.openphc.cce.protocol;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the real application context.
 *
 * <p>Every other test here is a unit test or a narrow {@code @WebMvcTest} slice listing its beans
 * explicitly, so nothing else exercises the actual wiring. That leaves a class of failure invisible to
 * an otherwise high coverage figure: a bean this service never names in source but needs at runtime.
 * {@code scanBasePackages}, {@code @EntityScan} and {@code @EnableJpaRepositories} are all widened to
 * {@code org.openphc.cce}, so the context also instantiates the components cce-common-util
 * contributes — including {@code FhirExpressionEvaluator}, whose constructor requires JSONLogic on
 * the classpath even though this service evaluates no expressions.
 *
 * <p>H2 with Flyway disabled: the point is bean wiring, not the schema, which the owning migrations
 * and the shared data dictionary cover.
 */
@SpringBootTest
@ActiveProfiles("contexttest")
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // Fails on any wiring or missing-runtime-dependency problem.
    }
}
