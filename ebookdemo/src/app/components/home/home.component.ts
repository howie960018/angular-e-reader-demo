import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
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
export class HomeComponent implements OnInit {
  books: Book[] = [];
  categories: Category[] = [];
  selectedCategory: string = '';
  currentUser: User | null = null;
  searchText: string = '';

  purchasedBookIds = new Set<string>();
  pendingBookIds = new Set<string>();

  constructor(
    private bookService: BookService,
    private authService: AuthService,
    private cartService: CartService,
    private orderService: OrderService,
    private router: Router
  ) {}

  ngOnInit(): void {
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
    this.bookService.getBooks().subscribe(books => {
      this.books = books;
    });
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

  filterByCategory(categoryId: string): void {
    this.selectedCategory = categoryId;
    if (categoryId) {
      this.bookService.getBooksByCategory(categoryId).subscribe(books => {
        this.books = books;
      });
    } else {
      this.loadBooks();
    }
  }

  get filteredBooks(): Book[] {
    return this.books.filter(book =>
      !this.purchasedBookIds.has(book.id) &&
      (book.title.toLowerCase().includes(this.searchText.toLowerCase()) ||
       book.author.toLowerCase().includes(this.searchText.toLowerCase()))
    );
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
