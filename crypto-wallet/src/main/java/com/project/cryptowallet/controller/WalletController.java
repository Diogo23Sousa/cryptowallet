package com.project.cryptowallet.controller;

import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WalletController provides REST API endpoints for managing the crypto wallet.
 * It includes operations to add assets, update prices, retrieve wallet summaries (current or historical),
 * and set the price update frequency.
 */
public interface WalletController {

    /**
     * Add a list of crypto assets to the wallet.
     *
     * @param assets List of WalletAsset objects containing symbol, quantity, and price.
     * @return ResponseEntity with a confirmation message.
     */
    @PostMapping("/add")
    ResponseEntity<String> addAssets(@RequestBody List<WalletAsset> assets);

    /**
     * Update the latest prices of all assets in the wallet.
     *
     * @return ResponseEntity with a confirmation message.
     */
    @GetMapping("/update")
    ResponseEntity<String> updatePrices();

    /**
     * Retrieve a wallet summary.
     * If a timestamp is provided, fetch the historical summary for that time.
     * Otherwise, fetch the current wallet summary.
     *
     * @param timestamp Optional timestamp to fetch historical summary.
     * @return ResponseEntity containing the wallet summary.
     */
    @GetMapping("/summary")
    ResponseEntity<WalletSummaryResponse> getWalletSummary(
            @RequestParam(value = "timestamp", required = false) LocalDateTime timestamp
    );

    /**
     * Set the frequency (in seconds) for updating wallet prices.
     *
     * @param frequencyInSeconds The new frequency in seconds.
     * @return ResponseEntity with a confirmation message.
     */
    @PostMapping("/frequency")
    ResponseEntity<String> setUpdateFrequency(@RequestParam long frequencyInSeconds);
}
