package com.project.cryptowallet.service;

import com.project.cryptowallet.client.CoinCapClient;
import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.model.WalletAssetHistory;
import com.project.cryptowallet.repository.WalletAssetHistoryRepository;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ScheduledExecutorService;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

    @Mock
    private WalletAssetRepository walletAssetRepository;

    @Mock
    private WalletAssetHistoryRepository walletAssetHistoryRepository;

    @Mock
    private CoinCapClient coinCapClient;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    private WalletServiceImpl walletService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        walletService = new WalletServiceImpl(
                walletAssetRepository,
                walletAssetHistoryRepository,
                coinCapClient,
                10
        );

        ReflectionTestUtils.setField(walletService, "scheduler", scheduledExecutorService);
    }

    @Test
    @Order(1)
    @DisplayName("1. Save Wallet Assets")
    public void testSaveAssets() {
        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(BigDecimal.TEN, 1L, "BTC", BigDecimal.ONE, null),
                new WalletAsset(BigDecimal.valueOf(500), 2L, "ETH", BigDecimal.valueOf(2), null)
        );

        walletService.saveAssets(assets);

        verify(walletAssetRepository, times(1)).saveAll(assets);
    }

    @Test
    @Order(2)
    @DisplayName("2. Update Prices Concurrently")
    public void testUpdatePricesConcurrently() {
        Map<String, String> symbolToIdMap = new HashMap<>();
        symbolToIdMap.put("BTC", "bitcoin");
        symbolToIdMap.put("ETH", "ethereum");

        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(BigDecimal.TEN, 1L, "BTC", BigDecimal.ONE, null),
                new WalletAsset(BigDecimal.valueOf(500), 2L, "ETH", BigDecimal.valueOf(2), null)
        );

        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);
        when(walletAssetRepository.findAll()).thenReturn(assets);
        when(coinCapClient.getLatestPrice(anyString())).thenReturn(BigDecimal.valueOf(50000));

        walletService.updatePricesConcurrently();

        verify(walletAssetRepository, times(assets.size())).save(any(WalletAsset.class));
        verify(walletAssetHistoryRepository, times(assets.size())).save(any(WalletAssetHistory.class));
    }

    @Test
    @Order(3)
    @DisplayName("3. Get Wallet Summary")
    public void testGetWalletSummary() {
        Map<String, String> symbolToIdMap = new HashMap<>();
        symbolToIdMap.put("BTC", "bitcoin");
        symbolToIdMap.put("ETH", "ethereum");

        WalletAsset asset1 = new WalletAsset(BigDecimal.ZERO, 1L, "BTC", BigDecimal.valueOf(0.5), null);
        WalletAsset asset2 = new WalletAsset(BigDecimal.ZERO, 2L, "ETH", BigDecimal.valueOf(2), null);

        List<WalletAsset> assets = Arrays.asList(asset1, asset2);

        when(walletAssetRepository.findAll()).thenReturn(assets);
        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);
        when(coinCapClient.getLatestPrice("bitcoin")).thenReturn(BigDecimal.valueOf(50000));
        when(coinCapClient.getLatestPrice("ethereum")).thenReturn(BigDecimal.valueOf(3000));

        WalletSummaryResponse summary = walletService.getWalletSummary(LocalDateTime.now());

        BigDecimal expectedTotalValue = BigDecimal.valueOf(50000 * 0.5 + 3000 * 2)
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(expectedTotalValue, summary.getTotalValue(),
                "Total wallet value should match the expected calculation");

        verify(walletAssetRepository, atLeastOnce()).findAll();
        verify(coinCapClient, times(1)).getLatestPrice("bitcoin");
        verify(coinCapClient, times(1)).getLatestPrice("ethereum");
    }

    @Test
    @Order(4)
    @DisplayName("4. Set Update Frequency")
    public void testSetUpdateFrequency() {
        walletService.setUpdateFrequency(20);
        verify(scheduledExecutorService).shutdownNow();
    }

    @Test
    @Order(5)
    @DisplayName("5. Refresh Symbol-to-ID Map")
    public void testRefreshSymbolToIdMap() {
        Map<String, String> symbolToIdMap = new HashMap<>();
        symbolToIdMap.put("BTC", "bitcoin");
        symbolToIdMap.put("ETH", "ethereum");

        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);

        walletService.updatePricesConcurrently();

        verify(coinCapClient, atLeastOnce()).fetchValidAssets();
    }

    @Test
    @Order(6)
    @DisplayName("6. Process Asset Price Update with Non-Existent Symbol")
    public void testProcessAssetPriceUpdateWithNonExistentSymbol() {
        Map<String, String> symbolToIdMap = new HashMap<>();
        WalletAsset asset = new WalletAsset(BigDecimal.TEN, 1L, "NONEXISTENT", BigDecimal.ONE, null);

        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);
        when(walletAssetRepository.findAll()).thenReturn(Arrays.asList(asset));

        walletService.updatePricesConcurrently();

        verify(walletAssetRepository, never()).save(asset);
    }
}
