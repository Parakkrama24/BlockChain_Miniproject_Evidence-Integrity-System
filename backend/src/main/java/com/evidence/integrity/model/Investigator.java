package com.evidence.integrity.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// The contract's isInvestigator mapping has no enumeration, and addInvestigator
// emits no event -- so the list of authorized addresses is tracked here,
// recorded whenever this backend successfully authorizes one on-chain.
@Entity
@Table(name = "investigators")
public class Investigator {
    @Id
    private String address;
    private Instant authorizedAt;

    protected Investigator() { }

    public Investigator(String address, Instant authorizedAt) {
        this.address = address;
        this.authorizedAt = authorizedAt;
    }

    public String getAddress() { return address; }
    public Instant getAuthorizedAt() { return authorizedAt; }
}
