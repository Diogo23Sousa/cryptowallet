package com.project.cryptowallet.service;

import com.project.cryptowallet.client.CoinCapClient;
import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.model.WalletAssetHistory;
import com.project.cryptowallet.repository.WalletAssetHistoryRepository;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletAssetRepository walletAssetRepository;
    private final WalletAssetHistoryRepository walletAssetHistoryRepository;
    private final CoinCapClient coinCapClient;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<String, String> symbolToIdMap = new ConcurrentHashMap<>();
    private long frequencyInSeconds;

    public WalletServiceImpl(WalletAssetRepository walletAssetRepository,
                             WalletAssetHistoryRepository walletAssetHistoryRepository,
                             CoinCapClient coinCapClient,
                             @Value("${price.update.frequency:10}") long defaultFrequency) {
        this.walletAssetRepository = walletAssetRepository;
        this.walletAssetHistoryRepository = walletAssetHistoryRepository;
        this.coinCapClient = coinCapClient;
        this.frequencyInSeconds = defaultFrequency;

        refreshSymbolToIdMap();
        startScheduledTask();
    }

    // Save assets
    @Override
    public void saveAssets(List<WalletAsset> assets) {
        walletAssetRepository.saveAll(assets);
        System.out.println("Assets saved successfully at: " + LocalDateTime.now());
    }

    // Update prices concurrently
    @Override
    public void updatePricesConcurrently() {
        System.out.println("Price update process started at: " + LocalDateTime.now());
        refreshSymbolToIdMap();

        List<WalletAsset> assets = walletAssetRepository.findAll();
        Semaphore semaphore = new Semaphore(3);
        List<Future<?>> futures = new ArrayList<>();

        for (WalletAsset asset : assets) {
            String symbol = asset.getSymbol().toUpperCase();
            String assetId = symbolToIdMap.get(symbol);

            if (assetId != null) {
                try {
                    semaphore.acquire();
                    System.out.println("Fetching price for asset: " + symbol + " at " + LocalDateTime.now());

                    Future<?> future = executor.submit(() -> {
                        try {
                            BigDecimal latestPrice = coinCapClient.getLatestPrice(assetId);
                            asset.setLatestPrice(latestPrice);

                            WalletAssetHistory history = new WalletAssetHistory(
                                    asset.getSymbol(), latestPrice, LocalDateTime.now(), asset
                            );
                            walletAssetHistoryRepository.save(history);
                            walletAssetRepository.save(asset);

                            System.out.println("Updated price for " + symbol + ": $"
                                    + latestPrice.setScale(2, RoundingMode.HALF_UP) + " at " + LocalDateTime.now());
                        } catch (Exception e) {
                            System.err.println("Error updating price for " + symbol + " at " + LocalDateTime.now() + ": " + e.getMessage());
                        } finally {
                            semaphore.release();
                        }
                    });

                    futures.add(future);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Error acquiring semaphore for " + symbol + " at " + LocalDateTime.now());
                }
            }
        }

        // Wait for all futures to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error completing update task: " + e.getMessage());
            }
        }

        System.out.println("Price update process completed at: " + LocalDateTime.now());
    }

    @Override
    public WalletSummaryResponse getWalletSummary(LocalDateTime timestamp) {
        if (timestamp == null) {
            return getCurrentWalletSummary();
        } else {
            return getHistoricalWalletSummary(timestamp);
        }
    }

    private WalletSummaryResponse getCurrentWalletSummary() {
        System.out.println("Generating current wallet summary at: " + LocalDateTime.now());
        List<WalletAsset> assets = walletAssetRepository.findAll();
        return calculateSummary(assets, null);
    }

    private WalletSummaryResponse getHistoricalWalletSummary(LocalDateTime timestamp) {
        System.out.println("Generating historical wallet summary at: " + timestamp);
        List<WalletAsset> assets = walletAssetRepository.findAll();
        return calculateSummary(assets, timestamp);
    }

    private WalletSummaryResponse calculateSummary(List<WalletAsset> assets, LocalDateTime timestamp) {
        BigDecimal totalValue = BigDecimal.ZERO;
        WalletAsset bestAsset = null, worstAsset = null;
        BigDecimal bestPerformance = BigDecimal.valueOf(-Double.MAX_VALUE);
        BigDecimal worstPerformance = BigDecimal.valueOf(Double.MAX_VALUE);

        for (WalletAsset asset : assets) {
            BigDecimal priceToUse;

            if (timestamp == null) {
                priceToUse = asset.getLatestPrice();
            } else {
                List<WalletAssetHistory> historyEntries = walletAssetHistoryRepository
                        .findByWalletAssetIdAndUpdatedAtBeforeOrderByUpdatedAtDesc(asset.getId(), timestamp);
                if (historyEntries.isEmpty()) continue;
                priceToUse = historyEntries.getFirst().getPrice();
            }

            if (priceToUse == null) continue;

            BigDecimal performance = priceToUse.subtract(asset.getPrice())
                    .divide(asset.getPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            totalValue = totalValue.add(asset.getQuantity().multiply(priceToUse));

            if (performance.compareTo(bestPerformance) > 0) {
                bestPerformance = performance;
                bestAsset = asset;
            }
            if (performance.compareTo(worstPerformance) < 0) {
                worstPerformance = performance;
                worstAsset = asset;
            }
        }

        return new WalletSummaryResponse(
                totalValue.setScale(2, RoundingMode.HALF_UP),
                bestAsset != null ? bestAsset.getSymbol() : "N/A",
                bestPerformance.setScale(2, RoundingMode.HALF_UP),
                worstAsset != null ? worstAsset.getSymbol() : "N/A",
                worstPerformance.setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Override
    public void setUpdateFrequency(long frequencyInSeconds) {
        System.out.println("Updating scheduler frequency to: " + frequencyInSeconds + " seconds.");
        this.frequencyInSeconds = frequencyInSeconds;

        scheduler.shutdownNow();
        scheduler = Executors.newScheduledThreadPool(1);
        startScheduledTask();
    }

    private void startScheduledTask() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            System.out.println("Scheduler running at: " + now + " | Frequency: " + frequencyInSeconds + " seconds");
            updatePricesConcurrently();
        }, 0, frequencyInSeconds, TimeUnit.SECONDS);
    }


    private void refreshSymbolToIdMap() {
        this.symbolToIdMap = coinCapClient.fetchValidAssets();
    }
}
