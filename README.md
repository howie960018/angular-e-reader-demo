# Ebookstore 電子書商城 — 技術文件

> **適用對象**：新進工程師  
> **目的**：快速理解每個模組的前後端運作邏輯、API 設計與開發注意事項  
> **技術棧**：Angular 前端 (`ebookdemo/`) + Spring Boot 後端 (`backend/`) + H2 In-Memory DB

---

## 目錄

1. [系統架構總覽](#系統架構總覽)
2. [身份驗證模組 (Auth)](#模組一身份驗證模組-auth)
3. [書籍管理模組 (Books)](#模組二書籍管理模組-books)
4. [電子書閱讀模組 (Ebook Reader)](#模組三電子書閱讀模組-ebook-reader)
5. [購物車模組 (Cart)](#模組四購物車模組-cart)
6. [結帳與訂單模組 (Checkout / Orders)](#模組五結帳與訂單模組-checkout--orders)
7. [錢包模組 (Wallet)](#模組六錢包模組-wallet)
8. [金流結算模組 (Settlement)](#模組七金流結算模組-settlement)

---

## 系統架構總覽

```
Angular (port 4200)  ──HTTP──►  Spring Boot (port 8080)  ──JPA──►  H2 DB
        │                              │
   AuthInterceptor               JwtAuthenticationFilter
  (自動帶 Bearer Token)          (每個請求驗證 JWT)
```

### 角色權限

| 角色 | 說明 |
|------|------|
| `USER` | 一般使用者，可瀏覽書籍、加購物車、付款、閱讀已購書 |
| `SELLER` | 出版商，可上架書籍、查看自己的分潤收入 |
| `ADMIN` | 管理員，可管理所有書籍/訂單/分類/儲值碼，執行金流結算 |

### JWT 運作機制

- **後端**：`JwtService` 使用 HMAC-SHA256 簽發 Token，payload 包含 `username` 與 `roles`
- **後端**：`JwtAuthenticationFilter` 在每個請求攔截，從 `Authorization: Bearer <token>` 標頭提取並驗證
- **前端**：`AuthInterceptor` 在所有 HTTP 請求自動附加 Token；若收到 `401` 則清除 `localStorage` 的 session

---

## 模組一：身份驗證模組 (Auth)

> **功能概述**：處理使用者的註冊、登入與登出。登入成功後發放 JWT，前端存於 `localStorage` 並透過 `BehaviorSubject` 維持全域登入狀態。

### 前後端邏輯運作流程

#### 登入流程

1. **前端觸發**：使用者在 `LoginComponent` 填寫帳號密碼，點選登入按鈕，呼叫 `AuthService.login()`
2. **API 請求**：
   ```
   POST /api/auth/login
   Body: { "username": "john", "password": "pass123" }
   ```
3. **後端處理**：
   - `AuthController.login()` 呼叫 `AuthenticationManager.authenticate()`，Spring Security 比對 BCrypt 雜湊密碼
   - 驗證通過後，`JwtService.generateToken()` 簽發 JWT
   - 回傳 `AuthResponse`（含 token 與 `UserDto`）
4. **回應與渲染**：
   - HTTP `200 OK`，body 含 `{ token, user: { id, username, email, role } }`
   - `AuthService.storeSession()` 將 token 存入 `localStorage['auth_token']`、user 存入 `localStorage['current_user']`
   - `currentUser$ BehaviorSubject` 推送新值，所有訂閱者（Navbar、路由守衛）立即更新
   - `CartService.loadCart()` 被呼叫，載入該使用者的購物車

#### 註冊流程

1. **前端觸發**：`RegisterComponent` 呼叫 `AuthService.register()`
2. **API 請求**：
   ```
   POST /api/auth/register
   Body: { "username": "...", "email": "...", "password": "...", "role": "USER" | "SELLER" }
   ```
3. **後端處理**：
   - 檢查 username / email 是否已存在，若衝突回傳 `409 Conflict`
   - `AppUserService.registerUser()` 以 BCrypt encode 密碼後存入 DB
   - 同時自動建立該使用者的 `Cart`（`CartRepository`）與 `Wallet`（`WalletRepository`），初始餘額 **1000 點**；SELLER 的 wallet `type` 為 `"seller"`，USER 為 `"user"`
   - 立即簽發 JWT 回傳
4. **回應與渲染**：同登入流程，HTTP `201 Created`，前端直接以新帳號登入狀態進入系統

#### 登出流程

1. **前端觸發**：Navbar 的登出按鈕呼叫 `AuthService.logout()`
2. 清除 `localStorage['auth_token']`、`localStorage['current_user']`
3. `currentUser$` 推送 `null`，`CartService.resetCart()` 將購物車 BehaviorSubject 設為 `null`
4. *(無後端請求，JWT 本身有效直到過期)*

### 核心檔案與元件清單

| 層級 | 檔案 | 負責功能 |
|------|------|---------|
| 前端 | `ebookdemo/src/app/components/login/login.component.ts` | 登入表單 UI |
| 前端 | `ebookdemo/src/app/components/register/register.component.ts` | 註冊表單 UI |
| 前端 | `ebookdemo/src/app/services/auth.service.ts` | 登入/登出邏輯、session 管理、`currentUser$` BehaviorSubject |
| 前端 | `ebookdemo/src/app/interceptors/auth.interceptor.ts` | 自動附加 Bearer Token、處理全域 401 |
| 後端 | `backend/.../controller/AuthController.java` | `/api/auth/login`、`/api/auth/register`、`/api/auth/me` |
| 後端 | `backend/.../service/AppUserService.java` | 使用者 CRUD、實作 `UserDetailsService` |
| 後端 | `backend/.../security/JwtService.java` | JWT 簽發與驗證（HS256） |
| 後端 | `backend/.../security/JwtAuthenticationFilter.java` | 每個請求的 Token 攔截與 SecurityContext 設定 |

### 開發注意事項

- **前端 ID 型態轉換**：後端 `id` 為 `Long`（數字），前端 `User.id` 為 `string`。`AuthService.normalizeUser()` 以 `String(raw.id)` 轉型，所有元件取得的 user id 都是字串，傳給後端前需用 `Number()` 轉回
- **BehaviorSubject 初始值**：`AuthService` 在建構時從 `localStorage` 讀取已存的 user，頁面重整後登入狀態仍保留
- **JWT 過期**：Token 過期時間設定於 `application.properties` 的 `security.jwt.expiration-ms`，前端目前無自動刷新機制，過期後下一個 API 請求會收到 `401`，`AuthInterceptor` 會清除 session 但不自動跳轉登入頁

---

## 模組二：書籍管理模組 (Books)

> **功能概述**：提供書籍的 CRUD 操作。公開 API 只回傳 `active` 狀態的書籍；Seller 可管理自己的書籍（含 `draft`/`discontinued`）；Admin 可管理所有書籍並強制下架（`banned`）。

### 書籍狀態說明

| 狀態 | 說明 | 可設定角色 |
|------|------|-----------|
| `draft` | 草稿，未對外顯示 | SELLER, ADMIN |
| `active` | 正常販售，公開可見 | SELLER, ADMIN |
| `discontinued` | 出版商停售 | SELLER, ADMIN |
| `banned` | 法律強制下架，**即使已購買也無法閱讀** | **ADMIN 限定** |

### 前後端邏輯運作流程

#### 公開書單（首頁）

1. **前端觸發**：`HomeComponent` 初始化，呼叫 `BookService.getBooks()`
2. **API 請求**：
   ```
   GET /api/books
   GET /api/books?categoryId=1       # 依分類篩選
   GET /api/books?sellerId=3         # 依出版商篩選
   ```
3. **後端處理**：`BookController.getAll()` → `BookService.findAll()` 執行 `bookRepo.findByStatus("active")`，僅回傳上架書籍
4. **回應**：`200 OK`，回傳 `BookDto[]`；前端 `normalizeBook()` 將數字 `id` 轉為字串

#### Seller 新增書籍

1. **前端觸發**：`SellerDashboardComponent` 的「新增書籍」表單，呼叫 `addBook()`
2. **API 請求**：
   ```
   POST /api/books
   Authorization: Bearer <token>
   Body: { title, author, description, price, categoryId, coverImage, content, status }
   ```
3. **後端處理**：
   - `@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")` 驗證權限
   - `BookService.create()` 建立 `Book` entity，`seller` 從 JWT 中的 `Authentication` 取得，不可偽造
   - 若 `categoryId` 非 null，查詢 `CategoryRepository` 確認分類存在
4. **回應**：`201 Created`，回傳完整 `BookDto`；前端重新呼叫 `loadBooks()` 更新列表

#### Seller/Admin 編輯書籍

1. **前端觸發**：點選「編輯」按鈕，`openEdit(book)` 將書籍資料填入 `editBook` 物件（含 `content`）；點「儲存」呼叫 `saveEdit()`
2. **API 請求**：
   ```
   PUT /api/books/{id}
   Authorization: Bearer <token>
   Body: { title, author, description, price, categoryId, coverImage, content, status }
   ```
3. **後端處理**：
   - `BookService.update()` 先驗證操作者是否為書籍擁有者或 Admin（`isOwner || isAdmin`）
   - 僅 Admin 可將 `status` 設為 `"banned"`；Seller 嘗試設 `banned` 會收到 `403 Forbidden`
4. **回應**：`200 OK`

#### Admin 刪除書籍

1. **API 請求**：`DELETE /api/books/{id}`（需 SELLER 或 ADMIN 角色）
2. **後端處理**：同樣驗證 owner/admin，`bookRepo.delete(book)` 硬刪除

### 核心檔案與元件清單

| 層級 | 檔案 | 負責功能 |
|------|------|---------|
| 前端 | `ebookdemo/src/app/components/seller-dashboard/seller-dashboard.component.ts` | 出版商書籍新增/編輯/刪除 |
| 前端 | `ebookdemo/src/app/components/seller-dashboard/seller-dashboard.component.html` | 書籍管理表單（含內文 `textarea`） |
| 前端 | `ebookdemo/src/app/components/admin-dashboard/admin-dashboard.component.ts` | 管理員書籍編輯（可設 `banned`） |
| 前端 | `ebookdemo/src/app/services/book.service.ts` | 所有書籍 API 呼叫、`normalizeBook()` |
| 前端 | `ebookdemo/src/app/models/index.ts` | `Book` interface（含 `content?: string`） |
| 後端 | `backend/.../controller/BookController.java` | `/api/books` 所有路由 |
| 後端 | `backend/.../service/BookService.java` | 書籍 CRUD 商務邏輯、權限驗證 |
| 後端 | `backend/.../bean/Book.java` | JPA Entity，`content` 欄位為 `TEXT` 型態 |
| 後端 | `backend/.../dto/BookRequest.java` | 建立/更新書籍的請求 DTO |
| 後端 | `backend/.../dto/BookDto.java` | 回應 DTO，含 `static BookDto from(Book)` 工廠方法 |

### 開發注意事項

- **`content` 欄位**：`Book.content` 在 DB 為 `TEXT`（無長度上限）。`BookRequest` 與 `BookDto` 都已有此欄位；前端 `SellerDashboard` 與 `AdminDashboard` 的 `editBook`/`newBook` 物件也需包含 `content`，否則更新時會帶 `null` 覆蓋既有內容
- **`GET /api/books/all`** vs **`GET /api/books`**：前者給 Admin，回傳所有狀態；後者公開，僅 `active`
- **分類 `categoryId`**：前端 model 中 `categoryId` 為 `string`，傳送給後端前需在 `BookService` 中用 `Number(book.categoryId)` 轉換
- **書籍擁有者識別**：後端 `update()` / `delete()` 直接從 `Authentication`（JWT）取得當前使用者，不可透過 body 偽造 `sellerId`

---

## 模組三：電子書閱讀模組 (Ebook Reader)

> **功能概述**：提供電子書試閱與完整閱讀功能。未購買者只能讀前 **100 字**（`PREVIEW_LENGTH` 常數）；已購買者、出版商本人或 Admin 可讀全文。`banned` 書籍任何人都無法閱讀。

### 前後端邏輯運作流程

#### 取得書籍內容（試閱 / 完整）

1. **前端觸發**：路由導航至 `/ebook/:id`，`EbookComponent.ngOnInit()` 讀取 route param 後呼叫 `loadContent(bookId)`，使用 `BookService.getBookContent()`
2. **API 請求**：
   ```
   GET /api/books/{id}/content
   Authorization: Bearer <token>   ← 若已登入；未登入可省略（permitAll）
   ```
3. **後端處理**（`BookContentController.getContent()`）：
   - 書籍狀態為 `banned` → 直接拋出 `403 ForbiddenException`
   - 解析 `Authentication`（可為 null）：
     - `isAdmin` = role 為 `ADMIN`
     - `isOwner` = 書籍的 `seller.id` 等於當前使用者 id
     - `purchased` = 查詢 `OrderItemRepository.hasPurchasedBook()`（COMPLETED 訂單中含此書）
   - `hasAccess = isAdmin || isOwner || purchased`
   - 回傳 `BookContentResponse`，若無存取權則 `content` 截斷至前 `100` 字
4. **回應**：
   ```json
   {
     "content": "（截斷或完整文字）",
     "hasAccess": false,
     "bookTitle": "...",
     "previewLength": 100,
     "totalLength": 5000
   }
   ```
5. **前端渲染**：
   - `EbookComponent` 依 `\n+` 分段，計算每段在固定寬高（`MAX_WIDTH=420px`, `MAX_HEIGHT=580px`）內的文字量進行**動態分頁**（`paginate()`）
   - `isPreview=true` 時，最後一頁插入 `LOCKED_MARKER` 作為付費牆佔位符，顯示購買引導
   - 支援雙頁 / 單頁閱讀切換、字體縮放（`Ctrl+滾輪`）、深色模式、跳頁
   - 右鍵選單被 `@HostListener('contextmenu')` 停用（防止複製全文）

#### 確認是否已購買（書籍詳情頁）

- `GET /api/books/{id}/purchased` → `{ "purchased": true | false }`
- 供 `BookDetailComponent` 決定顯示「購買」或「閱讀」按鈕

#### 取得已購書列表（我的書籍頁）

- `GET /api/books/purchased`（需登入）→ 查詢所有 `COMPLETED` 訂單中屬於該使用者的書籍
- `MyBooksComponent` 使用此 API 渲染書架

### 核心檔案與元件清單

| 層級 | 檔案 | 負責功能 |
|------|------|---------|
| 前端 | `ebookdemo/src/app/ebook/ebook.component.ts` | 閱讀器核心邏輯：取內容、動態分頁、翻頁、縮放 |
| 前端 | `ebookdemo/src/app/ebook/ebook.component.html` | 閱讀器 UI |
| 前端 | `ebookdemo/src/app/components/my-books/my-books.component.ts` | 「我的書籍」頁面 |
| 前端 | `ebookdemo/src/app/services/book.service.ts` | `getBookContent()`、`checkPurchased()`、`getPurchasedBooks()` |
| 後端 | `backend/.../controller/BookContentController.java` | `GET /api/books/{id}/content`、`/{id}/purchased`、`/purchased` |

### 開發注意事項

- **分頁演算法**：`paginate()` 使用隱藏的 `measureBox` DOM 元素量測文字高度，須在 `setTimeout(0)` 後執行確保 DOM 已渲染；字體變更後以 `repaginateKeepPosition()` 保持閱讀進度
- **`ChangeDetectionStrategy.OnPush`**：`EbookComponent` 使用此策略，資料變更後須手動呼叫 `cdr.markForCheck()`，否則畫面不會更新
- **`PREVIEW_LENGTH` 常數**：目前後端硬寫為 `100` 字（`BookContentController.PREVIEW_LENGTH`），前端 `EbookComponent` 中的 `previewLength: number = 1500` 是初始值，會被後端回應覆蓋
- **安全性**：完整內容截斷在後端進行，前端不可信賴的 `hasAccess` flag 僅用於 UI 渲染，實際資料已在後端截斷

---

## 模組四：購物車模組 (Cart)

> **功能概述**：讓使用者管理待購書籍清單。電子書特性：每本書**固定數量為 1**，不可重複加入已購買或待付款中的書籍。購物車以 `BehaviorSubject` 快取，Navbar 的購物車數字即時更新。

### 前後端邏輯運作流程

#### 加入購物車

1. **前端觸發**：`BookDetailComponent` 點選「加入購物車」，呼叫 `CartService.addToCart(book)`
2. **API 請求**：
   ```
   POST /api/cart/items
   Authorization: Bearer <token>
   Body: { "bookId": 5, "quantity": 1 }
   ```
3. **後端處理**（`CartService.addItem()`，`@Transactional`）：
   - 檢查 `OrderItemRepository.hasPurchasedBook()`：已購買過 → `400 BadRequest`「您已購買過…」
   - 檢查 `OrderItemRepository.hasBookInPendingOrder()`：有待付款訂單含此書 → `400 BadRequest`
   - 查詢 `CartItemRepository.findByCartAndBookId()`：已在購物車 → 直接回傳（不累加）
   - 以 `quantity = 1` 固定建立 `CartItem`
   - `cartItemRepo.flush()` 立即觸發 INSERT，讓 DB UNIQUE(cart_id, book_id) 約束在事務內生效
   - 並發情況下捕捉 `DataIntegrityViolationException`，靜默回傳現有購物車
4. **回應**：`201 Created`，回傳完整 `CartDto`；前端更新 `cart$ BehaviorSubject`

#### 移除購物車商品

- `DELETE /api/cart/items/{bookId}` → 後端 `CartService.removeItem()`，前端以樂觀更新（先修改本地 BehaviorSubject）

#### 清空購物車

- `DELETE /api/cart` → 後端 `CartService.clearCart()`，自動在下單後被呼叫

### 核心檔案與元件清單

| 層級 | 檔案 | 負責功能 |
|------|------|---------|
| 前端 | `ebookdemo/src/app/components/cart/cart.component.ts` | 購物車頁面 |
| 前端 | `ebookdemo/src/app/services/cart.service.ts` | API 呼叫、`cart$ BehaviorSubject` 快取管理 |
| 後端 | `backend/.../controller/CartController.java` | `/api/cart` 所有路由 |
| 後端 | `backend/.../service/CartService.java` | 加入/移除/清空邏輯，含並發防護 |
| 後端 | `backend/.../bean/CartItem.java` | UNIQUE(cart_id, book_id) 約束確保不重複 |

### 開發注意事項

- **`BehaviorSubject` 快取**：`CartService.cart$` 是全域狀態，Navbar、CartComponent、CheckoutComponent 都訂閱此 Observable；登出時呼叫 `resetCart()` 清除，避免 A 使用者看到 B 使用者的購物車
- **並發防護**：`addItem()` 使用 `flush()` + 捕捉 `DataIntegrityViolationException`，防止同一本書被並發重複加入；`getOrCreateCartForUpdate()` 用 `SELECT FOR UPDATE` 鎖定購物車 row，防止結帳時的競態條件
- **電子書數量邏輯**：`updateQuantity()` 若 `quantity <= 0` 視為刪除；`quantity > 0` 時不修改（ebook 永遠 1 份）

---

## 模組五：結帳與訂單模組 (Checkout / Orders)

> **功能概述**：兩階段付款流程 — 先「下單」建立 PENDING 訂單（不扣款），再「付款」完成訂單並扣除錢包點數。PENDING 訂單有 **5 分鐘** 有效期，逾時自動取消（後端排程 + 前端倒數）。

### 訂單狀態機

```
購物車 → [下單] → PENDING → [確認付款] → COMPLETED
                     │
                     └──[使用者取消 / 逾時 5 分鐘] → CANCELLED
```

### 前後端邏輯運作流程

#### 第一階段：下單（建立 PENDING 訂單）

1. **前端觸發**：`CheckoutComponent.completeOrder()` 呼叫 `OrderService.createOrder()`
2. **API 請求**：
   ```
   POST /api/orders
   Authorization: Bearer <token>
   Body: {}
   ```
3. **後端處理**（`OrderService.createFromCart()`，`@Transactional`）：
   - `getOrCreateCartForUpdate()` 以 `SELECT FOR UPDATE` 鎖定購物車（防並發雙重下單）
   - 計算 `totalPrice`
   - 建立 `BookOrder`（`status="PENDING"`），`expiresAt = now + 5 分鐘`
   - 將 `CartItem[]` 複製為 `OrderItem[]`
   - 呼叫 `CartService.clearCart()` 清空購物車
4. **回應**：`201 Created`，回傳 `OrderDto`；前端跳轉至 `/orders`

#### 第二階段：確認付款

1. **前端觸發**：`OrdersComponent.confirmPayment(order)` 呼叫 `OrderService.confirmPayment(orderId)`
2. **API 請求**：
   ```
   POST /api/orders/{id}/pay
   Authorization: Bearer <token>
   ```
3. **後端處理**（`OrderService.confirmPayment()`，`@Transactional`）：
   - 驗證訂單歸屬、狀態為 `PENDING`、未超過 `expiresAt`
   - 呼叫 `WalletService.purchase(user, total, order)` 扣款（`SELECT FOR UPDATE` 鎖定錢包）
   - 為每個 `OrderItem` 呼叫 `SettlementService.createPendingShare()` 建立待結算記錄（平台 30%，出版商 70%）
   - 訂單狀態改為 `COMPLETED`
4. **回應**：`200 OK`；前端更新訂單狀態顯示，書籍進入「我的書籍」

#### 逾時取消機制

- **後端**：`OrderService.cancelExpiredOrders()` 以 `@Scheduled(fixedDelay = 60000)` 每 60 秒掃描一次，批次將逾時 PENDING 訂單改為 `CANCELLED`
- **前端**：`OrdersComponent` 以 `setInterval` 每秒更新倒數顯示，前端倒數歸零時將本地 order 狀態切換為 `CANCELLED`（不呼叫 API，待下次 `loadOrders()` 從後端同步）

#### Admin 強制取消訂單

- `PUT /api/orders/{id}/status`（ADMIN）帶 body `{ "status": "CANCELLED" }`

### 核心檔案與元件清單

| 層級 | 檔案 | 負責功能 |
|------|------|---------|
| 前端 | `ebookdemo/src/app/components/checkout/checkout.component.ts` | 結帳頁，呼叫 createOrder |
| 前端 | `ebookdemo/src/app/components/orders/orders.component.ts` | 訂單列表、確認付款、取消、5 分鐘倒數計時 |
| 前端 | `ebookdemo/src/app/services/order.service.ts` | 所有訂單 API 呼叫 |
| 後端 | `backend/.../controller/OrderController.java` | `/api/orders` 所有路由 |
| 後端 | `backend/.../service/OrderService.java` | 下單、付款、取消、排程清理 |

### 開發注意事項

- **兩階段付款設計**：`createFromCart()` 不扣款僅鎖定，`confirmPayment()` 才真正扣款。這樣的設計讓使用者有機會確認金額，並防止購物車被清空後無法回溯
- **分潤記錄時機**：`RevenueShare` 在 `confirmPayment()` 時建立（`settled=false`），Admin 執行結算前都是待結算狀態；不可在下單時建立，因為此時可能取消
- **`SELECT FOR UPDATE` 鎖定**：`getOrCreateCartForUpdate()` 確保同一用戶並發結帳時，第二個請求拿到鎖後看到空購物車並拋出錯誤
- **`OrdersComponent.ngOnDestroy()`**：務必清除 `setInterval` 的 timerHandle，避免組件銷毀後 timer 繼續執行造成 memory leak

---

## 模組六：錢包模組 (Wallet)

> **功能概述**：管理使用者的點數餘額與交易記錄。支援兌換碼儲值、直接儲值（模擬金流），以及 Admin 手動入款。同時管理平台錢包（platform wallet）與出版商分潤錢包（publisher wallet）。

### 錢包種類

| 錢包 | 對象 | 主要欄位 |
|------|------|---------|
| `Wallet` | USER / SELLER | `balance`（BigDecimal），一般消費用 |
| `PlatformWallet` | 平台（唯一一個） | `balancePoints`，抽成 30% 累積 |
| `PublisherWallet` | 每位 SELLER | `balancePoints`，分潤 70% 累積 |

### 前後端邏輯運作流程

#### 查詢餘額

- **API**：`GET /api/wallet/balance` → `{ "balance": 1000 }`
- `WalletComponent` 訂閱此 API 顯示當前點數

#### 兌換碼儲值

1. **前端觸發**：`WalletComponent` 輸入兌換碼後呼叫 `WalletService.useTopUpCode(userId, code)`
2. **API 請求**：`POST /api/wallet/topup`，body `{ "code": "SUMMER2025" }`
3. **後端處理**（`WalletService.useTopUpCode()`，`@Transactional`）：
   - 查詢 `TopUpCodeRepository.findByCode()`，不存在回傳失敗結果
   - 查詢 `TopUpCodeUsageRepository.existsByTopUpCodeAndUser()`，已使用回傳失敗
   - `usageRepo.flush()` + 捕捉 `DataIntegrityViolationException` 防止並發重複兌換（DB UNIQUE(code_id, user_id)）
   - `getWalletLocked()` 以 `SELECT FOR UPDATE` 鎖定錢包後加點
   - 建立 `WalletTransaction`（type: `"TOPUP"`）
4. **回應**：`PaymentResult { success: true, message: "兌換成功", amount: 500 }`

#### 直接儲值（模擬金流）

- `POST /api/wallet/topup-direct`，body `{ "amount": 500 }`（任何登入使用者皆可）

#### Admin 手動入款

- `POST /api/wallet/deposit`，body `{ "userId": 3, "amount": 200, "description": "補償入款" }`（僅 ADMIN）
- 建立 `DEPOSIT` 型態的 `WalletTransaction`

#### 交易記錄查詢

- `GET /api/wallet/transactions` → 依 `createdAt DESC` 排序的 `WalletTransactionDto[]`

#### 儲值碼管理（Admin）

- `GET /api/wallet/topup-codes` → 所有儲值碼含使用人次
- `POST /api/wallet/topup-codes` → 建立新儲值碼
- `DELETE /api/wallet/topup-codes/{id}` → 刪除（但不影響已使用的 usage 記錄）

### 核心檔案與元件清單

| 層級 | 檔案 | 負責功能 |
|------|------|---------|
| 前端 | `ebookdemo/src/app/components/wallet/wallet.component.ts` | 錢包頁面：餘額、交易記錄、兌換碼 |
| 前端 | `ebookdemo/src/app/services/wallet.service.ts` | 所有錢包 API 呼叫，含三種錢包的 normalize 方法 |
| 後端 | `backend/.../controller/WalletController.java` | `/api/wallet` 所有路由 |
| 後端 | `backend/.../service/WalletService.java` | 餘額操作、兌換碼邏輯、並發防護 |
| 後端 | `backend/.../bean/TopUpCodeUsage.java` | 記錄哪位使用者已兌換過哪個代碼，DB UNIQUE 約束 |

### 開發注意事項

- **交易金額正負號**：`WalletTransaction.amount` 儲存時扣款為**負數**（`amount.negate()`），入帳為正數；前端顯示時需注意符號
- **`SELECT FOR UPDATE`**：`getWalletLocked()` 在每個錢包操作前鎖定 row，避免並發扣款時餘額不足卻仍成功
- **兌換碼設計**：同一個代碼不同使用者可各兌換一次（`TopUpCodeUsage` UNIQUE(code_id, user_id)）；Admin 刪除代碼後，已兌換的 usage 記錄仍保留（無 Cascade Delete）
- **`WalletService` 有兩個 `purchase` overload**：帶 `String description` 的是舊版相容呼叫；帶 `BookOrder order` 的是購書流程使用，後者會在交易記錄中關聯訂單

---

## 模組七：金流結算模組 (Settlement)

> **功能概述**：管理書籍銷售的分潤分配。使用者購書時建立待結算記錄（平台 30%、出版商 70%），由 Admin 手動執行「一鍵結算」，將點數分別入帳至平台錢包與出版商分潤錢包。

### 分潤流程圖

```
使用者確認付款
     │
     ▼
SettlementService.createPendingShare()
  → RevenueShare(settled=false)
  → platformSharePoints = total × 30%
  → publisherSharePoints = total × 70%
     │
     │（等待 Admin 執行結算）
     ▼
SettlementService.settle()
  → PlatformWallet.balancePoints += platformSharePoints
  → PublisherWallet.balancePoints += publisherSharePoints
  → RevenueShare.settled = true
```

### 前後端邏輯運作流程

#### Admin 查看待結算清單

- **API**：`GET /api/settlement/pending`（ADMIN）
- `AdminDashboardComponent` 切換至「金流結算」tab 時懶載入

#### Admin 執行結算

1. **前端觸發**：`AdminDashboardComponent.executeSettlement()` 呼叫 `SettlementService.executeSettlement()`
2. **API 請求**：`POST /api/settlement/execute`（ADMIN）
3. **後端處理**（`SettlementService.settle()`，`@Transactional`）：
   - `revenueShareRepo.findBySettledFalseForUpdate()` 以 `SELECT FOR UPDATE` 鎖定所有待結算記錄（防雙重結算）
   - `platformWalletRepo.findAllForUpdate()` 鎖定平台錢包
   - 逐筆處理：平台錢包入帳，出版商錢包以 `findByPublisherForUpdate()` 各自鎖定後入帳
   - 各建立 `PlatformTransaction` / `PublisherTransaction` 記錄
   - 所有 `RevenueShare` 標記 `settled=true`，記錄 `settledAt`
4. **回應**：`SettlementSummary { count, totalPlatformPoints, totalPublisherPoints, message }`
5. **前端渲染**：顯示結算結果摘要卡片，重新載入待結算清單

#### Seller 查看自己的分潤

- **API**：`GET /api/settlement/my-revenue`（SELLER/ADMIN）
- `SellerDashboardComponent` 的「分潤收入」tab 顯示此資料

#### Seller 查看分潤錢包餘額

- **API**：`GET /api/wallet/publisher/me`（SELLER/ADMIN）
- 回傳出版商分潤錢包的餘額與交易記錄

### 核心檔案與元件清單

| 層級 | 檔案 | 負責功能 |
|------|------|---------|
| 前端 | `ebookdemo/src/app/components/admin-dashboard/admin-dashboard.component.ts` | 結算管理 UI、執行結算、查看歷史 |
| 前端 | `ebookdemo/src/app/components/seller-dashboard/seller-dashboard.component.ts` | Seller 查看分潤收入與錢包 |
| 前端 | `ebookdemo/src/app/services/settlement.service.ts` | `getPending()`、`getHistory()`、`executeSettlement()`、`getMyRevenue()` |
| 後端 | `backend/.../controller/SettlementController.java` | `/api/settlement` 所有路由 |
| 後端 | `backend/.../service/SettlementService.java` | 分潤計算（30/70 拆分）、三層 FOR UPDATE 鎖定防雙重結算 |
| 後端 | `backend/.../bean/RevenueShare.java` | 分潤記錄 Entity（含 `settled`、`settledAt`） |

### 開發注意事項

- **結算是不可逆操作**：`settled=true` 後沒有反轉機制，執行前前端會 `confirm()` 確認
- **三層並發鎖**：`settle()` 依序鎖定「所有待結算記錄 → 平台錢包 → 各出版商錢包」，防止並發結算造成重複入帳；第二個並發請求在第一筆 commit 後會看到空列表直接回傳
- **分潤計算精度**：`PLATFORM_RATE = 0.30`，平台份額以 `HALF_UP` 四捨五入至小數點後 2 位，出版商份額 = total - platform（避免浮點累積誤差）
- **分潤錢包 vs 一般錢包**：`PublisherWallet.balancePoints` 是分潤用途，與 `Wallet.balance`（消費用）是不同的表格，不可混用；出版商的消費扣款走 `Wallet`，分潤收入走 `PublisherWallet`

---

## 附錄：API 路由速查表

| 路由 | Method | 角色 | 功能 |
|------|--------|------|------|
| `/api/auth/login` | POST | 公開 | 登入 |
| `/api/auth/register` | POST | 公開 | 註冊 |
| `/api/auth/me` | GET | 登入 | 取得當前使用者資訊 |
| `/api/books` | GET | 公開 | 取得 active 書籍列表 |
| `/api/books/all` | GET | 登入 | 取得所有書籍（含所有狀態，Admin 用） |
| `/api/books/my` | GET | SELLER/ADMIN | 取得自己的書籍 |
| `/api/books` | POST | SELLER/ADMIN | 新增書籍 |
| `/api/books/{id}` | PUT | SELLER/ADMIN | 更新書籍 |
| `/api/books/{id}` | DELETE | SELLER/ADMIN | 刪除書籍 |
| `/api/books/{id}/content` | GET | 公開 | 取得書籍內容（試閱/完整） |
| `/api/books/{id}/purchased` | GET | 登入 | 確認是否已購買此書 |
| `/api/books/purchased` | GET | 登入 | 取得已購書列表 |
| `/api/cart` | GET | 登入 | 取得購物車 |
| `/api/cart/items` | POST | 登入 | 加入商品 |
| `/api/cart/items/{bookId}` | DELETE | 登入 | 移除商品 |
| `/api/cart` | DELETE | 登入 | 清空購物車 |
| `/api/orders` | POST | 登入 | 下單（建立 PENDING） |
| `/api/orders/{id}/pay` | POST | 登入 | 確認付款 |
| `/api/orders/{id}/cancel` | POST | 登入 | 取消訂單 |
| `/api/orders/all` | GET | ADMIN | 取得所有訂單 |
| `/api/wallet` | GET | 登入 | 取得錢包資訊 |
| `/api/wallet/balance` | GET | 登入 | 取得餘額 |
| `/api/wallet/topup` | POST | 登入 | 兌換碼儲值 |
| `/api/wallet/topup-direct` | POST | 登入 | 直接儲值 |
| `/api/wallet/deposit` | POST | ADMIN | 手動入款 |
| `/api/wallet/platform` | GET | ADMIN | 平台錢包 |
| `/api/wallet/publisher/me` | GET | SELLER/ADMIN | 自己的分潤錢包 |
| `/api/wallet/publishers` | GET | ADMIN | 所有出版商錢包摘要 |
| `/api/wallet/topup-codes` | GET/POST | ADMIN | 儲值碼管理 |
| `/api/wallet/topup-codes/{id}` | DELETE | ADMIN | 刪除儲值碼 |
| `/api/settlement/pending` | GET | ADMIN | 待結算清單 |
| `/api/settlement/history` | GET | ADMIN | 結算歷史 |
| `/api/settlement/execute` | POST | ADMIN | 執行結算 |
| `/api/settlement/my-revenue` | GET | SELLER/ADMIN | 自己的分潤明細 |
| `/api/categories` | GET | 公開 | 取得分類列表 |
| `/api/categories` | POST | ADMIN | 新增分類 |
| `/api/categories/{id}` | DELETE | ADMIN | 刪除分類 |
