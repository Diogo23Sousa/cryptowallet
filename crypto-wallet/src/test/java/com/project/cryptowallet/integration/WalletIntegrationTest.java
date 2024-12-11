package com.project.cryptowallet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cryptowallet.client.CoinCapClient;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletAssetRepository walletAssetRepository;

    @Mock
    private CoinCapClient coinCapClient;

    @Test
    public void testAddAssets() throws Exception {
        when(coinCapClient.getLatestPrice("BTC")).thenReturn(BigDecimal.valueOf(50000));
        when(coinCapClient.getLatestPrice("ETH")).thenReturn(BigDecimal.valueOf(3000));

        WalletAsset asset1 = new WalletAsset(null, null, "BTC", BigDecimal.valueOf(0.5), null);
        WalletAsset asset2 = new WalletAsset(null, null, "ETH", BigDecimal.valueOf(2), null);

        String requestBody = objectMapper.writeValueAsString(Arrays.asList(asset1, asset2));

        mockMvc.perform(post("/api/wallet/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/wallet/summary"))
                .andExpect(status().isOk());
    }


    @Test
    public void testUpdatePrices() throws Exception {
        WalletAsset asset1 = new WalletAsset(null, null, "BTC", BigDecimal.valueOf(0.5), null);
        WalletAsset asset2 = new WalletAsset(null, null, "ETH", BigDecimal.valueOf(2), null);
        walletAssetRepository.saveAll(Arrays.asList(asset1, asset2));

        mockMvc.perform(get("/api/wallet/update"))
                .andExpect(status().isOk())
                .andExpect(content().string("Prices updated successfully"));

        mockMvc.perform(get("/api/wallet/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").exists());
    }

    @Test
    public void testGetWalletSummary() throws Exception {
        WalletAsset asset1 = new WalletAsset(null, null, "BTC", BigDecimal.valueOf(0.5), BigDecimal.valueOf(50000));
        WalletAsset asset2 = new WalletAsset(null, null, "ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(3000));
        walletAssetRepository.saveAll(Arrays.asList(asset1, asset2));

        mockMvc.perform(get("/api/wallet/summary"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSetUpdateFrequency() throws Exception {
        mockMvc.perform(post("/api/wallet/frequency?frequencyInSeconds=30"))
                .andExpect(status().isOk())
                .andExpect(content().string("Update frequency set to 30 seconds"));
    }
}
