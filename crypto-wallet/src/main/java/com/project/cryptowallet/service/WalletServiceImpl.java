package com.project.cryptowallet.service;

import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.repository.WalletAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.*;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletAssetRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public WalletServiceImpl(WalletAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveAssets(List<WalletAsset> assets) {
        repository.saveAll(assets);
    }

    @Override
    public void updatePricesConcurrently() {
        List<WalletAsset> assets = repository.findAll();
        List<? extends Future<?>> futures = assets.stream()
                .map(asset -> executor.submit(() -> {
                    String url = "https://api.coincap.io/v2/assets/" + asset.getSymbol().toLowerCase();
                    try {
                        String response = restTemplate.getForObject(url, String.class);
                        BigDecimal latestPrice = parsePriceFromResponse(response);
                        asset.setLatestPrice(latestPrice);
                        repository.save(asset);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }))
                .toList();

        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public String getWalletSummary() {
        List<WalletAsset> assets = repository.findAll();
        BigDecimal totalValue = BigDecimal.ZERO;
        WalletAsset bestAsset = null, worstAsset = null;
        BigDecimal bestPerformance = BigDecimal.valueOf(-Double.MAX_VALUE);
        BigDecimal worstPerformance = BigDecimal.valueOf(Double.MAX_VALUE);

        for (WalletAsset asset : assets) {
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

        return String.format(
                "Total: %.2f, Best Asset: %s (%.2f%%), Worst Asset: %s (%.2f%%)",
                totalValue,
                bestAsset != null ? bestAsset.getSymbol() : "N/A", bestPerformance,
                worstAsset != null ? worstAsset.getSymbol() : "N/A", worstPerformance
        );
    }

    @Override
    public List<WalletAsset> getWalletHistory() {
        return repository.findAll();
    }

    private BigDecimal parsePriceFromResponse(String response) {
        String priceString = response.replaceAll("[^0-9.]", "");
        return new BigDecimal(priceString).setScale(2, RoundingMode.HALF_UP);
    }
}
