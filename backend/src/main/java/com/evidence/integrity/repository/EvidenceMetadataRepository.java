package com.evidence.integrity.repository;

import com.evidence.integrity.model.EvidenceMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceMetadataRepository extends JpaRepository<EvidenceMetadata, Long> {
}