package com.project.cryptowallet.dto;

import java.math.BigDecimal;

public class WalletSummaryResponse {

    private BigDecimal totalValue;
    private String bestAsset;
    private BigDecimal bestPerformance;
    private String worstAsset;
    private BigDecimal worstPerformance;

    public WalletSummaryResponse() {}

    public WalletSummaryResponse(BigDecimal totalValue, String bestAsset, BigDecimal bestPerformance, String worstAsset, BigDecimal worstPerformance) {
        this.totalValue = totalValue;
        this.bestAsset = bestAsset;
        this.bestPerformance = bestPerformance;
        this.worstAsset = worstAsset;
        this.worstPerformance = worstPerformance;
    }

    // Getters and Setters
    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getBestAsset() {
        return bestAsset;
    }

    public void setBestAsset(String bestAsset) {
        this.bestAsset = bestAsset;
    }

    public BigDecimal getBestPerformance() {
        return bestPerformance;
    }

    public void setBestPerformance(BigDecimal bestPerformance) {
        this.bestPerformance = bestPerformance;
    }

    public String getWorstAsset() {
        return worstAsset;
    }

    public void setWorstAsset(String worstAsset) {
        this.worstAsset = worstAsset;
    }

    public BigDecimal getWorstPerformance() {
        return worstPerformance;
    }

    public void setWorstPerformance(BigDecimal worstPerformance) {
        this.worstPerformance = worstPerformance;
    }
}
