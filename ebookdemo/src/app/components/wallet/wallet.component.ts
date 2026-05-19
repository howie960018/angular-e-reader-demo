import { Component, OnInit } from '@angular/core';
import { WalletService } from '../../services/wallet.service';
import { AuthService } from '../../services/auth.service';
import { Wallet, WalletTransaction, User } from '../../models';

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.component.html',
  styleUrls: ['./wallet.component.css']
})
export class WalletComponent implements OnInit {
  wallet: Wallet | null = null;
  transactions: WalletTransaction[] = [];
  currentUser: User | null = null;
  availableCodes: string[] = [];

  // 兌換碼儲值
  topUpCode = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  // 直接儲值
  directAmount: number | null = null;
  directLoading = false;
  directError = '';
  directSuccess = '';

  constructor(
    private walletService: WalletService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.loadWallet();
        this.loadTransactions();
        this.loadAvailableCodes();
      }
    });
  }

  loadWallet(): void {
    this.walletService.getUserWallet().subscribe(wallet => { this.wallet = wallet; });
  }

  loadTransactions(): void {
    this.walletService.getTransactions().subscribe(transactions => { this.transactions = transactions; });
  }

  loadAvailableCodes(): void {
    this.walletService.getAvailableCodes().subscribe({
      next: codes => { this.availableCodes = codes; },
      error: () => { this.availableCodes = []; }
    });
  }

  // ── 兌換碼儲值 ──────────────────────────────────────────────
  useTopUpCode(): void {
    if (!this.topUpCode) { this.errorMessage = '請輸入兌換碼'; return; }
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.walletService.useTopUpCode(undefined, this.topUpCode).subscribe({
      next: result => {
        this.isLoading = false;
        if (result.success) {
          this.successMessage = `兌換成功，已入帳 ${result.amount} 點！`;
          this.topUpCode = '';
          this.refresh();
        } else {
          this.errorMessage = result.message;
        }
      },
      error: () => { this.isLoading = false; this.errorMessage = '兌換失敗，請稍後再試'; }
    });
  }

  getAvailableCodes(): string[] { return this.availableCodes; }

  setTopUpCode(code: string): void { this.topUpCode = code; }

  // ── 直接儲值 ────────────────────────────────────────────────
  topupDirect(): void {
    if (!this.directAmount || this.directAmount <= 0) {
      this.directError = '請輸入有效的儲值點數';
      return;
    }
    this.directLoading = true;
    this.directError = '';
    this.directSuccess = '';
    this.walletService.topupDirect(this.directAmount).subscribe({
      next: wallet => {
        this.directLoading = false;
        this.directSuccess = `儲值成功！目前餘額：${wallet.balance} 點`;
        this.directAmount = null;
        this.refresh();
      },
      error: err => {
        this.directLoading = false;
        this.directError = err?.error?.message || '儲值失敗，請稍後再試';
      }
    });
  }

  // ── 共用 ────────────────────────────────────────────────────
  getTypeLabel(type: string): string {
    const map: Record<string, string> = {
      TOPUP: '儲值', PURCHASE: '購書', DEPOSIT: '管理員入款',
      PAYMENT: '消費', REFUND: '退款', COMMISSION: '抽成', PAYOUT: '提款'
    };
    return map[type] ?? type;
  }

  private refresh(): void {
    this.loadWallet();
    this.loadTransactions();
    this.loadAvailableCodes();
  }
}
