package com.project.cryptowallet.repository;

import com.project.cryptowallet.model.WalletAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletAssetRepository extends JpaRepository<WalletAsset, Long> {
}
