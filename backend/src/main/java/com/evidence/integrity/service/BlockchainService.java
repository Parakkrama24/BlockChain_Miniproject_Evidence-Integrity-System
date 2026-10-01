package com.evidence.integrity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlockchainService {
    // Parameter order/types must match the Solidity event signature exactly --
    // this is what produces the correct topic0 hash for filtering logs.
    private static final Event EVIDENCE_REGISTERED_EVENT = new Event(
            "EvidenceRegistered",
            Arrays.asList(
                    new TypeReference<Uint256>(true) { },
                    new TypeReference<Utf8String>() { },
                    new TypeReference<Bytes32>() { },
                    new TypeReference<Address>() { },
                    new TypeReference<Uint256>() { }));

    private final Web3j web3j;
    private final Credentials adminCredentials;
    private final String contractAddress;

    public BlockchainService(Web3j web3j, Credentials credentials,
                             @Value("${evidence.blockchain.contract-address}") String contractAddress) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
        this.adminCredentials = credentials;
    }

    public boolean verifyHash(long evidenceId, String computedHash) throws Exception {
        Function function = new Function(
                "verifyHash",
                Arrays.asList(new Uint256(evidenceId), new Bytes32(Numeric.hexStringToByteArray("0x" + computedHash))),
                List.of(new TypeReference<Bool>() { }));
        List<Type> result = call(function);
        return (Boolean) result.get(0).getValue();
    }

    public OnChainEvidence getEvidence(long evidenceId) throws Exception {
        Function function = new Function(
                "getEvidence",
                List.of(new Uint256(evidenceId)),
                Arrays.asList(
                        new TypeReference<Utf8String>() { },
                        new TypeReference<Utf8String>() { },
                        new TypeReference<Bytes32>() { },
                        new TypeReference<Address>() { },
                        new TypeReference<Uint256>() { }));
        List<Type> result = callStruct(function);
        return new OnChainEvidence(
                (String) result.get(0).getValue(),
                (String) result.get(1).getValue(),
                Numeric.toHexStringNoPrefix((byte[]) result.get(2).getValue()),
                (String) result.get(3).getValue(),
                ((BigInteger) result.get(4).getValue()).longValue());
    }

    @SuppressWarnings("unchecked")
    public List<Long> getEvidenceByCase(String caseId) throws Exception {
        Function function = new Function(
                "getEvidenceByCase",
                List.of(new Utf8String(caseId)),
                List.of(new TypeReference<DynamicArray<Uint256>>() { }));
        List<Type> result = call(function);
        DynamicArray<Uint256> ids = (DynamicArray<Uint256>) result.get(0);
        List<Long> evidenceIds = new ArrayList<>();
        for (Uint256 id : ids.getValue()) {
            evidenceIds.add(id.getValue().longValue());
        }
        return evidenceIds;
    }

    public boolean isInvestigator(String address) throws Exception {
        Function function = new Function(
                "isInvestigator",
                List.of(new Address(address)),
                List.of(new TypeReference<Bool>() { }));
        List<Type> result = call(function);
        return (Boolean) result.get(0).getValue();
    }

    public RegistrationResult registerEvidence(String caseId, String fileName, String sha256Hash,
                                               Credentials signer) throws Exception {
        Function function = new Function(
                "registerEvidence",
                Arrays.asList(
                        new Utf8String(caseId),
                        new Utf8String(fileName),
                        new Bytes32(Numeric.hexStringToByteArray("0x" + sha256Hash))),
                Collections.emptyList());
        TransactionReceipt receipt = sendTransaction(function, signer);
        return new RegistrationResult(decodeEvidenceId(receipt), receipt.getTransactionHash());
    }

    // Always signed by the admin account -- the contract itself enforces that
    // only admin may call addInvestigator, so this is never demo-switchable.
    public String addInvestigator(String address) throws Exception {
        Function function = new Function(
                "addInvestigator",
                List.of(new Address(address)),
                Collections.emptyList());
        return sendTransaction(function, adminCredentials).getTransactionHash();
    }

    public List<AuditEntry> getAuditTrail() throws Exception {
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, contractAddress);
        filter.addSingleTopic(EventEncoder.encode(EVIDENCE_REGISTERED_EVENT));

        List<Log> logs = web3j.ethGetLogs(filter).send().getLogs().stream()
                .map(result -> (Log) result.get())
                .toList();

        List<AuditEntry> entries = new ArrayList<>();
        for (Log log : logs) {
            entries.add(decodeAuditEntry(log));
        }
        return entries;
    }

    private AuditEntry decodeAuditEntry(Log log) {
        EventValues values = Contract.staticExtractEventParameters(EVIDENCE_REGISTERED_EVENT, log);

        long evidenceId = ((BigInteger) values.getIndexedValues().get(0).getValue()).longValue();
        String caseId = (String) values.getNonIndexedValues().get(0).getValue();
        String hash = Numeric.toHexStringNoPrefix((byte[]) values.getNonIndexedValues().get(1).getValue());
        String uploader = (String) values.getNonIndexedValues().get(2).getValue();
        long timestamp = ((BigInteger) values.getNonIndexedValues().get(3).getValue()).longValue();

        return new AuditEntry(evidenceId, caseId, uploader, hash, timestamp, log.getTransactionHash());
    }

    private long decodeEvidenceId(TransactionReceipt receipt) {
        String eventSignature = EventEncoder.encode(EVIDENCE_REGISTERED_EVENT);
        for (Log log : receipt.getLogs()) {
            if (log.getTopics().isEmpty() || !log.getTopics().get(0).equals(eventSignature)) {
                continue;
            }
            EventValues values = Contract.staticExtractEventParameters(EVIDENCE_REGISTERED_EVENT, log);
            return ((BigInteger) values.getIndexedValues().get(0).getValue()).longValue();
        }
        throw new IllegalStateException("EvidenceRegistered event not found in transaction receipt");
    }

    private TransactionReceipt sendTransaction(Function function, Credentials signer) throws Exception {
        requireContractAddress();
        RawTransactionManager transactionManager = new RawTransactionManager(web3j, signer);
        String encodedFunction = FunctionEncoder.encode(function);
        EthSendTransaction response = transactionManager.sendTransaction(
                DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT,
                contractAddress, encodedFunction, BigInteger.ZERO);
        if (response.hasError()) {
            throw new IllegalStateException("Blockchain transaction failed: " + response.getError().getMessage());
        }
        return pollForReceipt(response.getTransactionHash());
    }

    private TransactionReceipt pollForReceipt(String txHash) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            var receiptResponse = web3j.ethGetTransactionReceipt(txHash).send();
            if (receiptResponse.getTransactionReceipt().isPresent()) {
                return receiptResponse.getTransactionReceipt().get();
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("Timed out waiting for transaction receipt: " + txHash);
    }

    private List<Type> call(Function function) throws Exception {
        requireContractAddress();
        EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        null, contractAddress, FunctionEncoder.encode(function)),
                DefaultBlockParameterName.LATEST).send();
        if (response.hasError()) {
            throw new IllegalStateException("Blockchain call failed: " + response.getError().getMessage());
        }
        return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
    }

    // A function that returns a single struct containing dynamic fields (like
    // EvidenceRecord, which has two strings) is ABI-encoded as one dynamic value:
    // a leading offset word wraps the struct's own fields. The flat decode used
    // by call() doesn't expect that extra layer, so it's stripped here first.
    private List<Type> callStruct(Function function) throws Exception {
        requireContractAddress();
        EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        null, contractAddress, FunctionEncoder.encode(function)),
                DefaultBlockParameterName.LATEST).send();
        if (response.hasError()) {
            throw new IllegalStateException("Blockchain call failed: " + response.getError().getMessage());
        }
        String withoutOuterOffset = "0x" + Numeric.cleanHexPrefix(response.getValue()).substring(64);
        return FunctionReturnDecoder.decode(withoutOuterOffset, function.getOutputParameters());
    }

    private void requireContractAddress() {
        if (contractAddress == null || contractAddress.isBlank()) {
            throw new IllegalStateException("EVIDENCE_REGISTRY_ADDRESS is not configured");
        }
    }

    public record OnChainEvidence(String caseId, String fileName, String storedHash,
                                  String uploader, long timestamp) { }

    public record RegistrationResult(long evidenceId, String txHash) { }

    public record AuditEntry(long evidenceId, String caseId, String uploader, String sha256Hash,
                             long timestamp, String txHash) { }
}
