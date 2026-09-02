package org.openphc.cce.protocol.domain.repository;

import org.openphc.cce.common.entity.ProtocolDefinition;
import org.openphc.cce.common.enums.ProtocolDefinitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProtocolDefinitionRepository extends JpaRepository<ProtocolDefinition, UUID> {

    /** Backs the cce.protocol.definitions.active gauge. */
    long countByStatus(ProtocolDefinitionStatus status);

    Optional<ProtocolDefinition> findByUrlAndVersion(String url, String version);

    List<ProtocolDefinition> findByUrl(String url);
}
