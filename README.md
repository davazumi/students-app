# Учёт студентов — Spring Boot + простой фронтенд

REST API на Spring Boot (JWT, роли SUPER_ADMIN / ADMIN / TEACHER) и веб-страница на HTML + CSS + JS без сборщиков.

## Запуск

```
start.cmd
```

Откройте в браузере: http://localhost:8080

- `start.cmd` — демо-режим со встроенной базой H2, PostgreSQL не нужен;
- `start.cmd postgres` — работа с PostgreSQL из `application.yml`.

Нужна Java 21+ (скрипт сам найдёт JDK в `%USERPROFILE%\.jdks`, если Java нет в PATH).

## Тестовые пользователи

| Логин | Пароль | Что может |
|---|---|---|
| admin@example.com | admin | просмотр, добавление, изменение и удаление студентов |
| teacher@example.com | teacher | просмотр студентов |
| superadmin@example.com | superAdmin | управление пользователями |

## Что добавлено к исходному проекту

- `src/main/resources/static` — страница (`index.html`, `style.css`, `app.js`), работает с `/api/v1/auth`, `/api/v1/student`, `/api/v1/users`;
- `SecurityConfiguration` — статические файлы доступны без авторизации;
- профиль `demo` (`application-demo.yml`) и зависимость H2;
- в `pom.xml` Lombok подключён как annotation processor (нужно для сборки на Java 23+);
- `start.cmd` — запуск одной командой.
