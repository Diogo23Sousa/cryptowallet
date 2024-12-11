package com.project.cryptowallet.service;

import com.project.cryptowallet.client.CoinCapClient;
import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.model.WalletAssetHistory;
import com.project.cryptowallet.repository.WalletAssetHistoryRepository;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

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

    @Override
    public void saveAssets(List<WalletAsset> assets) {
        walletAssetRepository.saveAll(assets);
        logger.info("Saved successfully: {} Assets", assets.size());
    }


    @Override
    public void updatePricesConcurrently() {
        logger.info("Starting price update process.");
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
                    Future<?> future = executor.submit(() -> {
                        try {
                            BigDecimal latestPrice = coinCapClient.getLatestPrice(assetId);
                            asset.setLatestPrice(latestPrice);

                            WalletAssetHistory history = new WalletAssetHistory(
                                    asset.getSymbol(), latestPrice, LocalDateTime.now(), asset
                            );
                            walletAssetHistoryRepository.save(history);
                            walletAssetRepository.save(asset);

                            logger.info("Updated price for {}: ${}", symbol, latestPrice);
                        } catch (Exception e) {
                            logger.error("Error updating price for {}: {}", symbol, e.getMessage(), e);
                        } finally {
                            semaphore.release();
                        }
                    });

                    futures.add(future);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Error acquiring semaphore for {}", symbol, e);
                }
            }
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error in price update task", e);
            }
        }

        logger.info("Price update process completed.");
    }

    @Override
    public WalletSummaryResponse getWalletSummary(LocalDateTime timestamp) {
        logger.info("Generating wallet summary...");
        List<WalletAsset> assets = walletAssetRepository.findAll();

        // Aggregate by symbol and calculate quantities
        Map<String, BigDecimal> totalQuantities = assets.stream()
                .collect(Collectors.groupingBy(
                        WalletAsset::getSymbol,
                        Collectors.reducing(BigDecimal.ZERO, WalletAsset::getQuantity, BigDecimal::add)
                ));

        BigDecimal totalValue = BigDecimal.ZERO;

        for (WalletAsset asset : assets) {
            BigDecimal price = asset.getLatestPrice() != null ? asset.getLatestPrice() : BigDecimal.ZERO;
            totalValue = totalValue.add(price.multiply(asset.getQuantity()));
        }

        logger.info("Total wallet value: ${}", totalValue);
        totalQuantities.forEach((symbol, quantity) ->
                logger.info("Symbol: {}, Total Quantity: {}", symbol, quantity));

        return new WalletSummaryResponse(
                totalValue.setScale(2, RoundingMode.HALF_UP),
                "N/A", BigDecimal.ZERO, "N/A", BigDecimal.ZERO
        );
    }

    @Override
    public void setUpdateFrequency(long frequencyInSeconds) {
        logger.info("Updating scheduler frequency to {} seconds.", frequencyInSeconds);
        this.frequencyInSeconds = frequencyInSeconds;

        scheduler.shutdownNow();
        scheduler = Executors.newScheduledThreadPool(1);
        startScheduledTask();
    }

    private void startScheduledTask() {
        scheduler.scheduleAtFixedRate(this::updatePricesConcurrently, 0, frequencyInSeconds, TimeUnit.SECONDS);
    }

    private void refreshSymbolToIdMap() {
        this.symbolToIdMap = coinCapClient.fetchValidAssets();
        logger.info("Symbol-to-ID map refreshed.");
    }
}
