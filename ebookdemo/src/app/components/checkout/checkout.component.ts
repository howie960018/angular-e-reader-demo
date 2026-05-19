import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';
import { WalletService } from '../../services/wallet.service';
import { AuthService } from '../../services/auth.service';
import { Cart, User } from '../../models';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  cart: Cart | null = null;
  currentUser: User | null = null;
  walletBalance: number = 0;
  isProcessing: boolean = false;
  errorMessage: string = '';

  constructor(
    private cartService: CartService,
    private orderService: OrderService,
    private walletService: WalletService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cartService.getCart().subscribe(cart => {
      this.cart = cart;
    });

    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.walletService.getWalletBalance().subscribe(balance => {
          this.walletBalance = balance;
        });
        // Ensure cart is loaded
        this.cartService.loadCart().subscribe();
      }
    });
  }

  getTotalPrice(): number {
    return this.cartService.getTotalPrice();
  }

  completeOrder(): void {
    if (!this.currentUser || !this.cart || this.cart.items.length === 0) return;

    const totalPrice = this.getTotalPrice();
    if (this.walletBalance < totalPrice) {
      this.errorMessage = 'Insufficient balance in wallet';
      return;
    }

    this.isProcessing = true;
    this.errorMessage = '';

    // Backend handles wallet deduction + cart clearing in one call
    this.orderService.createOrder().subscribe({
      next: () => {
        this.router.navigate(['/orders']);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Order failed. Please try again.';
        this.isProcessing = false;
        // Refresh balance in case it changed
        this.walletService.getWalletBalance().subscribe(b => { this.walletBalance = b; });
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/cart']);
  }
}
