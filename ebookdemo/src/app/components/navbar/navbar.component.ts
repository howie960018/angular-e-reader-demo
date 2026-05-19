import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { User, UserRole, Cart } from '../../models';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  currentUser$: Observable<User | null>;
  cart$: Observable<Cart | null>;
  cartItemCount: number = 0;
  UserRole = UserRole;

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private router: Router
  ) {
    this.currentUser$ = this.authService.currentUser;
    this.cart$ = this.cartService.getCart();
  }

  ngOnInit(): void {
    this.cart$.subscribe(cart => {
      this.cartItemCount = cart?.items.length ?? 0;
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  goToHome(): void {
    this.router.navigate(['/']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }
}
