package com.evidence.integrity.controller;

import com.evidence.integrity.service.InvestigatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/investigators")
public class InvestigatorController {
    private final InvestigatorService investigatorService;

    public InvestigatorController(InvestigatorService investigatorService) {
        this.investigatorService = investigatorService;
    }

    @GetMapping
    public List<String> list() {
        return investigatorService.listInvestigators();
    }

    @PostMapping
    public ResponseEntity<Void> authorize(@RequestBody AuthorizeRequest request) throws Exception {
        investigatorService.authorize(request.address());
        return ResponseEntity.ok().build();
    }

    public record AuthorizeRequest(String address) { }
}
