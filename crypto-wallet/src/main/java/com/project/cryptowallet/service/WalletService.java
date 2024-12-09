package com.project.cryptowallet.service;

import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;

import java.util.List;

/**
 * WalletService defines the business logic for managing the crypto wallet.
 * It includes operations for saving assets, updating prices, calculating wallet summary,
 * and retrieving historical wallet data.
 */
public interface WalletService {

    /**
     * Save a list of crypto assets to the wallet database.
     *
     * @param assets List of WalletAsset objects containing symbol, quantity, and price.
     */
    void saveAssets(List<WalletAsset> assets);

    /**
     * Update the latest prices of all wallet assets.
     * The prices are fetched concurrently using a thread pool limited to 3 threads.
     */
    void updatePricesConcurrently();

    /**
     * Calculate and retrieve the wallet summary.
     * The summary includes:
     * - Total value of all assets.
     * - Best performing asset with its performance percentage.
     * - Worst performing asset with its performance percentage.
     *
     * @return WalletSummaryResponse containing wallet summary details.
     */
    WalletSummaryResponse getWalletSummary();

    /**
     * Retrieve all wallet assets, including their current and historical state.
     *
     * @return List of WalletAsset objects.
     */
    List<WalletAsset> getWalletHistory();

    /**
     * Set the frequency (in seconds) for updating wallet prices.
     *
     * @param frequencyInSeconds The new frequency in seconds.
     */
    void setUpdateFrequency(long frequencyInSeconds);
}
