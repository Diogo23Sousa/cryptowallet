package com.project.cryptowallet.service;

import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WalletService defines the business logic for managing the crypto wallet.
 * It includes operations for saving assets, updating prices, calculating wallet summary,
 * and setting the price update frequency.
 */
public interface WalletService {

    /**
     * Save a list of crypto assets to the wallet database.
     *
     * @param assets List of WalletAsset objects containing symbol, quantity, and price.
     */
    void saveAssets(List<WalletAsset> assets);

    /**
     * Update the latest prices of all wallet assets concurrently.
     * The prices are fetched using a thread pool limited to 3 threads.
     */
    void updatePricesConcurrently();

    /**
     * Retrieve the wallet summary.
     * - If 'timestamp' is provided, fetch the historical summary at the given time.
     * - If 'timestamp' is null, fetch the current wallet summary.
     *
     * @param timestamp Optional timestamp for the historical summary.
     * @return WalletSummaryResponse containing wallet details.
     */
    WalletSummaryResponse getWalletSummary(LocalDateTime timestamp);
    /**
     * Set the frequency (in seconds) for updating wallet prices.
     *
     * @param frequencyInSeconds The new frequency in seconds.
     */
    void setUpdateFrequency(long frequencyInSeconds);
}
