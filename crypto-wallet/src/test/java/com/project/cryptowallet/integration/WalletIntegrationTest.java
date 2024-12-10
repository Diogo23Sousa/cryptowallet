package com.project.cryptowallet.integration;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WalletIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletAssetRepository walletAssetRepository;

    @Test
    void testAddAssetsAndGetSummary() {
        // 1. Add assets
        WalletAsset asset = new WalletAsset();
        asset.setSymbol("BTC");
        asset.setQuantity(new BigDecimal("0.5"));
        asset.setPrice(new BigDecimal("40000"));

        ResponseEntity<String> addResponse = restTemplate.postForEntity("/api/wallet/add", List.of(asset), String.class);
        assertEquals(200, addResponse.getStatusCodeValue());
        assertTrue(addResponse.getBody().contains("Assets added successfully"));

        // 2. Verify asset is saved
        List<WalletAsset> savedAssets = walletAssetRepository.findAll();
        assertEquals(1, savedAssets.size());
        assertEquals("BTC", savedAssets.get(0).getSymbol());

        // 3. Get summary
        ResponseEntity<String> summaryResponse = restTemplate.getForEntity("/api/wallet/summary", String.class);
        assertEquals(200, summaryResponse.getStatusCodeValue());
        assertTrue(summaryResponse.getBody().contains("BTC"));
    }
}
