import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
})
export class LoginPageComponent implements OnInit {
  private auth  = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  ngOnInit() {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/app/dashboard'], { replaceUrl: true });
    }
  }

  view        = signal<'login' | 'otp' | 'forgot'>('login');
  email       = signal('');
  password    = signal('');
  otp         = signal('');
  showPass    = signal(false);
  loading     = this.auth.isLoading;
  errorMsg    = signal('');

  doLogin() {
    this.errorMsg.set('');
    this.auth.login(this.email(), this.password()).subscribe({
      error: (err) => this.errorMsg.set(err.message),
    });
  }

  doForgot() {
    this.auth.forgotPassword(this.email()).subscribe({
      next: () => { this.toast.success('Reset link sent!'); this.view.set('login'); },
      error: (err) => this.errorMsg.set(err.message),
    });
  }

  doVerifyOtp() {
    this.errorMsg.set('');
    this.auth.verifyOtp(this.email(), this.otp()).subscribe({
      error: (err) => this.errorMsg.set(err.message),
    });
  }
}
