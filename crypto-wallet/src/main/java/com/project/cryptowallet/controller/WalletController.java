package com.project.cryptowallet.controller;

import com.project.cryptowallet.model.WalletAsset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * WalletController provides REST API endpoints for managing the crypto wallet.
 * It includes operations to add assets, update prices, retrieve the wallet summary,
 * and fetch historical data of the wallet.
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
     * Prices are fetched concurrently (with a limit of 3 concurrent threads) from the CoinCap API.
     *
     * @return ResponseEntity with a confirmation message.
     */
    @GetMapping("/update")
    ResponseEntity<String> updatePrices();

    /**
     * Retrieve a summary of the wallet including:
     * - Total value of all assets.
     * - The best performing asset and its performance percentage.
     * - The worst performing asset and its performance percentage.
     *
     * @return ResponseEntity containing the wallet summary as a JSON-formatted string.
     */
    @GetMapping("/summary")
    ResponseEntity<String> getWalletSummary();

    /**
     * Retrieve all wallet assets from the database, including their current and historical state.
     *
     * @return ResponseEntity containing a list of WalletAsset objects.
     */
    @GetMapping("/history")
    ResponseEntity<List<WalletAsset>> getWalletHistory();
}
