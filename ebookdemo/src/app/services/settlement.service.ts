import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RevenueShare, SettlementSummary } from '../models';

@Injectable({
  providedIn: 'root'
})
export class SettlementService {
  private readonly baseUrl = 'http://localhost:8080/api/settlement';

  constructor(private http: HttpClient) {}

  /** Admin：取得待結算清單 */
  getPending(): Observable<RevenueShare[]> {
    return this.http.get<any[]>(`${this.baseUrl}/pending`).pipe(
      map(list => list.map(r => this.normalize(r)))
    );
  }

  /** Admin：取得結算歷史 */
  getHistory(): Observable<RevenueShare[]> {
    return this.http.get<any[]>(`${this.baseUrl}/history`).pipe(
      map(list => list.map(r => this.normalize(r)))
    );
  }

  /** Admin：執行結算 */
  executeSettlement(): Observable<SettlementSummary> {
    return this.http.post<any>(`${this.baseUrl}/execute`, {}).pipe(
      map(raw => ({
        count: raw.count,
        totalPlatformPoints: Number(raw.totalPlatformPoints),
        totalPublisherPoints: Number(raw.totalPublisherPoints),
        message: raw.message
      }))
    );
  }

  /** Seller：取得自己的分潤明細 */
  getMyRevenue(): Observable<RevenueShare[]> {
    return this.http.get<any[]>(`${this.baseUrl}/my-revenue`).pipe(
      map(list => list.map(r => this.normalize(r)))
    );
  }

  private normalize(raw: any): RevenueShare {
    return {
      id: String(raw.id),
      orderId: String(raw.orderId),
      bookId: String(raw.bookId),
      bookTitle: raw.bookTitle,
      publisherId: String(raw.publisherId),
      publisherName: raw.publisherName,
      totalPoints: Number(raw.totalPoints),
      platformSharePoints: Number(raw.platformSharePoints),
      publisherSharePoints: Number(raw.publisherSharePoints),
      settled: raw.settled,
      createdAt: new Date(raw.createdAt),
      settledAt: raw.settledAt ? new Date(raw.settledAt) : undefined
    };
  }
}
