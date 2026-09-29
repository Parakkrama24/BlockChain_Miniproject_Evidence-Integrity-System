package com.evidence.integrity.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evidence_metadata")
public class EvidenceMetadata {
    @Id
    private Long id;
    private String caseId;
    private String fileName;
    private String storagePath;
    private String sha256Hash;
    private String uploader;
    private Instant uploadedAt;
    private Long chainEvidenceId;
    private String txHash;

    protected EvidenceMetadata() { }

    public Long getId() { return id; }
    public String getCaseId() { return caseId; }
    public String getFileName() { return fileName; }
    public String getStoragePath() { return storagePath; }
    public String getSha256Hash() { return sha256Hash; }
    public String getUploader() { return uploader; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Long getChainEvidenceId() { return chainEvidenceId; }
    public String getTxHash() { return txHash; }
}