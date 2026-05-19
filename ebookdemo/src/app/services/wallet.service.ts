import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Wallet, WalletTransaction, TransactionType,
  PlatformWalletInfo, PlatformTransaction,
  PublisherWalletInfo, PublisherTransaction
} from '../models';

export interface TopUpCodeInfo {
  id: string;
  code: string;
  amount: number;
  usageCount: number;
  createdAt: Date;
}

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private readonly baseUrl = 'http://localhost:8080/api/wallet';

  constructor(private http: HttpClient) {}

  getUserWallet(_userId?: string): Observable<Wallet> {
    return this.http.get<any>(this.baseUrl).pipe(
      map(raw => this.normalizeWallet(raw))
    );
  }

  getWalletBalance(_userId?: string): Observable<number> {
    return this.http.get<any>(`${this.baseUrl}/balance`).pipe(
      map(res => Number(res.balance))
    );
  }

  getTransactions(_walletId?: string): Observable<WalletTransaction[]> {
    return this.http.get<any[]>(`${this.baseUrl}/transactions`).pipe(
      map(list => list.map(t => this.normalizeTransaction(t)))
    );
  }

  useTopUpCode(_userId: string | undefined, code: string): Observable<{ success: boolean; message: string; amount?: number }> {
    return this.http.post<any>(`${this.baseUrl}/topup`, { code }).pipe(
      map(res => ({
        success: res.success,
        message: res.message,
        amount: res.amount != null ? Number(res.amount) : undefined
      }))
    );
  }

  /** 取得當前使用者尚未兌換過的代碼（供錢包頁面顯示提示） */
  getAvailableCodes(): Observable<string[]> {
    return this.http.get<any[]>(`${this.baseUrl}/topup-codes/available`).pipe(
      map(list => list.map((c: any) => c.code))
    );
  }

  /** 取得所有儲值碼及使用次數（Admin only） */
  getAllTopUpCodes(): Observable<TopUpCodeInfo[]> {
    return this.http.get<any[]>(`${this.baseUrl}/topup-codes`).pipe(
      map(list => list.map(c => ({
        id: String(c.id),
        code: c.code,
        amount: Number(c.amount),
        usageCount: c.usageCount ?? 0,
        createdAt: new Date(c.createdAt)
      })))
    );
  }

  /** 建立新儲值碼（Admin only） */
  createTopUpCode(code: string, amount: number): Observable<TopUpCodeInfo> {
    return this.http.post<any>(`${this.baseUrl}/topup-codes`, { code, amount }).pipe(
      map(c => ({
        id: String(c.id),
        code: c.code,
        amount: Number(c.amount),
        usageCount: 0,
        createdAt: new Date(c.createdAt)
      }))
    );
  }

  /** 刪除儲值碼（Admin only） */
  deleteTopUpCode(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/topup-codes/${id}`);
  }

  deposit(userId: string, amount: number, description: string = '入款'): Observable<Wallet> {
    return this.http.post<any>(`${this.baseUrl}/deposit`, { userId: Number(userId), amount, description }).pipe(
      map(raw => this.normalizeWallet(raw))
    );
  }

  /** 直接儲值（模擬金流） */
  topupDirect(amount: number): Observable<Wallet> {
    return this.http.post<any>(`${this.baseUrl}/topup-direct`, { amount }).pipe(
      map(raw => this.normalizeWallet(raw))
    );
  }

  /** Admin：取得平台錢包（含交易明細） */
  getPlatformWallet(): Observable<PlatformWalletInfo> {
    return this.http.get<any>(`${this.baseUrl}/platform`).pipe(
      map(raw => ({
        id: String(raw.id),
        balancePoints: Number(raw.balancePoints),
        transactions: (raw.transactions || []).map((t: any) => this.normalizePlatformTx(t))
      }))
    );
  }

  /** Seller：取得自己的出版商分潤錢包（含交易明細） */
  getMyPublisherWallet(): Observable<PublisherWalletInfo> {
    return this.http.get<any>(`${this.baseUrl}/publisher/me`).pipe(
      map(raw => this.normalizePublisherWallet(raw))
    );
  }

  /** Admin：取得所有出版商錢包摘要 */
  getAllPublisherWallets(): Observable<PublisherWalletInfo[]> {
    return this.http.get<any[]>(`${this.baseUrl}/publishers`).pipe(
      map(list => list.map(raw => ({
        publisherId: String(raw.publisherId),
        publisherName: raw.publisherName,
        balancePoints: Number(raw.balancePoints)
      })))
    );
  }

  /** Admin：取得指定出版商錢包（含交易明細） */
  getPublisherWalletById(publisherId: string): Observable<PublisherWalletInfo> {
    return this.http.get<any>(`${this.baseUrl}/publishers/${publisherId}`).pipe(
      map(raw => this.normalizePublisherWallet(raw))
    );
  }

  private normalizePublisherWallet(raw: any): PublisherWalletInfo {
    return {
      publisherId: String(raw.publisherId),
      publisherName: raw.publisherName,
      balancePoints: Number(raw.balancePoints),
      transactions: (raw.transactions || []).map((t: any) => this.normalizePublisherTx(t))
    };
  }

  private normalizePlatformTx(t: any): PlatformTransaction {
    return {
      id: String(t.id),
      type: t.type,
      amountPoints: Number(t.amountPoints),
      orderId: t.orderId != null ? String(t.orderId) : undefined,
      createdAt: new Date(t.createdAt)
    };
  }

  private normalizePublisherTx(t: any): PublisherTransaction {
    return {
      id: String(t.id),
      type: t.type,
      amountPoints: Number(t.amountPoints),
      orderId: t.orderId != null ? String(t.orderId) : undefined,
      createdAt: new Date(t.createdAt)
    };
  }

  private normalizeWallet(raw: any): Wallet {
    return {
      id: String(raw.id),
      userId: String(raw.userId),
      type: raw.type,
      balance: Number(raw.balance),
      createdAt: new Date(raw.createdAt)
    };
  }

  private normalizeTransaction(raw: any): WalletTransaction {
    return {
      id: String(raw.id),
      walletId: String(raw.walletId),
      type: raw.type as TransactionType,
      amount: Number(raw.amount),
      description: raw.description ?? '',
      createdAt: new Date(raw.createdAt)
    };
  }
}
