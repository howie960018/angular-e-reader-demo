import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Order, OrderItem, OrderStatus, Book } from '../models';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly baseUrl = 'http://localhost:8080/api/orders';

  constructor(private http: HttpClient) {}

  /** Current user's orders */
  getOrders(): Observable<Order[]> {
    return this.http.get<any>(this.baseUrl).pipe(
      map(page => (page.content ?? page).map((o: any) => this.normalizeOrder(o)))
    );
  }

  /** For backward compat — same as getOrders() (server knows user from JWT) */
  getUserOrders(_userId?: string): Observable<Order[]> {
    return this.getOrders();
  }

  /** All orders — admin only */
  getAllOrders(): Observable<Order[]> {
    return this.http.get<any>(`${this.baseUrl}/all`).pipe(
      map(page => (page.content ?? page).map((o: any) => this.normalizeOrder(o)))
    );
  }

  getOrderById(id: string): Observable<Order | undefined> {
    return this.http.get<any>(`${this.baseUrl}/${id}`).pipe(
      map(o => this.normalizeOrder(o))
    );
  }

  /** 確認下單：建立 PENDING 訂單，不扣款 */
  createOrder(): Observable<Order> {
    return this.http.post<any>(this.baseUrl, {}).pipe(
      map(o => this.normalizeOrder(o))
    );
  }

  /** 確認付款：扣款並完成訂單 */
  confirmPayment(orderId: string): Observable<Order> {
    return this.http.post<any>(`${this.baseUrl}/${orderId}/pay`, {}).pipe(
      map(o => this.normalizeOrder(o))
    );
  }

  /** 使用者主動取消待付款訂單 */
  cancelOrder(orderId: string): Observable<Order> {
    return this.http.post<any>(`${this.baseUrl}/${orderId}/cancel`, {}).pipe(
      map(o => this.normalizeOrder(o))
    );
  }

  updateOrderStatus(orderId: string, status: OrderStatus): Observable<Order> {
    return this.http.put<any>(`${this.baseUrl}/${orderId}/status`, { status }).pipe(
      map(o => this.normalizeOrder(o))
    );
  }

  deleteOrder(orderId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${orderId}`);
  }

  private normalizeOrder(raw: any): Order {
    const items: OrderItem[] = (raw.items ?? []).map((item: any) => ({
      id: String(item.id),
      orderId: String(item.orderId),
      bookId: String(item.bookId),
      book: this.normalizeBook(item.book),
      price: item.price,
      quantity: item.quantity
    }));

    return {
      id: String(raw.id),
      userId: String(raw.userId),
      username: raw.username ?? undefined,
      items,
      totalPrice: raw.totalPrice,
      status: raw.status as OrderStatus,
      createdAt: new Date(raw.createdAt),
      updatedAt: new Date(raw.updatedAt),
      expiresAt: raw.expiresAt ? new Date(raw.expiresAt) : undefined
    };
  }

  private normalizeBook(raw: any): Book {
    return {
      id: String(raw.id),
      title: raw.title,
      author: raw.author,
      description: raw.description ?? '',
      price: raw.price,
      categoryId: String(raw.categoryId ?? ''),
      sellerId: String(raw.sellerId),
      sellerName: raw.sellerName,
      coverImage: raw.coverImage ?? '',
      content: raw.content,
      status: raw.status ?? 'active',
      createdAt: new Date(raw.createdAt),
      updatedAt: new Date(raw.updatedAt)
    };
  }
}
