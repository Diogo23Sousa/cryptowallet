package com.project.cryptowallet.service;

import com.project.cryptowallet.client.CoinCapClient;
import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.model.WalletAssetHistory;
import com.project.cryptowallet.repository.WalletAssetHistoryRepository;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

        // Manually create the service with mock dependencies
        walletService = new WalletServiceImpl(
                walletAssetRepository,
                walletAssetHistoryRepository,
                coinCapClient,
                10 // default frequency
        );

        // Inject the mock scheduler
        ReflectionTestUtils.setField(walletService, "scheduler", scheduledExecutorService);
    }

    @Test
    public void testSaveAssets() {
        // Arrange
        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(BigDecimal.TEN, 1L, "BTC", BigDecimal.ONE, null),
                new WalletAsset(BigDecimal.valueOf(500), 2L, "ETH", BigDecimal.valueOf(2), null)
        );

        // Act
        walletService.saveAssets(assets);

        // Assert
        verify(walletAssetRepository, times(1)).saveAll(assets);
    }

    @Test
    public void testUpdatePricesConcurrently() {
        // Arrange
        Map<String, String> symbolToIdMap = new HashMap<>();
        symbolToIdMap.put("BTC", "bitcoin");
        symbolToIdMap.put("ETH", "ethereum");

        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(BigDecimal.TEN, 1L, "BTC", BigDecimal.ONE, null),
                new WalletAsset(BigDecimal.valueOf(500), 2L, "ETH", BigDecimal.valueOf(2), null)
        );

        // Stubbing
        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);
        when(walletAssetRepository.findAll()).thenReturn(assets);
        when(coinCapClient.getLatestPrice(anyString())).thenReturn(BigDecimal.valueOf(50000));

        // Act
        walletService.updatePricesConcurrently();

        // Assert
        verify(walletAssetRepository, times(assets.size())).save(any(WalletAsset.class));
        verify(walletAssetHistoryRepository, times(assets.size())).save(any(WalletAssetHistory.class));
    }

    @Test
    public void testGetWalletSummary() {
        // Arrange
        Map<String, String> symbolToIdMap = new HashMap<>();
        symbolToIdMap.put("BTC", "bitcoin");
        symbolToIdMap.put("ETH", "ethereum");

        // Assets with initial values
        WalletAsset asset1 = new WalletAsset(BigDecimal.ZERO, 1L, "BTC", BigDecimal.valueOf(0.5), null);
        WalletAsset asset2 = new WalletAsset(BigDecimal.ZERO, 2L, "ETH", BigDecimal.valueOf(2), null);

        List<WalletAsset> assets = Arrays.asList(asset1, asset2);

        // Mock repository calls
        when(walletAssetRepository.findAll()).thenReturn(assets);

        // Mock API call to fetch asset prices
        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);
        when(coinCapClient.getLatestPrice("bitcoin")).thenReturn(BigDecimal.valueOf(50000));
        when(coinCapClient.getLatestPrice("ethereum")).thenReturn(BigDecimal.valueOf(3000));

        // Act
        WalletSummaryResponse summary = walletService.getWalletSummary(LocalDateTime.now());

        // Expected total wallet value
        BigDecimal expectedTotalValue = BigDecimal.valueOf(50000 * 0.5 + 3000 * 2)
                .setScale(2, RoundingMode.HALF_UP);

        // Assert
        assertEquals(expectedTotalValue, summary.getTotalValue(),
                "Total wallet value should match the expected calculation");

        // Verify interactions
        verify(walletAssetRepository, atLeastOnce()).findAll();
        verify(coinCapClient, times(1)).getLatestPrice("bitcoin");
        verify(coinCapClient, times(1)).getLatestPrice("ethereum");
    }


    @Test
    public void testSetUpdateFrequency() {
        // Act
        walletService.setUpdateFrequency(20);

        // Assert
        verify(scheduledExecutorService).shutdownNow();
    }

    @Test
    public void testRefreshSymbolToIdMap() {
        // Arrange
        Map<String, String> symbolToIdMap = new HashMap<>();
        symbolToIdMap.put("BTC", "bitcoin");
        symbolToIdMap.put("ETH", "ethereum");

        // Stubbing
        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);

        // Act & Assert
        walletService.updatePricesConcurrently();

        // Verify that fetchValidAssets is called during the method execution
        verify(coinCapClient, atLeastOnce()).fetchValidAssets();
    }

    @Test
    public void testProcessAssetPriceUpdateWithNonExistentSymbol() {
        Map<String, String> symbolToIdMap = new HashMap<>();
        WalletAsset asset = new WalletAsset(BigDecimal.TEN, 1L, "NONEXISTENT", BigDecimal.ONE, null);

        when(coinCapClient.fetchValidAssets()).thenReturn(symbolToIdMap);
        when(walletAssetRepository.findAll()).thenReturn(Arrays.asList(asset));

        walletService.updatePricesConcurrently();

        verify(walletAssetRepository, never()).save(asset);
    }
}