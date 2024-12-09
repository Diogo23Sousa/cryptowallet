package com.project.cryptowallet.controller;

import com.project.cryptowallet.dto.WalletSummaryResponse;
import com.project.cryptowallet.model.WalletAsset;
import com.project.cryptowallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletControllerImpl implements WalletController {

    private final WalletService walletService;

    public WalletControllerImpl(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/add")
    @Override
    public ResponseEntity<String> addAssets(@RequestBody List<WalletAsset> assets) {
        walletService.saveAssets(assets);
        return ResponseEntity.ok("Assets added successfully");
    }

    @GetMapping("/update")
    @Override
    public ResponseEntity<String> updatePrices() {
        walletService.updatePricesConcurrently();
        return ResponseEntity.ok("Prices updated successfully");
    }

    @GetMapping("/summary")
    @Override
    public ResponseEntity<WalletSummaryResponse> getWalletSummary() {
        return ResponseEntity.ok(walletService.getWalletSummary());
    }

    @GetMapping("/history")
    @Override
    public ResponseEntity<List<WalletAsset>> getWalletHistory() {
        return ResponseEntity.ok(walletService.getWalletHistory());
    }

    @Override
    @PostMapping("/frequency")
    public ResponseEntity<String> setUpdateFrequency(@RequestParam long frequencyInSeconds) {
        walletService.setUpdateFrequency(frequencyInSeconds);
        return ResponseEntity.ok("Update frequency set to " + frequencyInSeconds + " seconds");
    }
}
