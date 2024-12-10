package com.project.cryptowallet.controller;

import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.OK;

class WalletControllerImplTest {

    private final WalletService walletService = Mockito.mock(WalletService.class);
    private final WalletControllerImpl walletController = new WalletControllerImpl(walletService);

    @Test
    void testAddAssets() {
        List<WalletAsset> assets = Arrays.asList(
                new WalletAsset(new BigDecimal("40000"), null, "BTC", new BigDecimal("0.5"), null)
        );

        ResponseEntity<String> response = walletController.addAssets(assets);
        assertEquals(OK, response.getStatusCode());
        assertEquals("Assets added successfully", response.getBody());
        verify(walletService, times(1)).saveAssets(assets);
    }

    @Test
    void testUpdatePrices() {
        ResponseEntity<String> response = walletController.updatePrices();
        assertEquals(OK, response.getStatusCode());
        assertEquals("Prices updated successfully", response.getBody());
        verify(walletService, times(1)).updatePricesConcurrently();
    }

    @Test
    void testGetWalletSummary_Current() {
        WalletSummaryResponse mockSummary = new WalletSummaryResponse(
                new BigDecimal("20000"), "BTC", new BigDecimal("5.0"), "ETH", new BigDecimal("1.2")
        );
        when(walletService.getWalletSummary(null)).thenReturn(mockSummary);

        ResponseEntity<WalletSummaryResponse> response = walletController.getWalletSummary(null);
        assertEquals(OK, response.getStatusCode());
        assertEquals(mockSummary, response.getBody());
        verify(walletService, times(1)).getWalletSummary(null);
    }

    @Test
    void testGetWalletSummary_Historical() {
        LocalDateTime timestamp = LocalDateTime.now();
        WalletSummaryResponse mockSummary = new WalletSummaryResponse(
                new BigDecimal("18000"), "ETH", new BigDecimal("4.5"), "DOGE", new BigDecimal("0.8")
        );
        when(walletService.getWalletSummary(timestamp)).thenReturn(mockSummary);

        ResponseEntity<WalletSummaryResponse> response = walletController.getWalletSummary(timestamp);
        assertEquals(OK, response.getStatusCode());
        assertEquals(mockSummary, response.getBody());
        verify(walletService, times(1)).getWalletSummary(timestamp);
    }

    @Test
    void testSetUpdateFrequency() {
        long frequency = 10L;
        ResponseEntity<String> response = walletController.setUpdateFrequency(frequency);
        assertEquals(OK, response.getStatusCode());
        assertEquals("Update frequency set to 10 seconds", response.getBody());
        verify(walletService, times(1)).setUpdateFrequency(frequency);
    }
}
