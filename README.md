# SiliconBay Backend

Simple university-level e-commerce backend built with:
- Java 21
- Embedded Tomcat
- Jersey (JAX-RS)
- Hibernate ORM
- MySQL

## What is implemented

- User auth flow: register, verify, login, logout
- Product APIs: featured, best sellers, product by id
- Cart CRUD
- Wishlist CRUD
- Order flow: checkout + read + status update + delete
- Payment flow: payment methods + saved instruments CRUD
- Transaction flow: read + status update + delete
- Return flow: create + read + status update + delete
- Seller management: create/update own seller profile
- Admin management: users + sellers listing and updates
- Startup DB seeding for demo data

## Base URL

`http://localhost:8080/siliconbay/api`

## Demo users seeded on startup

- Admin
  - email: `admin@siliconbay.com`
  - password: `Admin@123`
- Buyer
  - email: `buyer@siliconbay.com`
  - password: `Buyer@123`
- Seller
  - email: `seller@siliconbay.com`
  - password: `Seller@123`

## Key API routes

### Public
- `GET /test`
- `POST /users`
- `POST /users/login`
- `POST /verify`
- `GET /verify?email=...&verificationCode=...`
- `GET /products/featured`
- `GET /products/best-sellers`
- `GET /products/{id}`

### Authenticated user (`@IsUser`)
- `GET /users/logout`
- `GET /cart`
- `POST /cart/items`
- `PUT /cart/items/{id}`
- `DELETE /cart/items/{id}`
- `DELETE /cart/clear`
- `GET /wishlist`
- `POST /wishlist/items`
- `PUT /wishlist/items/{id}`
- `DELETE /wishlist/items/{id}`
- `DELETE /wishlist/clear`
- `POST /payments/instruments`
- `GET /payments/instruments`
- `PUT /payments/instruments/{id}`
- `DELETE /payments/instruments/{id}`
- `GET /payments/methods`
- `POST /orders/checkout`
- `GET /orders`
- `GET /orders/{id}`
- `PUT /orders/{id}/status`
- `DELETE /orders/{id}`
- `GET /transactions`
- `GET /transactions/{id}`
- `PUT /transactions/{id}/status`
- `DELETE /transactions/{id}`
- `POST /returns`
- `GET /returns`
- `GET /returns/{id}`
- `PUT /returns/{id}/status`
- `DELETE /returns/{id}`
- `POST /sellers`
- `GET /sellers/me`
- `PUT /sellers/me`

### Admin (`@IsAdmin`)
- `GET /admin/users`
- `PUT /admin/users/{id}`
- `GET /admin/sellers`
- `PUT /admin/sellers/{id}`

## Notes

- This project keeps validation/basic rules simple for university use.
- Passwords are stored as plain text by design for this project scope.
- Seeder runs in `ContextPathListener` at startup and is idempotent-friendly.

