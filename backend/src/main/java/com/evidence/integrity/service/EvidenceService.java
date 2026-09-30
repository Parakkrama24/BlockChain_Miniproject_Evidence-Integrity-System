package com.evidence.integrity.service;

import com.evidence.integrity.model.EvidenceMetadata;
import com.evidence.integrity.repository.EvidenceMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@Service
public class EvidenceService {
    private final HashingService hashingService;
    private final BlockchainService blockchainService;
    private final EvidenceMetadataRepository repository;

    public EvidenceService(HashingService hashingService, BlockchainService blockchainService,
                           EvidenceMetadataRepository repository) {
        this.hashingService = hashingService;
        this.blockchainService = blockchainService;
        this.repository = repository;
    }

    public VerificationResult verify(long evidenceId, MultipartFile file) throws IOException {
        String computedHash = hashingService.sha256Hex(file);
        try {
            BlockchainService.OnChainEvidence evidence = blockchainService.getEvidence(evidenceId);
            boolean verified = blockchainService.verifyHash(evidenceId, computedHash);
            return new VerificationResult(evidenceId, verified, evidence.storedHash(), computedHash, Instant.now());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify evidence against the blockchain", exception);
        }
    }

    public EvidenceMetadata getMetadata(long evidenceId) {
        return repository.findById(evidenceId)
                .orElseThrow(() -> new EvidenceNotFoundException(evidenceId));
    }

    public record VerificationResult(long evidenceId, boolean verified, String storedHash,
                                     String computedHash, Instant checkedAt) { }

    public static class EvidenceNotFoundException extends RuntimeException {
        public EvidenceNotFoundException(long evidenceId) {
            super("Evidence metadata not found: " + evidenceId);
        }
    }
}