import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { AppRoutingModule } from './app-routing.module';

import { AppComponent } from './app.component';
import { EbookComponent } from './ebook/ebook.component';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { BookDetailComponent } from './components/book-detail/book-detail.component';
import { CartComponent } from './components/cart/cart.component';
import { CheckoutComponent } from './components/checkout/checkout.component';
import { OrdersComponent } from './components/orders/orders.component';
import { WalletComponent } from './components/wallet/wallet.component';
import { SellerDashboardComponent } from './components/seller-dashboard/seller-dashboard.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { MyBooksComponent } from './components/my-books/my-books.component';

@NgModule({
  declarations: [
    AppComponent,
    EbookComponent,
    HomeComponent,
    LoginComponent,
    RegisterComponent,
    BookDetailComponent,
    CartComponent,
    CheckoutComponent,
    OrdersComponent,
    WalletComponent,
    SellerDashboardComponent,
    AdminDashboardComponent,
    NavbarComponent,
    MyBooksComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }