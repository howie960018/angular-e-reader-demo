import { Component, OnInit, OnDestroy } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';
import { Order, OrderStatus, User } from '../../models';

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.css']
})
export class OrdersComponent implements OnInit, OnDestroy {
  orders: Order[] = [];
  currentUser: User | null = null;
  countdowns: { [orderId: string]: string } = {};
  actionInProgress: { [orderId: string]: boolean } = {};
  errorMessages: { [orderId: string]: string } = {};

  private timerHandle: any;

  constructor(
    private orderService: OrderService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.loadOrders();
      }
    });
    // 每秒更新倒數計時
    this.timerHandle = setInterval(() => this.updateCountdowns(), 1000);
  }

  ngOnDestroy(): void {
    if (this.timerHandle) {
      clearInterval(this.timerHandle);
    }
  }

  loadOrders(): void {
    this.orderService.getUserOrders().subscribe(orders => {
      this.orders = orders.sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      this.updateCountdowns();
    });
  }

  getOrderItemsCount(order: Order): number {
    return order.items.length;
  }

  getStatusLabel(status: OrderStatus): string {
    switch (status) {
      case OrderStatus.COMPLETED: return '已完成';
      case OrderStatus.PENDING:   return '待付款';
      case OrderStatus.CANCELLED: return '已取消';
      default: return status;
    }
  }

  isPending(order: Order): boolean {
    return order.status === OrderStatus.PENDING;
  }

  isExpired(order: Order): boolean {
    if (!order.expiresAt) return false;
    return new Date() > new Date(order.expiresAt);
  }

  private updateCountdowns(): void {
    const now = new Date().getTime();
    for (const order of this.orders) {
      if (order.status === OrderStatus.PENDING && order.expiresAt) {
        const remaining = new Date(order.expiresAt).getTime() - now;
        if (remaining <= 0) {
          this.countdowns[order.id] = '00:00';
          // 若前端倒數到期，刷新訂單狀態
          if (order.status === OrderStatus.PENDING) {
            order.status = OrderStatus.CANCELLED;
          }
        } else {
          const minutes = Math.floor(remaining / 60000);
          const seconds = Math.floor((remaining % 60000) / 1000);
          this.countdowns[order.id] =
            String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
        }
      }
    }
  }

  confirmPayment(order: Order): void {
    if (this.actionInProgress[order.id]) return;
    this.actionInProgress[order.id] = true;
    this.errorMessages[order.id] = '';

    this.orderService.confirmPayment(order.id).subscribe({
      next: (updated) => {
        const idx = this.orders.findIndex(o => o.id === order.id);
        if (idx !== -1) this.orders[idx] = updated;
        this.actionInProgress[order.id] = false;
      },
      error: (err) => {
        this.errorMessages[order.id] = err?.error?.message || '付款失敗，請再試一次';
        this.actionInProgress[order.id] = false;
        this.loadOrders();
      }
    });
  }

  cancelOrder(order: Order): void {
    if (this.actionInProgress[order.id]) return;
    this.actionInProgress[order.id] = true;
    this.errorMessages[order.id] = '';

    this.orderService.cancelOrder(order.id).subscribe({
      next: (updated) => {
        const idx = this.orders.findIndex(o => o.id === order.id);
        if (idx !== -1) this.orders[idx] = updated;
        this.actionInProgress[order.id] = false;
      },
      error: (err) => {
        this.errorMessages[order.id] = err?.error?.message || '取消失敗，請再試一次';
        this.actionInProgress[order.id] = false;
      }
    });
  }
}
