import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BookService } from '../../services/book.service';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { Book, User } from '../../models';

@Component({
  selector: 'app-book-detail',
  templateUrl: './book-detail.component.html',
  styleUrls: ['./book-detail.component.css']
})
export class BookDetailComponent implements OnInit {
  book: Book | null = null;
  currentUser: User | null = null;
  isLoading = true;
  isPurchased = false;

  constructor(
    private route: ActivatedRoute,
    private bookService: BookService,
    private cartService: CartService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
    });
    this.route.params.subscribe(params => {
      const bookId = params['id'];
      this.loadBook(bookId);
      if (this.authService.isAuthenticated()) {
        this.bookService.checkPurchased(bookId).subscribe({
          next: res => { this.isPurchased = res.purchased; },
          error: () => { this.isPurchased = false; }
        });
      }
    });
  }

  loadBook(bookId: string): void {
    this.bookService.getBookById(bookId).subscribe(book => {
      this.book = book || null;
      this.isLoading = false;
    });
  }

  get canPurchase(): boolean {
    return !!this.currentUser && !this.isPurchased && this.book?.status === 'active';
  }

  get canRead(): boolean {
    if (!this.currentUser) return false;
    if (this.currentUser.role === 'ADMIN') return true;
    if (this.currentUser.id === this.book?.sellerId) return true;
    return this.isPurchased && this.book?.status !== 'banned';
  }

  get statusLabel(): string {
    const labels: Record<string, string> = {
      draft: '尚未上架',
      active: '正常販售',
      discontinued: '已停售',
      banned: '已依法下架'
    };
    return labels[this.book?.status ?? ''] ?? '';
  }

  get statusClass(): string {
    return `status-${this.book?.status ?? ''}`;
  }

  addToCart(): void {
    if (!this.currentUser) { this.router.navigate(['/login']); return; }
    if (!this.book) return;
    this.cartService.addToCart(this.book, 1).subscribe({
      next: () => { this.router.navigate(['/cart']); },
      error: err => {
        const msg = err?.error?.message || '加入購物車失敗';
        alert(msg);
        // 若後端回報已購買，立即更新狀態
        if (msg.includes('已購買')) {
          this.isPurchased = true;
        }
      }
    });
  }

  buyNow(): void {
    this.addToCart();
  }

  readBook(): void {
    if (!this.currentUser) { this.router.navigate(['/login']); return; }
    if (this.book) this.router.navigate(['/read', this.book.id]);
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
