package com.evidence.integrity.repository;

import com.evidence.integrity.model.Investigator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigatorRepository extends JpaRepository<Investigator, String> {
}
