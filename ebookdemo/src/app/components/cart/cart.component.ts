import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { Cart, User } from '../../models';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {
  cart: Cart | null = null;
  currentUser: User | null = null;

  constructor(
    private cartService: CartService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cartService.getCart().subscribe(cart => {
      this.cart = cart;
    });
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
    });
  }

  removeItem(bookId: string): void {
    this.cartService.removeFromCart(bookId).subscribe();
  }

  updateQuantity(bookId: string, quantity: number): void {
    if (quantity > 0) {
      this.cartService.updateQuantity(bookId, quantity).subscribe();
    }
  }

  getTotalPrice(): number {
    return this.cartService.getTotalPrice();
  }

  checkout(): void {
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }
    this.router.navigate(['/checkout']);
  }

  continueShopping(): void {
    this.router.navigate(['/']);
  }
}
