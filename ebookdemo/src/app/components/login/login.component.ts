import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginForm = {
    username: '',
    password: ''
  };
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  login(): void {
    if (!this.loginForm.username || !this.loginForm.password) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const request: LoginRequest = {
        username: this.loginForm.username,
        password: this.loginForm.password
      };
      this.authService.login(request).subscribe({
        next: () => {
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.errorMessage = err.message;
          this.isLoading = false;
        }
      });
    } catch (error: any) {
      this.errorMessage = error.message;
      this.isLoading = false;
    }
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  fillDemo(role: string): void {
    const credentials: { [key: string]: { username: string; password: string } } = {
      user: { username: 'user1', password: 'password' },
      seller: { username: 'seller1', password: 'password' },
      admin: { username: 'admin1', password: 'password' }
    };
    const demo = credentials[role];
    if (demo) {
      this.loginForm.username = demo.username;
      this.loginForm.password = demo.password;
    }
  }
}
