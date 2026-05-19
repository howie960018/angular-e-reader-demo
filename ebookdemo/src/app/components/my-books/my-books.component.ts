import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { BookService } from '../../services/book.service';
import { Book, User } from '../../models';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-my-books',
  templateUrl: './my-books.component.html',
  styleUrls: ['./my-books.component.css']
})
export class MyBooksComponent implements OnInit {
  purchasedBooks: Book[] = [];
  currentUser: User | null = null;
  isLoading = true;

  constructor(
    private authService: AuthService,
    private bookService: BookService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (!user) {
        this.router.navigate(['/login']);
        return;
      }
      this.loadPurchasedBooks();
    });
  }

  loadPurchasedBooks(): void {
    this.isLoading = true;
    this.bookService.getPurchasedBooks().subscribe({
      next: books => {
        this.purchasedBooks = books;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  readBook(bookId: string): void {
    this.router.navigate(['/read', bookId]);
  }

  goShopping(): void {
    this.router.navigate(['/']);
  }
}
