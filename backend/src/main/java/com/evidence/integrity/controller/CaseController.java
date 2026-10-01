package com.evidence.integrity.controller;

import com.evidence.integrity.service.BlockchainService;
import com.evidence.integrity.service.CaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CaseController {
    private final CaseService caseService;
    private final BlockchainService blockchainService;

    public CaseController(CaseService caseService, BlockchainService blockchainService) {
        this.caseService = caseService;
        this.blockchainService = blockchainService;
    }

    @GetMapping("/cases")
    public List<CaseService.CaseSummary> listCases() throws Exception {
        return caseService.listCases();
    }

    @GetMapping("/cases/{caseId}/evidence")
    public List<CaseService.EvidenceSummary> getCaseEvidence(@PathVariable String caseId) throws Exception {
        return caseService.getCaseEvidence(caseId);
    }

    @GetMapping("/audit-trail")
    public List<BlockchainService.AuditEntry> auditTrail() throws Exception {
        return blockchainService.getAuditTrail();
    }
}
