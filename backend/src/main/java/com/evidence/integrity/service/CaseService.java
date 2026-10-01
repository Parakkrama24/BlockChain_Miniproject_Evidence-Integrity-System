package com.evidence.integrity.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CaseService {
    private final BlockchainService blockchainService;

    public CaseService(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    public List<CaseSummary> listCases() throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BlockchainService.AuditEntry entry : blockchainService.getAuditTrail()) {
            counts.merge(entry.caseId(), 1L, Long::sum);
        }
        List<CaseSummary> summaries = new ArrayList<>();
        counts.forEach((caseId, count) -> summaries.add(new CaseSummary(caseId, count)));
        return summaries;
    }

    public List<EvidenceSummary> getCaseEvidence(String caseId) throws Exception {
        List<EvidenceSummary> summaries = new ArrayList<>();
        for (Long evidenceId : blockchainService.getEvidenceByCase(caseId)) {
            BlockchainService.OnChainEvidence evidence = blockchainService.getEvidence(evidenceId);
            summaries.add(new EvidenceSummary(evidenceId, evidence.caseId(), evidence.fileName(),
                    evidence.storedHash(), evidence.uploader(), evidence.timestamp()));
        }
        return summaries;
    }

    public record CaseSummary(String caseId, long count) { }

    public record EvidenceSummary(long evidenceId, String caseId, String fileName, String sha256Hash,
                                  String uploader, long timestamp) { }
}
