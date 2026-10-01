package com.evidence.integrity.service;

import org.springframework.stereotype.Service;

import java.util.List;

// A fixed set of Hardhat's well-known default test accounts, used only so the
// demo UI can let a presenter pick which account signs a registration request --
// e.g. to show an unauthorized account getting rejected, then succeeding after
// being authorized. These keys are publicly known (same ones `npx hardhat node`
// prints) and only ever usable against a local chain.
@Service
public class DemoSignerService {
    public record DemoSigner(String address, String privateKey, String label) { }

    private final List<DemoSigner> signers = List.of(
            new DemoSigner(
                    "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
                    "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80",
                    "Account #0 (admin, pre-authorized)"),
            new DemoSigner(
                    "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
                    "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d",
                    "Account #1"),
            new DemoSigner(
                    "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
                    "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a",
                    "Account #2"),
            new DemoSigner(
                    "0x90F79bf6EB2c4f870365E785982E1f101E93b906",
                    "0x7c852118294e51e653712a81e05800f419141751be58f605c371e15141b007a6",
                    "Account #3"));

    public List<DemoSigner> listSigners() {
        return signers;
    }

    public String privateKeyFor(String address) {
        return signers.stream()
                .filter(signer -> signer.address().equalsIgnoreCase(address))
                .findFirst()
                .map(DemoSigner::privateKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown demo signer address: " + address));
    }

    public String defaultAddress() {
        return signers.get(0).address();
    }
}
