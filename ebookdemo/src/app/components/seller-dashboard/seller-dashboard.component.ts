import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BookService } from '../../services/book.service';
import { AuthService } from '../../services/auth.service';
import { WalletService } from '../../services/wallet.service';
import { SettlementService } from '../../services/settlement.service';
import { Book, User, UserRole, Category, PublisherWalletInfo, RevenueShare } from '../../models';

@Component({
  selector: 'app-seller-dashboard',
  templateUrl: './seller-dashboard.component.html',
  styleUrls: ['./seller-dashboard.component.css']
})
export class SellerDashboardComponent implements OnInit {
  books: Book[] = [];
  categories: Category[] = [];
  currentUser: User | null = null;
  activeTab: 'books' | 'revenue' = 'books';

  showAddForm = false;
  showEditForm = false;
  editingBook: Book | null = null;
  successMessage = '';
  errorMessage = '';

  newBook = { title: '', author: '', description: '', price: 0, status: 'active', categoryId: '', coverImage: '' };
  editBook = { title: '', author: '', description: '', price: 0, status: 'active', categoryId: '', coverImage: '' };

  readonly sellerStatuses = [
    { value: 'draft',        label: '草稿（未上架）' },
    { value: 'active',       label: '正常販售' },
    { value: 'discontinued', label: '停售' }
  ];

  // ── 分潤收入 ──────────────────────────────────────────────
  publisherWallet: PublisherWalletInfo | null = null;
  myRevenue: RevenueShare[] = [];
  revenueSubTab: 'transactions' | 'shares' = 'transactions';
  revenueLoading = false;

  constructor(
    private bookService: BookService,
    private authService: AuthService,
    private walletService: WalletService,
    private settlementService: SettlementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.bookService.getCategories().subscribe(cats => { this.categories = cats; });
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (!user || user.role !== UserRole.SELLER) {
        this.router.navigate(['/']);
        return;
      }
      this.loadBooks();
    });
  }

  switchTab(tab: 'books' | 'revenue'): void {
    this.activeTab = tab;
    if (tab === 'revenue' && !this.publisherWallet) {
      this.loadRevenue();
    }
  }

  loadBooks(): void {
    this.bookService.getMyBooks().subscribe(books => { this.books = books; });
  }

  loadRevenue(): void {
    this.revenueLoading = true;
    this.walletService.getMyPublisherWallet().subscribe({
      next: wallet => { this.publisherWallet = wallet; this.revenueLoading = false; },
      error: () => { this.revenueLoading = false; }
    });
    this.settlementService.getMyRevenue().subscribe(list => { this.myRevenue = list; });
  }

  getCategoryName(categoryId: string): string {
    return this.categories.find(c => c.id === categoryId)?.name ?? '-';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      draft: '草稿', active: '販售中', discontinued: '已停售', banned: '已下架'
    };
    return map[status] ?? status;
  }

  getRevenueTypeLabel(type: string): string {
    return type === 'revenue_share' ? '分潤入帳' : type;
  }

  addBook(): void {
    if (!this.currentUser) return;
    if (!this.newBook.title || !this.newBook.author || !this.newBook.price) {
      this.errorMessage = '請填寫書名、作者和價格';
      return;
    }
    const payload = { ...this.newBook, sellerId: this.currentUser.id, sellerName: this.currentUser.username };
    this.bookService.addBook(payload as any).subscribe({
      next: () => {
        this.showSuccessMessage('書籍新增成功！');
        this.newBook = { title: '', author: '', description: '', price: 0, status: 'active', categoryId: '', coverImage: '' };
        this.showAddForm = false;
        this.loadBooks();
      },
      error: err => { this.errorMessage = err?.error?.message || '新增失敗'; }
    });
  }

  openEdit(book: Book): void {
    this.editingBook = book;
    this.editBook = {
      title: book.title, author: book.author, description: book.description,
      price: book.price, status: book.status, categoryId: book.categoryId, coverImage: book.coverImage
    };
    this.showEditForm = true;
    this.showAddForm = false;
  }

  saveEdit(): void {
    if (!this.editingBook) return;
    this.bookService.updateBook(this.editingBook.id, this.editBook as any).subscribe({
      next: () => {
        this.showSuccessMessage('書籍更新成功！');
        this.showEditForm = false;
        this.editingBook = null;
        this.loadBooks();
      },
      error: err => { this.errorMessage = err?.error?.message || '更新失敗'; }
    });
  }

  cancelEdit(): void { this.showEditForm = false; this.editingBook = null; }

  deleteBook(bookId: string): void {
    if (!confirm('確定要刪除此書籍嗎？')) return;
    this.bookService.deleteBook(bookId).subscribe({
      next: () => { this.showSuccessMessage('書籍已刪除'); this.loadBooks(); },
      error: err => { this.errorMessage = err?.error?.message || '刪除失敗'; }
    });
  }

  private showSuccessMessage(msg: string): void {
    this.successMessage = msg;
    this.errorMessage = '';
    setTimeout(() => { this.successMessage = ''; }, 3000);
  }
}
