package com.project.cryptowallet.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CoinCapClient {

    private final RestTemplate restTemplate;

    @Value("${coincap.api.base-url}")
    private String baseUrl;

    public CoinCapClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch valid assets (symbol to ID mapping) from CoinCap.
     *
     * @return A map where the key is the symbol (e.g., "BTC") and the value is the CoinCap ID (e.g., "bitcoin").
     */
    public Map<String, String> fetchValidAssets() {
        String url = baseUrl + "/assets";
        Map<String, String> symbolToIdMap = new ConcurrentHashMap<>();

        try {
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                for (Map<String, Object> asset : dataList) {
                    String symbol = ((String) asset.get("symbol")).toUpperCase();
                    String id = (String) asset.get("id");
                    symbolToIdMap.put(symbol, id); // Map BTC -> bitcoin
                }
            }
            return symbolToIdMap;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch valid assets from CoinCap", e);
        }
    }

    /**
     * Fetch the latest price for a given asset ID.
     *
     * @param assetId The CoinCap asset ID.
     * @return The latest price as a BigDecimal.
     */
    public BigDecimal getLatestPrice(String assetId) {
        String url = baseUrl + "/assets/" + assetId;
        try {
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String priceUsd = (String) data.get("priceUsd");
                return new BigDecimal(priceUsd).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch price for asset ID: " + assetId, e);
        }
        throw new RuntimeException("Price not found for asset ID: " + assetId);
    }
}
