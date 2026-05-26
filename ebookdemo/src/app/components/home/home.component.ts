import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { BookService } from '../../services/book.service';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';
import { Book, Category, OrderStatus, User, UserRole } from '../../models';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {
  books: Book[] = [];
  categories: Category[] = [];
  selectedCategory: string = '';
  currentUser: User | null = null;
  searchText: string = '';
  sortBy: string = 'default';
  minPrice: number | null = null;
  maxPrice: number | null = null;

  currentPage = 0;
  totalPages = 1;
  totalElements = 0;
  readonly pageSize = 8;

  purchasedBookIds = new Set<string>();
  pendingBookIds = new Set<string>();

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private bookService: BookService,
    private authService: AuthService,
    private cartService: CartService,
    private orderService: OrderService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadBooks();
    });

    this.loadBooks();
    this.loadCategories();
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (user && user.role === UserRole.USER) {
        this.loadPurchasedBooks();
      } else {
        this.purchasedBookIds.clear();
        this.pendingBookIds.clear();
      }
    });
  }

  loadBooks(): void {
    const keyword = this.searchText.trim();
    const fetch$ = this.selectedCategory
      ? this.bookService.getBooksByCategory(this.selectedCategory, this.currentPage, this.pageSize, keyword)
      : this.bookService.getBooks(this.currentPage, this.pageSize, keyword);

    fetch$.subscribe(result => {
      this.books = result.content;
      this.totalPages = result.totalPages;
      this.totalElements = result.totalElements;
      this.currentPage = result.currentPage;
    });
  }

  onSearchChange(value: string): void {
    this.searchText = value;
    this.searchSubject.next(value);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCategories(): void {
    this.bookService.getCategories().subscribe(categories => {
      this.categories = categories;
    });
  }

  private loadPurchasedBooks(): void {
    this.orderService.getUserOrders().subscribe(orders => {
      this.purchasedBookIds.clear();
      this.pendingBookIds.clear();
      for (const order of orders) {
        if (order.status === OrderStatus.COMPLETED) {
          order.items.forEach(item => this.purchasedBookIds.add(item.bookId));
        } else if (order.status === OrderStatus.PENDING) {
          order.items.forEach(item => this.pendingBookIds.add(item.bookId));
        }
      }
    });
  }

  resetFilters(): void {
    this.sortBy = 'default';
    this.minPrice = null;
    this.maxPrice = null;
  }

  get hasActiveFilters(): boolean {
    return this.sortBy !== 'default' || this.minPrice !== null || this.maxPrice !== null;
  }

  filterByCategory(categoryId: string): void {
    this.selectedCategory = categoryId;
    this.currentPage = 0;
    this.loadBooks();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadBooks();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  get filteredBooks(): Book[] {
    const min = this.minPrice !== null && !isNaN(+this.minPrice) ? +this.minPrice : null;
    const max = this.maxPrice !== null && !isNaN(+this.maxPrice) ? +this.maxPrice : null;

    const result = this.books.filter(book =>
      !this.purchasedBookIds.has(book.id) &&
      (min === null || book.price >= min) &&
      (max === null || book.price <= max)
    );

    switch (this.sortBy) {
      case 'price-asc':  return [...result].sort((a, b) => a.price - b.price);
      case 'price-desc': return [...result].sort((a, b) => b.price - a.price);
      case 'title-asc':  return [...result].sort((a, b) => a.title.localeCompare(b.title, 'zh-TW'));
      case 'title-desc': return [...result].sort((a, b) => b.title.localeCompare(a.title, 'zh-TW'));
      case 'newest':     return [...result].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      default:           return result;
    }
  }

  isPending(bookId: string): boolean {
    return this.pendingBookIds.has(bookId);
  }

  get pageTitle(): string {
    if (!this.selectedCategory) return 'All Books';
    const category = this.categories.find(c => c.id === this.selectedCategory);
    return `Books in ${category?.name || 'Category'}`;
  }

  viewBook(bookId: string): void {
    this.router.navigate(['/book', bookId]);
  }

  addToCart(book: Book): void {
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }
    this.cartService.addToCart(book, 1).subscribe({
      next: () => alert('已加入購物車！'),
      error: (err) => alert(err?.error?.message || '加入失敗')
    });
  }
}
