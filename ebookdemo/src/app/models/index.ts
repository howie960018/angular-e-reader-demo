// User roles
export enum UserRole {
  USER = 'USER',
  SELLER = 'SELLER',
  ADMIN = 'ADMIN'
}

// Transaction types
export enum TransactionType {
  DEPOSIT = 'DEPOSIT',
  PAYMENT = 'PAYMENT',
  REFUND = 'REFUND',
  COMMISSION = 'COMMISSION',
  PAYOUT = 'PAYOUT',
  TOPUP = 'TOPUP',
  PURCHASE = 'PURCHASE'
}

// Order status
export enum OrderStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

// User model
export interface User {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  createdAt: Date;
}

// Wallet model
export interface Wallet {
  id: string;
  userId: string;
  type: 'user' | 'system' | 'seller';
  balance: number;
  createdAt: Date;
}

// Wallet transaction
export interface WalletTransaction {
  id: string;
  walletId: string;
  type: TransactionType;
  /** 正數 = 入帳，負數 = 扣款 */
  amount: number;
  description: string;
  orderId?: string;
  bookId?: string;
  createdAt: Date;
}

// Platform wallet transaction
export interface PlatformTransaction {
  id: string;
  type: string;
  amountPoints: number;
  orderId?: string;
  createdAt: Date;
}

// Platform wallet
export interface PlatformWalletInfo {
  id: string;
  balancePoints: number;
  transactions: PlatformTransaction[];
}

// Publisher wallet transaction
export interface PublisherTransaction {
  id: string;
  type: string;
  amountPoints: number;
  orderId?: string;
  createdAt: Date;
}

// Publisher wallet
export interface PublisherWalletInfo {
  publisherId: string;
  publisherName: string;
  balancePoints: number;
  transactions?: PublisherTransaction[];
}

// Revenue share record
export interface RevenueShare {
  id: string;
  orderId: string;
  bookId: string;
  bookTitle: string;
  publisherId: string;
  publisherName: string;
  totalPoints: number;
  platformSharePoints: number;
  publisherSharePoints: number;
  settled: boolean;
  createdAt: Date;
  settledAt?: Date;
}

// Settlement result summary
export interface SettlementSummary {
  count: number;
  totalPlatformPoints: number;
  totalPublisherPoints: number;
  message: string;
}

// Category
export interface Category {
  id: string;
  name: string;
  description: string;
}

// Book
export interface Book {
  id: string;
  title: string;
  author: string;
  description: string;
  price: number;
  categoryId: string;
  sellerId: string;
  sellerName: string;
  coverImage: string;
  content?: string;
  /** draft | active | discontinued | banned */
  status: string;
  createdAt: Date;
  updatedAt: Date;
}

// Cart item
export interface CartItem {
  bookId: string;
  book: Book;
  quantity: number;
}

// Cart
export interface Cart {
  id: string;
  userId: string;
  items: CartItem[];
  createdAt: Date;
  updatedAt: Date;
}

// Order item
export interface OrderItem {
  id: string;
  orderId: string;
  bookId: string;
  book: Book;
  price: number;
  quantity: number;
}

// Order
export interface Order {
  id: string;
  userId: string;
  items: OrderItem[];
  totalPrice: number;
  status: OrderStatus;
  createdAt: Date;
  updatedAt: Date;
}

// Wishlist
export interface Wishlist {
  id: string;
  userId: string;
  bookIds: string[];
  createdAt: Date;
  updatedAt: Date;
}

// TopUp code
export interface TopUpCode {
  id: string;
  code: string;
  amount: number;
  used: boolean;
  usedBy?: string;
  usedAt?: Date;
  createdAt: Date;
}

// Auth response
export interface AuthResponse {
  token: string;
  user: User;
}

// Login request
export interface LoginRequest {
  username: string;
  password: string;
}

// Register request
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role: UserRole;
}
