# Foody - Design Documents

All diagrams are [Mermaid](https://mermaid.js.org/) and render directly on GitHub and in VS Code (Markdown Preview Mermaid Support).

## 1. Architecture

```mermaid
flowchart LR
    subgraph Browser["Browser (plain HTML / CSS / JS)"]
        P[Pages: index, product, cart, orders, profile, admin, login, reset]
        A[api.js - apiFetch + JWT in localStorage]
    end
    subgraph Backend["Spring Boot 3 (Java 17+)"]
        SEC[Security filter chain<br/>JwtAuthFilter + RestErrorHandlers]
        C[Controllers<br/>Auth, Product, Cart, Order, Admin, ...]
        S[Services<br/>Order, Cart, Coupon, Pricing, PasswordReset, CSV]
        EX[GlobalExceptionHandler<br/>ApiErrorResponse]
        R[Spring Data repositories]
    end
    DB[(MongoDB<br/>users, products, carts, orders,<br/>coupons, reviews, restaurant_info)]
    RP[[Razorpay<br/>test mode]]
    M[[SMTP / Gmail]]

    P --> A --> SEC --> C --> S --> R --> DB
    S --> RP
    S --> M
    C -.errors.-> EX
```

## 2. Use case diagram

```mermaid
flowchart LR
    Guest([Guest]):::actor
    Customer([Customer]):::actor
    Admin([Admin]):::actor

    subgraph Foody
        UC1(Browse / search / filter menu)
        UC2(Register)
        UC3(Login)
        UC4(Forgot and reset password)
        UC5(Manage cart)
        UC6(Apply coupon)
        UC7(Place order - COD / Razorpay)
        UC8(Track, filter, cancel, reorder orders)
        UC9(Review delivered dishes)
        UC10(Manage profile, addresses, wishlist)
        UC12(Manage products and coupons)
        UC13(Update order status)
        UC14(Block / unblock users)
        UC15(Dashboard, analytics, CSV export)
        UC16(Set restaurant details and hours)
    end

    Guest --> UC1 & UC2 & UC3 & UC4
    Customer --> UC1 & UC5 & UC6 & UC7 & UC8 & UC9 & UC10
    Admin --> UC12 & UC13 & UC14 & UC15 & UC16
    UC7 -. includes .-> UC6
    classDef actor fill:#fde8e6,stroke:#e84c3d
```

## 3. Data flow diagrams

### Level 0 (context)

```mermaid
flowchart LR
    C([Customer]) -- registration, login, orders, reviews --> F((Foody System))
    F -- menu, order status, invoice --> C
    A([Admin]) -- products, coupons, status updates --> F
    F -- analytics, CSV --> A
    F -- payment request --> RP([Razorpay])
    RP -- payment result --> F
    F -- order / reset emails --> M([Mail server])
```

### Level 1 (place an order)

```mermaid
flowchart TB
    C([Customer]) --> P1[1.0 Manage cart]
    P1 <--> D2[(D2 Carts)]
    P1 --> P2[2.0 Validate checkout<br/>stock, coupon, opening hours]
    P2 <--> D3[(D3 Products)]
    P2 <--> D4[(D4 Coupons)]
    P2 --> P3[3.0 Price order<br/>GST + delivery fee]
    P3 --> P4[4.0 Verify payment]
    P4 <--> RP([Razorpay])
    P4 --> P5[5.0 Save order and reduce stock]
    P5 --> D5[(D5 Orders)]
    P5 --> D3
    P5 --> M([Mail server])
    D5 --> P6[6.0 Track / reorder / invoice]
    P6 --> C
    A([Admin]) --> P7[7.0 Update status] --> D5
```

## 4. Entity relationship diagram

```mermaid
erDiagram
    USER ||--o{ ADDRESS : "has (embedded)"
    USER ||--o| CART : owns
    USER ||--o{ ORDER : places
    USER ||--o{ REVIEW : writes
    USER }o--o{ PRODUCT : "wishlists"
    PRODUCT ||--o{ REVIEW : receives
    PRODUCT ||--o{ CART_ITEM : "in"
    CART ||--|{ CART_ITEM : contains
    ORDER ||--|{ CART_ITEM : "snapshot of items"
    ORDER }o--o| COUPON : "applies"
    ORDER ||--|| RESTAURANT_INFO : "copies"

    USER {
        string id PK
        string name
        string email UK
        string passwordHash
        string role "ROLE_CUSTOMER | ROLE_ADMIN"
        boolean blocked
        string resetTokenHash
        datetime resetTokenExpiry
    }
    PRODUCT {
        string id PK
        string name
        string category
        double price
        int stock
        boolean vegetarian
        int preparationTimeMinutes
    }
    ORDER {
        string id PK
        string userEmail FK
        double subtotalAmount
        double discountAmount
        double taxAmount
        double deliveryFee
        double totalAmount
        string status
        string paymentMode
        string paymentStatus
        datetime orderTime
    }
    COUPON {
        string id PK
        string code UK
        string discountType "PERCENT | FLAT"
        double discountValue
        double minOrderAmount
        date expiryDate
        boolean active
    }
    REVIEW {
        string id PK
        string productId FK
        string userEmail FK
        int rating
        string comment
    }
    CART {
        string id PK
        string userEmail FK
    }
    CART_ITEM {
        string productId FK
        int quantity
        double price
    }
    RESTAURANT_INFO {
        string id PK
        string name
        string openingHours
        double latitude
        double longitude
    }
```

## 5. Sequence diagrams

### Login and authenticated request

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Frontend (api.js)
    participant API as AuthController / AuthService
    participant DB as MongoDB
    U->>FE: email + password
    FE->>API: POST /api/auth/login
    API->>DB: findByEmail
    DB-->>API: user (BCrypt hash)
    API-->>FE: 200 {token, role}  (401 on bad credentials)
    FE->>FE: store token in localStorage
    FE->>API: GET /api/cart (Authorization: Bearer token)
    API->>API: JwtAuthFilter validates token and role
    API-->>FE: cart JSON (401 / 403 JSON error otherwise)
```

### Forgot / reset password

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Frontend
    participant API as PasswordResetService
    participant DB as MongoDB
    participant M as Mail
    U->>FE: enter email
    FE->>API: POST /api/auth/forgot-password
    API->>DB: find user, save SHA-256(token) + 15 min expiry
    API->>M: email link /reset-password.html?token=...
    API-->>FE: generic "if registered, link sent"
    U->>FE: open link, enter new password
    FE->>API: POST /api/auth/reset-password {token, newPassword}
    API->>DB: find by hash, check expiry, save BCrypt hash, clear token
    API-->>FE: 200 (400 if invalid or expired)
```

### Place order

```mermaid
sequenceDiagram
    actor C as Customer
    participant FE as cart.js
    participant O as OrderService
    participant Co as CouponService
    participant Pr as PricingService
    participant Pay as PaymentService
    participant DB as MongoDB
    C->>FE: Place order
    FE->>O: POST /api/orders/place
    O->>O: cart not empty, restaurant open?
    O->>DB: check stock for each item
    O->>Pay: verify Razorpay signature (if online)
    O->>Co: validate coupon
    O->>Pr: GST + delivery fee
    O->>DB: reduce stock, save order, delete cart
    O-->>FE: order (status PLACED)
    O--)C: confirmation email
```

## 6. Class diagram (core)

```mermaid
classDiagram
    class OrderController
    class OrderService {
        +placeOrder(email, request) Order
        +reorder(email, orderId) Map
        +cancelCustomerOrder(email, id) Order
        +getAdminOrdersPage(page,size,status,q) PageResponse
        +getAnalytics() Map
    }
    class PricingService {
        +calculate(subtotal, discount) Breakdown
    }
    class CouponService {
        +validate(code, subtotal) CouponValidationResponse
    }
    class RestaurantInfoService {
        +isOpenNow() boolean
    }
    class PasswordResetService {
        +requestReset(email) String
        +resetPassword(token, newPassword) String
    }
    class ApiException
    class GlobalExceptionHandler
    OrderController --> OrderService
    OrderService --> PricingService
    OrderService --> CouponService
    OrderService --> RestaurantInfoService
    ApiException <|-- ResourceNotFoundException
    ApiException <|-- BadRequestException
    ApiException <|-- ConflictException
    ApiException <|-- UnauthorizedException
    ApiException <|-- ForbiddenException
    GlobalExceptionHandler ..> ApiException : maps to HTTP status
```

## 7. Demo flow (5 minutes)

1. **Start**: `cd backend && ./run-with-test-db.sh`, then `cd frontend && python3 -m http.server 5500`, open `http://localhost:5500`.
2. **Admin** (`admin@gmail.com` / `admin123`): Dashboard -> *Seed products* -> add a coupon `WELCOME10` -> look at the revenue charts.
3. **Customer**: Register -> Login -> browse, *Load more*, filter Veg, toggle dark mode.
4. Add dishes to cart -> see **GST + delivery fee** (free above Rs 499) -> apply `WELCOME10` -> add address -> place a COD order.
5. **Orders**: filter by status, *Reorder*, download invoice.
6. **Admin**: change the order status to Delivered -> *Orders CSV*.
7. **Customer**: leave a review on the delivered dish.
8. **Forgot password**: login page -> *Forgot password* -> copy the link printed in the backend console -> set a new password.
9. **API docs**: `http://localhost:8080/swagger-ui.html` -> Authorize with the login token.
