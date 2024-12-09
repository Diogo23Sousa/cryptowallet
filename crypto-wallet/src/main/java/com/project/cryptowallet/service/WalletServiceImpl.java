package com.project.cryptowallet.service;

import com.project.cryptowallet.client.CoinCapClient;
import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletAssetRepository repository;
    private final CoinCapClient coinCapClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<String, String> symbolToIdMap = new ConcurrentHashMap<>();
    private long frequencyInSeconds = 5; // Default frequency (30 seconds)

    public WalletServiceImpl(WalletAssetRepository repository, CoinCapClient coinCapClient) {
        this.repository = repository;
        this.coinCapClient = coinCapClient;
        refreshSymbolToIdMap(); // Load symbol-to-ID mapping on startup
        startScheduledTask();
    }

    @Override
    public void saveAssets(List<WalletAsset> assets) {
        repository.saveAll(assets);
    }

    @Override
    public void updatePricesConcurrently() {
        refreshSymbolToIdMap(); // Ensure latest symbol-to-ID map is loaded

        List<WalletAsset> assets = repository.findAll();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Log the start time
        System.out.println("Now is " + LocalDateTime.now().format(timeFormatter));

        Semaphore semaphore = new Semaphore(3); // Allow only 3 tasks to run concurrently

        List<Future<?>> futures = new ArrayList<>();

        for (WalletAsset asset : assets) {
            String symbol = asset.getSymbol().toUpperCase();
            String assetId = symbolToIdMap.get(symbol);

            if (assetId != null) {
                try {
                    semaphore.acquire(); // Acquire a permit to submit the task
                    System.out.println("Submitted request " + symbol + " at "
                            + LocalDateTime.now().format(timeFormatter));

                    Future<?> future = executor.submit(() -> {
                        try {
                            BigDecimal latestPrice = coinCapClient.getLatestPrice(assetId);
                            asset.setLatestPrice(latestPrice);
                            repository.save(asset);
                            System.out.println("Updated price for: " + symbol);
                        } catch (Exception e) {
                            System.err.println("Error updating price for: " + symbol);
                        } finally {
                            semaphore.release(); // Release the permit when the task is complete
                        }
                    });

                    futures.add(future);

                } catch (InterruptedException e) {
                    System.err.println("Error submitting request for: " + symbol);
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Price update task completed.");
    }


    @Override
    public WalletSummaryResponse getWalletSummary() {
        List<WalletAsset> assets = repository.findAll();
        BigDecimal totalValue = BigDecimal.ZERO;
        WalletAsset bestAsset = null, worstAsset = null;
        BigDecimal bestPerformance = BigDecimal.valueOf(-Double.MAX_VALUE);
        BigDecimal worstPerformance = BigDecimal.valueOf(Double.MAX_VALUE);

        for (WalletAsset asset : assets) {
            if (asset.getLatestPrice() == null) {
                System.out.println("Skipping asset with null latest price: " + asset.getSymbol());
                continue; // Skip this asset if latestPrice is null
            }

            BigDecimal performance = asset.getLatestPrice().subtract(asset.getPrice())
                    .divide(asset.getPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            totalValue = totalValue.add(asset.getQuantity().multiply(asset.getLatestPrice()));

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
                bestPerformance.compareTo(BigDecimal.valueOf(-Double.MAX_VALUE)) == 0 ? BigDecimal.ZERO : bestPerformance.setScale(2, RoundingMode.HALF_UP),
                worstAsset != null ? worstAsset.getSymbol() : "N/A",
                worstPerformance.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 0 ? BigDecimal.ZERO : worstPerformance.setScale(2, RoundingMode.HALF_UP)
        );
    }


    @Override
    public List<WalletAsset> getWalletHistory() {
        return repository.findAll();
    }

    @Override
    public void setUpdateFrequency(long frequencyInSeconds) {
        this.frequencyInSeconds = frequencyInSeconds;
        scheduler.shutdownNow();
        startScheduledTask();
    }

    private void startScheduledTask() {
        scheduler.scheduleAtFixedRate(
                this::updatePricesConcurrently,
                0, frequencyInSeconds, TimeUnit.SECONDS
        );
        System.out.println("Scheduled task started with frequency: " + frequencyInSeconds + " seconds");
    }

    /**
     * Refresh the symbol-to-ID map by fetching valid assets from CoinCap.
     */
    private void refreshSymbolToIdMap() {
        try {
            this.symbolToIdMap = coinCapClient.fetchValidAssets();
            System.out.println("Refreshed symbol-to-ID map with " + symbolToIdMap.size() + " entries.");
        } catch (Exception e) {
            System.err.println("Failed to refresh symbol-to-ID map: " + e.getMessage());
            throw new RuntimeException("Failed to fetch valid assets", e);
        }
    }
}
