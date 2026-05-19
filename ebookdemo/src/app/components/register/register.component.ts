import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest, UserRole } from '../../models';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  registerForm = {
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: UserRole.USER
  };
  errorMessage: string = '';
  isLoading: boolean = false;
  UserRole = UserRole;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  register(): void {
    if (!this.registerForm.username || !this.registerForm.email || !this.registerForm.password) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    if (this.registerForm.password !== this.registerForm.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const request: RegisterRequest = {
        username: this.registerForm.username,
        email: this.registerForm.email,
        password: this.registerForm.password,
        role: this.registerForm.role
      };
      this.authService.register(request).subscribe({
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

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
