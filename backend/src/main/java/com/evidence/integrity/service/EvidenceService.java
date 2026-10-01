package com.evidence.integrity.service;

import com.evidence.integrity.model.EvidenceMetadata;
import com.evidence.integrity.repository.EvidenceMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.crypto.Credentials;

import java.io.IOException;
import java.time.Instant;

@Service
public class EvidenceService {
    private final HashingService hashingService;
    private final BlockchainService blockchainService;
    private final EvidenceMetadataRepository repository;
    private final FileStorageService fileStorageService;
    private final DemoSignerService demoSignerService;

    public EvidenceService(HashingService hashingService, BlockchainService blockchainService,
                           EvidenceMetadataRepository repository, FileStorageService fileStorageService,
                           DemoSignerService demoSignerService) {
        this.hashingService = hashingService;
        this.blockchainService = blockchainService;
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.demoSignerService = demoSignerService;
    }

    public RegistrationResponse register(String caseId, String uploader, MultipartFile file,
                                         String signerAddress) throws IOException {
        String fileName = file.getOriginalFilename();
        String hash = hashingService.sha256Hex(file);
        String resolvedSigner = (signerAddress == null || signerAddress.isBlank())
                ? demoSignerService.defaultAddress() : signerAddress;
        Credentials signer = Credentials.create(demoSignerService.privateKeyFor(resolvedSigner));
        try {
            BlockchainService.RegistrationResult result =
                    blockchainService.registerEvidence(caseId, fileName, hash, signer);
            String storagePath = fileStorageService.store(result.evidenceId(), fileName, file);
            Instant uploadedAt = Instant.now();
            repository.save(new EvidenceMetadata(result.evidenceId(), caseId, fileName, storagePath, hash,
                    uploader, uploadedAt, result.evidenceId(), result.txHash()));
            return new RegistrationResponse(result.evidenceId(), fileName, hash, result.txHash(), uploadedAt);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to register evidence on the blockchain: "
                    + exception.getMessage(), exception);
        }
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

    public record RegistrationResponse(long evidenceId, String fileName, String sha256Hash,
                                       String txHash, Instant uploadedAt) { }

    public static class EvidenceNotFoundException extends RuntimeException {
        public EvidenceNotFoundException(long evidenceId) {
            super("Evidence metadata not found: " + evidenceId);
        }
    }
}