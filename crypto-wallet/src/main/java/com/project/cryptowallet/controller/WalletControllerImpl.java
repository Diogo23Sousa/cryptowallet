package com.project.cryptowallet.controller;

import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
public class WalletControllerImpl {

    private static final Logger logger = LoggerFactory.getLogger(WalletControllerImpl.class);
    private final WalletService walletService;

    public WalletControllerImpl(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/add")
    public ResponseEntity<String> addAssets(@RequestBody List<WalletAsset> assets) {
        String assetDetails = assets.stream()
                .map(a -> String.format("Symbol: %s, Quantity: %.2f, Price: %.2f",
                        a.getSymbol(), a.getQuantity(), a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO))
                .collect(Collectors.joining("; "));
        logger.info("Received request to add assets: [{}]", assetDetails);

        walletService.saveAssets(assets);
        logger.info("Assets added successfully.");

        return ResponseEntity.ok("Assets added successfully");
    }


    @GetMapping("/update")
    public ResponseEntity<String> updatePrices() {
        logger.info("Received request to update prices.");
        walletService.updatePricesConcurrently();
        logger.info("Prices updated successfully.");
        return ResponseEntity.ok("Prices updated successfully");
    }

    @GetMapping("/summary")
    public ResponseEntity<WalletSummaryResponse> getWalletSummary(
            @RequestParam(value = "timestamp", required = false) LocalDateTime timestamp) {
        logger.info("Received request to fetch wallet summary for timestamp: {}", timestamp);
        WalletSummaryResponse summary = walletService.getWalletSummary(timestamp);
        logger.info("Wallet summary generated successfully: {}", summary);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/frequency")
    public ResponseEntity<String> setUpdateFrequency(@RequestParam long frequencyInSeconds) {
        logger.info("Received request to set update frequency to {} seconds", frequencyInSeconds);
        walletService.setUpdateFrequency(frequencyInSeconds);
        logger.info("Update frequency set to {} seconds", frequencyInSeconds);
        return ResponseEntity.ok("Update frequency set to " + frequencyInSeconds + " seconds");
    }
}
