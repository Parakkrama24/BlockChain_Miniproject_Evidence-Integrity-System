package com.evidence.integrity.service;

import com.evidence.integrity.model.Investigator;
import com.evidence.integrity.repository.InvestigatorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class InvestigatorService {
    private final BlockchainService blockchainService;
    private final InvestigatorRepository repository;
    private final String adminAddress;

    public InvestigatorService(BlockchainService blockchainService, InvestigatorRepository repository,
                               @Value("${evidence.blockchain.admin-address}") String adminAddress) {
        this.blockchainService = blockchainService;
        this.repository = repository;
        this.adminAddress = adminAddress;
    }

    @PostConstruct
    void seedAdmin() {
        if (adminAddress != null && !adminAddress.isBlank() && repository.findById(adminAddress).isEmpty()) {
            repository.save(new Investigator(adminAddress, Instant.now()));
        }
    }

    public List<String> listInvestigators() {
        return repository.findAll().stream().map(Investigator::getAddress).toList();
    }

    public void authorize(String address) throws Exception {
        blockchainService.addInvestigator(address);
        repository.save(new Investigator(address, Instant.now()));
    }
}
