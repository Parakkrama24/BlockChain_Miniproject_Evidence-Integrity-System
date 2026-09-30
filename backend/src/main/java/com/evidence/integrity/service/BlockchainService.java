package com.evidence.integrity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Bytes32;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Service
public class BlockchainService {
    private final Web3j web3j;
    private final String contractAddress;

    public BlockchainService(Web3j web3j,
                             @Value("${evidence.blockchain.contract-address}") String contractAddress) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
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
        List<Type> result = call(function);
        return new OnChainEvidence(
                (String) result.get(0).getValue(),
                (String) result.get(1).getValue(),
                Numeric.toHexStringNoPrefix((byte[]) result.get(2).getValue()),
                (String) result.get(3).getValue(),
                ((BigInteger) result.get(4).getValue()).longValue());
    }

    private List<Type> call(Function function) throws Exception {
        if (contractAddress == null || contractAddress.isBlank()) {
            throw new IllegalStateException("EVIDENCE_REGISTRY_ADDRESS is not configured");
        }
        EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        null, contractAddress, FunctionEncoder.encode(function)),
                DefaultBlockParameterName.LATEST).send();
        if (response.hasError()) {
            throw new IllegalStateException("Blockchain call failed: " + response.getError().getMessage());
        }
        return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
    }

    public record OnChainEvidence(String caseId, String fileName, String storedHash,
                                  String uploader, long timestamp) { }
}