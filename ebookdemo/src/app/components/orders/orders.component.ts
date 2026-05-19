import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';
import { Order, User } from '../../models';

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.css']
})
export class OrdersComponent implements OnInit {
  orders: Order[] = [];
  currentUser: User | null = null;

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
  }

  loadOrders(): void {
    this.orderService.getUserOrders().subscribe(orders => {
      this.orders = orders;
    });
  }

  getOrderItemsCount(order: Order): number {
    return order.items.length;
  }
}
