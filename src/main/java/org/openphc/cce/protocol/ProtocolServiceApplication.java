package org.openphc.cce.protocol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * CCE Protocol Service — the definitional plane of the CCE platform.
 *
 * <p>Sole writer of {@code protocol_definition}, {@code action_definition} and
 * {@code trigger_index}. It parses and validates FHIR R4 PlanDefinition and ActivityDefinition
 * resources on the way in and decomposes each action's triggers into the flat index the Matcher
 * Service reads on every inbound event.
 *
 * <p>It owns no runtime state: no protocol instances, no steps, no deviations. Downstream services
 * read these tables directly and pick up changes by polling, so this service publishes no events.
 *
 * <p>scanBasePackages is widened to {@code org.openphc.cce} so the beans cce-common-util
 * contributes (PlanDefinitionParser, FhirExpressionEvaluator) are found alongside this
 * service's own.
 */
@SpringBootApplication(scanBasePackages = "org.openphc.cce")
// @Entity and @Repository types are not picked up by component scanning, so both are pointed at
// org.openphc.cce as well: the shared entities and repositories live in cce-common-util.
@EntityScan("org.openphc.cce")
@EnableJpaRepositories("org.openphc.cce")
public class ProtocolServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProtocolServiceApplication.class, args);
    }
}
