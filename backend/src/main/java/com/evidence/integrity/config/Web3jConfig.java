package com.evidence.integrity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3jConfig {
    @Bean(destroyMethod = "shutdown")
    Web3j web3j(@Value("${evidence.blockchain.rpc-url}") String rpcUrl) {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    Credentials credentials(@Value("${evidence.blockchain.private-key}") String privateKey) {
        return Credentials.create(privateKey);
    }
}