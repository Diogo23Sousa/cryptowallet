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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        List<WalletAsset> assets = Collections.singletonList(new WalletAsset(new BigDecimal("40000"), null, "BTC", new BigDecimal("0.5"), null));
        walletService.saveAssets(assets);
        verify(walletAssetRepository, times(1)).saveAll(assets);
    }

    @Test
    void testUpdatePricesConcurrently() throws Exception {
        WalletAsset asset = new WalletAsset(new BigDecimal("40000"), 1L, "BTC", new BigDecimal("0.5"), null);
        when(walletAssetRepository.findAll()).thenReturn(Collections.singletonList(asset));
        when(coinCapClient.getLatestPrice("BTC")).thenReturn(new BigDecimal("42000"));

        walletService.updatePricesConcurrently();

        verify(walletAssetRepository, times(1)).save(asset);
        verify(walletAssetHistoryRepository, times(1)).save(any(WalletAssetHistory.class));
    }

    @Test
    void testGetWalletSummary_Current() {
        WalletAsset asset = new WalletAsset(new BigDecimal("40000"), 1L, "BTC", new BigDecimal("0.5"), new BigDecimal("45000"));
        when(walletAssetRepository.findAll()).thenReturn(Collections.singletonList(asset));

        WalletSummaryResponse summary = walletService.getWalletSummary(null);

        assertEquals(new BigDecimal("22500.00"), summary.getTotalValue());
        assertEquals("BTC", summary.getBestAsset());
    }

    @Test
    void testGetWalletSummary_Historical() {
        WalletAsset asset = new WalletAsset(new BigDecimal("40000"), 1L, "BTC", new BigDecimal("0.5"), null);
        WalletAssetHistory history = new WalletAssetHistory("BTC", new BigDecimal("41000"), LocalDateTime.now().minusDays(1), asset);

        when(walletAssetRepository.findAll()).thenReturn(Collections.singletonList(asset));
        when(walletAssetHistoryRepository.findByWalletAssetIdAndUpdatedAtBeforeOrderByUpdatedAtDesc(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(history));

        WalletSummaryResponse summary = walletService.getWalletSummary(LocalDateTime.now().minusDays(1));

        assertEquals(new BigDecimal("20500.00"), summary.getTotalValue());
        assertEquals("BTC", summary.getBestAsset());
    }
}
