package com.evidence.integrity.controller;

import com.evidence.integrity.model.EvidenceMetadata;
import com.evidence.integrity.service.EvidenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {
    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EvidenceService.RegistrationResponse register(@RequestPart("file") MultipartFile file,
                                                          @RequestParam String caseId,
                                                          @RequestParam String uploader,
                                                          @RequestParam(required = false) String signerAddress)
            throws java.io.IOException {
        return evidenceService.register(caseId, uploader, file, signerAddress);
    }

    @PostMapping(value = "/{evidenceId}/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EvidenceService.VerificationResult verify(@PathVariable long evidenceId,
                                                      @RequestPart("file") MultipartFile file)
            throws java.io.IOException {
        return evidenceService.verify(evidenceId, file);
    }

    @GetMapping("/{evidenceId}")
    public EvidenceMetadata getEvidence(@PathVariable long evidenceId) {
        return evidenceService.getMetadata(evidenceId);
    }

    @ExceptionHandler(EvidenceService.EvidenceNotFoundException.class)
    ResponseEntity<String> handleNotFound(EvidenceService.EvidenceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<String> handleTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("File exceeds the maximum upload size (50MB).");
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<String> handleBlockchainError(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}