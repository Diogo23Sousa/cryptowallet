package com.project.cryptowallet.service;

import com.project.cryptowallet.client.CoinCapClient;
import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.model.WalletAssetHistory;
import com.project.cryptowallet.repository.WalletAssetHistoryRepository;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceImplTest {

    private WalletAssetRepository walletAssetRepository;
    private WalletAssetHistoryRepository walletAssetHistoryRepository;
    private CoinCapClient coinCapClient;
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        walletAssetRepository = Mockito.mock(WalletAssetRepository.class);
        walletAssetHistoryRepository = Mockito.mock(WalletAssetHistoryRepository.class);
        coinCapClient = Mockito.mock(CoinCapClient.class);

        walletService = new WalletServiceImpl(walletAssetRepository, walletAssetHistoryRepository, coinCapClient, 10L);
    }

    @Test
    void testSaveAssets() {
        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(new BigDecimal("40000"), null, "BTC", new BigDecimal("0.5"), null),
                new WalletAsset(new BigDecimal("2500"), null, "ETH", new BigDecimal("2.0"), null),
                new WalletAsset(new BigDecimal("0.08"), null, "DOGE", new BigDecimal("1000"), null),
                new WalletAsset(new BigDecimal("300"), null, "ADA", new BigDecimal("50"), null)
        );

        walletService.saveAssets(assets);
        verify(walletAssetRepository, times(1)).saveAll(assets);
    }

    @Test
    void testUpdatePricesConcurrently() throws Exception {
        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(new BigDecimal("40000"), 1L, "BTC", new BigDecimal("0.5"), null),
                new WalletAsset(new BigDecimal("2500"), 2L, "ETH", new BigDecimal("2.0"), null),
                new WalletAsset(new BigDecimal("0.08"), 3L, "DOGE", new BigDecimal("1000"), null),
                new WalletAsset(new BigDecimal("300"), 4L, "ADA", new BigDecimal("50"), null)
        );

        when(walletAssetRepository.findAll()).thenReturn(assets);
        when(coinCapClient.getLatestPrice("BTC")).thenReturn(new BigDecimal("42000"));
        when(coinCapClient.getLatestPrice("ETH")).thenReturn(new BigDecimal("2600"));
        when(coinCapClient.getLatestPrice("DOGE")).thenReturn(new BigDecimal("0.09"));
        when(coinCapClient.getLatestPrice("ADA")).thenReturn(new BigDecimal("320"));

        walletService.updatePricesConcurrently();

        // Wait for asynchronous execution
        TimeUnit.SECONDS.sleep(2);

        verify(walletAssetRepository, times(4)).save(any(WalletAsset.class));
        verify(walletAssetHistoryRepository, times(4)).save(any(WalletAssetHistory.class));
    }

    @Test
    void testGetWalletSummary_Current() {
        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(new BigDecimal("40000"), 1L, "BTC", new BigDecimal("0.5"), new BigDecimal("45000")),
                new WalletAsset(new BigDecimal("2500"), 2L, "ETH", new BigDecimal("2.0"), new BigDecimal("2700")),
                new WalletAsset(new BigDecimal("0.08"), 3L, "DOGE", new BigDecimal("1000"), new BigDecimal("0.10")),
                new WalletAsset(new BigDecimal("300"), 4L, "ADA", new BigDecimal("50"), new BigDecimal("350"))
        );

        when(walletAssetRepository.findAll()).thenReturn(assets);

        WalletSummaryResponse summary = walletService.getWalletSummary(null);

        assertEquals(new BigDecimal("49100.00"), summary.getTotalValue()); // Total value calculation
        assertEquals("DOGE", summary.getBestAsset()); // DOGE with best performance
    }

    @Test
    void testGetWalletSummary_Historical() {
        WalletAsset asset1 = new WalletAsset(new BigDecimal("40000"), 1L, "BTC", new BigDecimal("0.5"), null);
        WalletAssetHistory history1 = new WalletAssetHistory("BTC", new BigDecimal("41000"), LocalDateTime.now().minusDays(1), asset1);

        WalletAsset asset2 = new WalletAsset(new BigDecimal("2500"), 2L, "ETH", new BigDecimal("2.0"), null);
        WalletAssetHistory history2 = new WalletAssetHistory("ETH", new BigDecimal("2550"), LocalDateTime.now().minusDays(1), asset2);

        when(walletAssetRepository.findAll()).thenReturn(Arrays.asList(asset1, asset2));
        when(walletAssetHistoryRepository.findByWalletAssetIdAndUpdatedAtBeforeOrderByUpdatedAtDesc(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(history1))
                .thenReturn(Collections.singletonList(history2));

        WalletSummaryResponse summary = walletService.getWalletSummary(LocalDateTime.now().minusDays(1));

        assertEquals(new BigDecimal("46600.00"), summary.getTotalValue()); // Total value of historical prices
        assertEquals("ETH", summary.getBestAsset());
    }
}
