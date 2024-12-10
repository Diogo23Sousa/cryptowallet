package com.project.cryptowallet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class WalletAssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private BigDecimal price;

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "wallet_asset_id")
    private WalletAsset walletAsset;

    public WalletAssetHistory() {}

    public WalletAssetHistory(String symbol, BigDecimal price, LocalDateTime updatedAt, WalletAsset walletAsset) {
        this.symbol = symbol;
        this.price = price;
        this.updatedAt = updatedAt;
        this.walletAsset = walletAsset;
    }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public WalletAsset getWalletAsset() { return walletAsset; }
}

