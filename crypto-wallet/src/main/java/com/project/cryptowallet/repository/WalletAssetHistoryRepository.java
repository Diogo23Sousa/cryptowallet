package com.project.cryptowallet.repository;

import com.project.cryptowallet.model.WalletAssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface WalletAssetHistoryRepository extends JpaRepository<WalletAssetHistory, Long> {

    /**
     * Find the most recent WalletAssetHistory entry before or at the given timestamp.
     *
     * @param walletAssetId The ID of the WalletAsset.
     * @param timestamp     The target timestamp.
     * @return A list of WalletAssetHistory entries ordered by updatedAt descending.
     */
    List<WalletAssetHistory> findByWalletAssetIdAndUpdatedAtBeforeOrderByUpdatedAtDesc(Long walletAssetId, LocalDateTime timestamp);
}


