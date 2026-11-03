# Financial Flow Management API

Навчальний Spring Boot REST API для системи управління фінансовими потоками підприємства.

## Технології

- Java 17+
- Spring Boot 4.0.9 (Spring MVC, Jackson 3)
- Spring Web
- Spring Data JPA
- PostgreSQL
- Maven
- Lombok
- Bean Validation

## Запуск PostgreSQL

```bash
docker compose up -d
```

Цей compose-файл у папці `rest-api` запускає тільки базу для локальної розробки.

## Запуск backend

```bash
mvn spring-boot:run
```

API буде доступне за адресою:

```text
http://localhost:8080/api
```

## Основні endpoints

Для створення або оновлення операції `categoryId` є обов’язковим, а `title` — необов’язковим. Порожня назва нормалізується до `null`.

- `POST /api/transactions`
- `GET /api/transactions`
- `GET /api/transactions/{id}`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`
- `GET /api/transactions?type=EXPENSE&categoryId=1&startDate=2026-01-01&endDate=2026-01-31`
- `POST /api/categories`
- `GET /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`
- `POST /api/budgets`
- `GET /api/budgets`
- `PUT /api/budgets/{id}`
- `DELETE /api/budgets/{id}`
- `GET /api/budgets/{id}/status`
- `GET /api/reports?startDate=2026-01-01&endDate=2026-01-31&userId=1`

## Тести

```bash
mvn test
```

## Оновлення до Spring Boot 4

Оновлено з 3.3.4 через проміжну перевірку на 3.5.16 до 4.0.9.
Java 17 та адреси API залишаються попередніми. Для вебчастини використовується
`spring-boot-starter-webmvc`.

Пошук операцій використовує JPA Criteria та додає лише передані фільтри.
Це усуває помилку типізації параметрів PostgreSQL при фільтрації за датами
з Hibernate 7. Формат JSON для React та Android збережено.
Закриття SSE-з'єднання обробляється без спроби надіслати JSON у недоступний потік.

## Примітка про безпеку

На першому етапі повна авторизація не реалізована. Entity `User` залишена в моделі, щоб надалі додати Spring Security та JWT.
