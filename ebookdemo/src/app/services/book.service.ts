import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Book, Category } from '../models';

@Injectable({
  providedIn: 'root'
})
export class BookService {
  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getBooks(): Observable<Book[]> {
    return this.http.get<any[]>(`${this.baseUrl}/books`).pipe(
      map(list => list.map(b => this.normalizeBook(b)))
    );
  }

  getBookById(id: string): Observable<Book | undefined> {
    return this.http.get<any>(`${this.baseUrl}/books/${id}`).pipe(
      map(b => this.normalizeBook(b))
    );
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<any[]>(`${this.baseUrl}/categories`).pipe(
      map(list => list.map(c => this.normalizeCategory(c)))
    );
  }

  getBooksByCategory(categoryId: string): Observable<Book[]> {
    const params = new HttpParams().set('categoryId', categoryId);
    return this.http.get<any[]>(`${this.baseUrl}/books`, { params }).pipe(
      map(list => list.map(b => this.normalizeBook(b)))
    );
  }

  getBooksBySeller(sellerId: string): Observable<Book[]> {
    const params = new HttpParams().set('sellerId', sellerId);
    return this.http.get<any[]>(`${this.baseUrl}/books`, { params }).pipe(
      map(list => list.map(b => this.normalizeBook(b)))
    );
  }

  addBook(book: Omit<Book, 'id' | 'createdAt' | 'updatedAt'>): Observable<Book> {
    const payload = {
      title: book.title,
      author: book.author,
      description: book.description,
      price: book.price,
      categoryId: book.categoryId ? Number(book.categoryId) : null,
      coverImage: book.coverImage,
      content: book.content
    };
    return this.http.post<any>(`${this.baseUrl}/books`, payload).pipe(
      map(b => this.normalizeBook(b))
    );
  }

  updateBook(id: string, updates: Partial<Book>): Observable<Book> {
    const payload = {
      title: updates.title,
      author: updates.author,
      description: updates.description,
      price: updates.price,
      categoryId: updates.categoryId ? Number(updates.categoryId) : null,
      coverImage: updates.coverImage,
      content: updates.content,
      status: (updates as any).status
    };
    return this.http.put<any>(`${this.baseUrl}/books/${id}`, payload).pipe(
      map(b => this.normalizeBook(b))
    );
  }

  deleteBook(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/books/${id}`);
  }

  addCategory(name: string, description: string): Observable<Category> {
    return this.http.post<any>(`${this.baseUrl}/categories`, { name, description }).pipe(
      map(c => this.normalizeCategory(c))
    );
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categories/${id}`);
  }

  /** 出版商：取得自己所有書籍（含 draft/discontinued） */
  getMyBooks(): Observable<Book[]> {
    return this.http.get<any[]>(`${this.baseUrl}/books/my`).pipe(
      map(list => list.map(b => this.normalizeBook(b)))
    );
  }

  /** Admin：取得所有書籍（含所有狀態） */
  getAllBooksForAdmin(): Observable<Book[]> {
    return this.http.get<any[]>(`${this.baseUrl}/books/all`).pipe(
      map(list => list.map(b => this.normalizeBook(b)))
    );
  }

  /** 確認當前用戶是否已購買此書 */
  checkPurchased(bookId: string): Observable<{ purchased: boolean }> {
    return this.http.get<{ purchased: boolean }>(`${this.baseUrl}/books/${bookId}/purchased`);
  }

  /** 取得當前用戶已購買的書籍（需登入） */
  getPurchasedBooks(): Observable<Book[]> {
    return this.http.get<any[]>(`${this.baseUrl}/books/purchased`).pipe(
      map(list => list.map(b => this.normalizeBook(b)))
    );
  }

  /** 取得書籍內容（試閱或完整） */
  getBookContent(bookId: string): Observable<{ content: string; hasAccess: boolean; bookTitle: string; previewLength: number; totalLength: number }> {
    return this.http.get<any>(`${this.baseUrl}/books/${bookId}/content`);
  }

  /** Normalize backend response (numeric ids) to frontend model (string ids) */
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

  private normalizeCategory(raw: any): Category {
    return {
      id: String(raw.id),
      name: raw.name,
      description: raw.description ?? ''
    };
  }
}
