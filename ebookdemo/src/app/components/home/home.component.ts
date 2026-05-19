import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BookService } from '../../services/book.service';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { Book, Category, User } from '../../models';

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

  constructor(
    private bookService: BookService,
    private authService: AuthService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadBooks();
    this.loadCategories();
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
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
      book.title.toLowerCase().includes(this.searchText.toLowerCase()) ||
      book.author.toLowerCase().includes(this.searchText.toLowerCase())
    );
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
    this.cartService.addToCart(book, 1).subscribe(() => {
      alert('Added to cart!');
    });
  }
}
