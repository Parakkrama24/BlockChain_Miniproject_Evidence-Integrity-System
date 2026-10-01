package com.evidence.integrity.controller;

import com.evidence.integrity.service.DemoSignerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo-signers")
public class DemoSignerController {
    private final DemoSignerService demoSignerService;

    public DemoSignerController(DemoSignerService demoSignerService) {
        this.demoSignerService = demoSignerService;
    }

    @GetMapping
    public List<PublicSigner> list() {
        return demoSignerService.listSigners().stream()
                .map(signer -> new PublicSigner(signer.address(), signer.label()))
                .toList();
    }

    public record PublicSigner(String address, String label) { }
}
