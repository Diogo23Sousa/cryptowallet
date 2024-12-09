package com.project.cryptowallet.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CoinCapClient {

    private final RestTemplate restTemplate;

    public CoinCapClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final String BASE_URL = "https://api.coincap.io/v2/assets/";

    /**
     * Fetch the latest price for a given cryptocurrency symbol.
     *
     * @param symbol The symbol of the cryptocurrency (e.g., "bitcoin" for BTC).
     * @return The latest price as a BigDecimal.
     * @throws RuntimeException if the API request fails or the price is not found.
     */
    public BigDecimal getLatestPrice(String symbol) {
        String url = BASE_URL + symbol.toLowerCase();
        try {
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String priceUsd = (String) data.get("priceUsd");
                return new BigDecimal(priceUsd).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch price for asset: " + symbol, e);
        }
        throw new RuntimeException("Price data not found for asset: " + symbol);
    }
}
