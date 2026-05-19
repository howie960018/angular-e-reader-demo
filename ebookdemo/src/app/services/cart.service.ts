import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Cart, CartItem, Book } from '../models';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly baseUrl = 'http://localhost:8080/api/cart';

  /** Local cache so navbar/checkout can subscribe without extra API calls */
  private cart$ = new BehaviorSubject<Cart | null>(null);

  constructor(private http: HttpClient) {
    if (localStorage.getItem('auth_token')) {
      this.loadCart().subscribe();
    }
  }

  /** Subscribe to this for reactive cart updates (navbar uses this) */
  getCart(): Observable<Cart | null> {
    return this.cart$.asObservable();
  }

  /** Fetch cart from API and update cache */
  loadCart(): Observable<Cart | null> {
    return this.http.get<any>(this.baseUrl).pipe(
      map(raw => this.normalizeCart(raw)),
      tap(cart => this.cart$.next(cart))
    );
  }

  addToCart(book: Book, quantity: number = 1): Observable<Cart | null> {
    return this.http.post<any>(`${this.baseUrl}/items`, { bookId: Number(book.id), quantity }).pipe(
      map(raw => this.normalizeCart(raw)),
      tap(cart => this.cart$.next(cart))
    );
  }

  removeFromCart(bookId: string): Observable<Cart | null> {
    return this.http.delete<void>(`${this.baseUrl}/items/${bookId}`).pipe(
      tap(() => {
        const cart = this.cart$.value;
        if (cart) {
          cart.items = cart.items.filter(i => i.bookId !== bookId);
          cart.updatedAt = new Date();
          this.cart$.next({ ...cart });
        }
      }),
      map(() => this.cart$.value)
    );
  }

  updateQuantity(bookId: string, quantity: number): Observable<Cart | null> {
    return this.http.put<any>(`${this.baseUrl}/items/${bookId}`, { quantity }).pipe(
      map(raw => this.normalizeCart(raw)),
      tap(cart => this.cart$.next(cart))
    );
  }

  clearCart(): Observable<Cart | null> {
    return this.http.delete<void>(this.baseUrl).pipe(
      tap(() => {
        const cart = this.cart$.value;
        if (cart) {
          cart.items = [];
          cart.updatedAt = new Date();
          this.cart$.next({ ...cart });
        }
      }),
      map(() => this.cart$.value)
    );
  }

  /** Called on logout so navbar count resets immediately */
  resetCart(): void {
    this.cart$.next(null);
  }

  /** Sync helper used by checkout to show total before placing order */
  getTotalPrice(): number {
    const cart = this.cart$.value;
    if (!cart) return 0;
    return cart.items.reduce((sum, item) => sum + (Number(item.book.price) * item.quantity), 0);
  }

  private normalizeCart(raw: any): Cart {
    const items: CartItem[] = (raw.items ?? []).map((item: any) => ({
      bookId: String(item.bookId),
      quantity: item.quantity,
      book: this.normalizeBook(item.book)
    }));

    return {
      id: String(raw.id),
      userId: String(raw.userId),
      items,
      createdAt: new Date(raw.createdAt),
      updatedAt: new Date(raw.updatedAt)
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
