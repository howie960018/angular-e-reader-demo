import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BookService } from '../../services/book.service';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';
import { WalletService, TopUpCodeInfo } from '../../services/wallet.service';
import { SettlementService } from '../../services/settlement.service';
import {
  Book, Order, User, UserRole, Category, OrderStatus,
  RevenueShare, SettlementSummary, PlatformWalletInfo, PublisherWalletInfo
} from '../../models';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  books: Book[] = [];
  orders: Order[] = [];
  categories: Category[] = [];
  topUpCodes: TopUpCodeInfo[] = [];
  currentUser: User | null = null;
  activeTab: 'books' | 'orders' | 'categories' | 'topupcodes' | 'settlement' | 'wallet' = 'books';

  successMessage = '';
  errorMessage = '';

  // 分類 CRUD
  newCategory = { name: '', description: '' };
  editingCategory: Category | null = null;
  editCategory = { name: '', description: '' };

  // 書籍編輯
  editingBook: Book | null = null;
  allCategories: Category[] = [];
  editBook = { title: '', author: '', description: '', price: 0, status: 'active', categoryId: '', coverImage: '' };

  readonly allStatuses = [
    { value: 'draft',        label: '草稿（未上架）' },
    { value: 'active',       label: '正常販售' },
    { value: 'discontinued', label: '出版社停售' },
    { value: 'banned',       label: '法律強制下架' }
  ];

  // 訂單編輯
  editingOrderId: string | null = null;
  editOrderStatus = '';
  expandedOrderId: string | null = null;

  // 儲值碼
  newCode = { code: '', amount: 0 };

  // ── 金流結算 ────────────────────────────────────────────────
  pendingShares: RevenueShare[] = [];
  settledShares: RevenueShare[] = [];
  settlementResult: SettlementSummary | null = null;
  settlementView: 'pending' | 'history' = 'pending';
  settling = false;

  // ── 金流錢包 ────────────────────────────────────────────────
  platformWallet: PlatformWalletInfo | null = null;
  publisherWallets: PublisherWalletInfo[] = [];
  walletLoading = false;

  constructor(
    private bookService: BookService,
    private orderService: OrderService,
    private authService: AuthService,
    private walletService: WalletService,
    private settlementService: SettlementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (!user || user.role !== UserRole.ADMIN) {
        this.router.navigate(['/']);
        return;
      }
      this.loadAll();
    });
  }

  loadAll(): void {
    this.bookService.getAllBooksForAdmin().subscribe(b => { this.books = b; });
    this.orderService.getAllOrders().subscribe(o => { this.orders = o; });
    this.bookService.getCategories().subscribe(c => { this.categories = c; this.allCategories = c; });
    this.walletService.getAllTopUpCodes().subscribe(codes => { this.topUpCodes = codes; });
  }

  // ── tab 切換（懶載入金流資料） ───────────────────────────────
  switchTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
    if (tab === 'settlement' && this.pendingShares.length === 0 && this.settledShares.length === 0) {
      this.loadSettlement();
    }
    if (tab === 'wallet' && !this.platformWallet) {
      this.loadWalletData();
    }
  }

  // ── 書籍管理 ─────────────────────────────────────────────────
  openEditBook(book: Book): void {
    this.editingBook = book;
    this.editBook = {
      title: book.title, author: book.author, description: book.description,
      price: book.price, status: book.status, categoryId: book.categoryId, coverImage: book.coverImage
    };
  }

  saveEditBook(): void {
    if (!this.editingBook) return;
    this.bookService.updateBook(this.editingBook.id, this.editBook as any).subscribe({
      next: () => {
        this.showSuccess('書籍已更新');
        this.editingBook = null;
        this.bookService.getAllBooksForAdmin().subscribe(b => { this.books = b; });
      },
      error: err => { this.showError(err?.error?.message || '更新失敗'); }
    });
  }

  cancelEditBook(): void { this.editingBook = null; }

  deleteBook(bookId: string): void {
    if (!confirm('確定刪除此書籍？')) return;
    this.bookService.deleteBook(bookId).subscribe({
      next: () => { this.showSuccess('書籍已刪除'); this.bookService.getBooks().subscribe(b => { this.books = b; }); },
      error: err => { this.showError(err?.error?.message || '刪除失敗'); }
    });
  }

  // ── 訂單管理 ─────────────────────────────────────────────────
  toggleOrderDetail(orderId: string): void {
    this.expandedOrderId = this.expandedOrderId === orderId ? null : orderId;
  }

  startEditOrder(order: Order): void {
    this.editingOrderId = order.id;
    this.editOrderStatus = 'CANCELLED';
  }

  saveEditOrder(): void {
    if (!this.editingOrderId) return;
    this.orderService.updateOrderStatus(this.editingOrderId, this.editOrderStatus as OrderStatus).subscribe({
      next: () => {
        this.showSuccess('訂單已取消');
        this.editingOrderId = null;
        this.orderService.getAllOrders().subscribe(o => { this.orders = o; });
      },
      error: err => { this.showError(err?.error?.message || '更新失敗'); }
    });
  }

  cancelEditOrder(): void { this.editingOrderId = null; }

  deleteOrder(orderId: string): void {
    if (!confirm('確定刪除此訂單？')) return;
    this.orderService.deleteOrder(orderId).subscribe({
      next: () => { this.showSuccess('訂單已刪除'); this.orders = this.orders.filter(o => o.id !== orderId); },
      error: err => { this.showError(err?.error?.message || '刪除失敗'); }
    });
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = { COMPLETED: '已完成', PENDING: '處理中', CANCELLED: '已取消' };
    return map[status] ?? status;
  }

  // ── 分類管理 ─────────────────────────────────────────────────
  addCategory(): void {
    if (!this.newCategory.name) { this.showError('請輸入分類名稱'); return; }
    this.bookService.addCategory(this.newCategory.name, this.newCategory.description).subscribe({
      next: () => {
        this.showSuccess('分類新增成功！');
        this.newCategory = { name: '', description: '' };
        this.bookService.getCategories().subscribe(c => { this.categories = c; });
      },
      error: err => { this.showError(err?.error?.message || '新增失敗'); }
    });
  }

  openEditCategory(cat: Category): void {
    this.editingCategory = cat;
    this.editCategory = { name: cat.name, description: cat.description };
  }

  saveEditCategory(): void {
    if (!this.editingCategory) return;
    this.bookService.deleteCategory(this.editingCategory.id).subscribe({
      next: () => {
        this.bookService.addCategory(this.editCategory.name, this.editCategory.description).subscribe({
          next: () => {
            this.showSuccess('分類已更新');
            this.editingCategory = null;
            this.bookService.getCategories().subscribe(c => { this.categories = c; });
          }
        });
      },
      error: err => { this.showError(err?.error?.message || '更新失敗'); }
    });
  }

  cancelEditCategory(): void { this.editingCategory = null; }

  deleteCategory(categoryId: string): void {
    if (!confirm('確定刪除此分類？（需確保分類下無書籍）')) return;
    this.bookService.deleteCategory(categoryId).subscribe({
      next: () => { this.showSuccess('分類已刪除'); this.categories = this.categories.filter(c => c.id !== categoryId); },
      error: err => { this.showError(err?.error?.message || '無法刪除（分類下仍有書籍）'); }
    });
  }

  // ── 儲值碼管理 ───────────────────────────────────────────────
  createTopUpCode(): void {
    if (!this.newCode.code || !this.newCode.amount) { this.showError('請填寫兌換碼和點數'); return; }
    this.walletService.createTopUpCode(this.newCode.code, this.newCode.amount).subscribe({
      next: () => {
        this.showSuccess(`儲值碼 ${this.newCode.code} 建立成功！`);
        this.newCode = { code: '', amount: 0 };
        this.walletService.getAllTopUpCodes().subscribe(codes => { this.topUpCodes = codes; });
      },
      error: err => { this.showError(err?.error?.message || '建立失敗'); }
    });
  }

  deleteTopUpCode(id: string): void {
    if (!confirm('確定刪除此儲值碼？')) return;
    this.walletService.deleteTopUpCode(id).subscribe({
      next: () => { this.showSuccess('儲值碼已刪除'); this.topUpCodes = this.topUpCodes.filter(c => c.id !== id); },
      error: err => { this.showError(err?.error?.message || '刪除失敗'); }
    });
  }

  // ── 金流結算 ─────────────────────────────────────────────────
  loadSettlement(): void {
    this.settlementService.getPending().subscribe(list => { this.pendingShares = list; });
    this.settlementService.getHistory().subscribe(list => { this.settledShares = list; });
  }

  executeSettlement(): void {
    if (!confirm(`確定執行結算？共 ${this.pendingShares.length} 筆待結算`)) return;
    this.settling = true;
    this.settlementResult = null;
    this.settlementService.executeSettlement().subscribe({
      next: result => {
        this.settling = false;
        this.settlementResult = result;
        this.loadSettlement();
        if (this.platformWallet) { this.loadWalletData(); }
      },
      error: err => {
        this.settling = false;
        this.showError(err?.error?.message || '結算失敗');
      }
    });
  }

  // ── 金流錢包 ─────────────────────────────────────────────────
  loadWalletData(): void {
    this.walletLoading = true;
    this.walletService.getPlatformWallet().subscribe({
      next: w => { this.platformWallet = w; this.walletLoading = false; },
      error: () => { this.walletLoading = false; }
    });
    this.walletService.getAllPublisherWallets().subscribe(list => { this.publisherWallets = list; });
  }

  // ── 共用 ─────────────────────────────────────────────────────
  getBookStatusLabel(status: string): string {
    const map: Record<string, string> = {
      draft: '草稿', active: '販售中', discontinued: '已停售', banned: '已下架'
    };
    return map[status] ?? status;
  }

  getCategoryName(categoryId: string): string {
    return this.allCategories.find(c => c.id === categoryId)?.name ?? '-';
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg; this.errorMessage = '';
    setTimeout(() => { this.successMessage = ''; }, 3000);
  }

  private showError(msg: string): void {
    this.errorMessage = msg;
    setTimeout(() => { this.errorMessage = ''; }, 4000);
  }
}
