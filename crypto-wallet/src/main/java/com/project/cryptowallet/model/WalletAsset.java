package com.project.cryptowallet.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class WalletAsset {

    public WalletAsset() {
    }

    public WalletAsset(BigDecimal price, Long id, String symbol, BigDecimal quantity, BigDecimal latestPrice) {
        this.price = price;
        this.id = id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.latestPrice = latestPrice;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal latestPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }
}
