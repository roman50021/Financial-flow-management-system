# Financial Flow Management System

Навчальний проєкт "Система управління фінансовими потоками".

## Структура

- `rest-api` - Spring Boot REST API з PostgreSQL, JPA, DTO, validation, exception handler і service tests.
- `react-frontend` - базовий React/Vite вебінтерфейс для роботи з API.
- `android-app` - Android-клієнт на Kotlin і Jetpack Compose для перегляду, створення та видалення операцій.

## Швидкий старт backend

```bash
cd rest-api
docker compose up -d
mvn spring-boot:run
```

## Docker: тільки backend

Підіймає PostgreSQL і Spring Boot API.

```bash
docker compose -f docker-compose.backend.yml up --build
```

Backend буде доступний тут:

```text
http://localhost:8080/api
```

## Docker: backend і frontend

Підіймає PostgreSQL, Spring Boot API і React frontend.

```bash
docker compose -f docker-compose.full.yml up --build
```

Адреси:

```text
Frontend: http://localhost:3000
Backend:  http://localhost:8080/api
```

## Швидкий старт frontend

```bash
cd react-frontend
npm install
npm run dev
```

## Швидкий старт Android

Після запуску backend відкрийте папку `android-app` в Android Studio та запустіть модуль `app` на емуляторі. Для емулятора вже налаштовано адресу `http://10.0.2.2:8080/api`.

Зібрати debug APK можна з термінала:

```bash
cd android-app
./gradlew assembleDebug
```

Детальніше: [`android-app/README.md`](android-app/README.md).
