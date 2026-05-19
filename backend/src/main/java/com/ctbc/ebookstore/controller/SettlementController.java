package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.dto.RevenueShareDto;
import com.ctbc.ebookstore.dto.SettlementSummary;
import com.ctbc.ebookstore.service.AppUserService;
import com.ctbc.ebookstore.service.SettlementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementService settlementService;
    private final AppUserService userService;

    public SettlementController(SettlementService settlementService, AppUserService userService) {
        this.settlementService = settlementService;
        this.userService = userService;
    }

    /** Admin：取得所有待結算清單 */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RevenueShareDto> getPending() {
        return settlementService.getPending();
    }

    /** Admin：取得結算歷史 */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RevenueShareDto> getHistory() {
        return settlementService.getHistory();
    }

    /** Admin：執行結算（一鍵結算所有待結算項目） */
    @PostMapping("/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public SettlementSummary execute() {
        return settlementService.settle();
    }

    /** Seller：查看自己的分潤紀錄 */
    @GetMapping("/my-revenue")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public List<RevenueShareDto> getMyRevenue(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return settlementService.getPublisherRevenue(user);
    }
}
