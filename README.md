# TaskBoard

TaskBoard — это REST API на Spring Boot для управления проектами, задачами, тегами, комментариями и пользователями.

## Возможности
- CRUD для пользователей, проектов, задач, тегов и комментариев
- Поиск задач по вложенным сущностям (проект + теги) через JPQL
- Поиск просроченных задач через native SQL
- Пагинация и сортировка для списков (Pageable)
- In-memory кэши на HashMap для часто запрашиваемых списков и поисков с инвалидацией при изменениях
## Технологии
- Java 21
- Spring Boot 4.0.2, Spring Web, Spring Data JPA, Validation
- PostgreSQL
- Lombok

## Установка
1. Установите Java 21 и Maven.
2. Запустите PostgreSQL и выберите базу данных.
3. Настройте доступ к базе в `src/main/resources/application.properties`.
4. Опционально создайте `.env` в корне проекта:

```properties
DB_PASSWORD=your_password
```

Файл `.env` подхватывается через `spring.config.import=optional:file:./.env[.properties]`.

## Запуск
```bash
mvn spring-boot:run
```

Или:

```bash
./mvnw spring-boot:run
```

## API
Базовый путь: `/api`

Пользователи:
- GET `/users`
- GET `/users/{id}`
- POST `/users`
- PUT `/users/{id}`
- PATCH `/users/{id}`
- DELETE `/users/{id}`

Проекты:
- GET `/projects`
- GET `/projects/{id}`
- POST `/projects`
- POST `/projects/{id}/members`
- PUT `/projects/{id}`
- PATCH `/projects/{id}`
- DELETE `/projects/{id}`

Задачи:
- GET `/tasks`
- GET `/tasks/{id}`
- POST `/tasks`
- PUT `/tasks/{id}`
- PATCH `/tasks/{id}`
- DELETE `/tasks/{id}`
- GET `/tasks/search` (JPQL)
- GET `/tasks/overdue` (native SQL)

Теги:
- GET `/tags`
- POST `/tags`
- POST `/tasks/{taskId}/tags/{tagId}`
- DELETE `/tasks/{taskId}/tags/{tagId}`

Комментарии:
- GET `/tasks/{taskId}/comments`
- POST `/tasks/{taskId}/comments`
- PUT `/comments/{id}`
- DELETE `/comments/{id}`

## Пагинация и сортировка
Все списоковые эндпоинты принимают стандартные параметры Spring Data:
- `page` (с 0), `size`, `sort` (например `sort=dueDate,desc`)

По умолчанию:
- `page=0`, `size=20`

Важно: контроллеры возвращают только список элементов без Page-метаданных. Если нужны метаданные, возвращайте `Page<T>` из контроллера.

## Поиск задач
JPQL поиск: `GET /api/tasks/search`

Обязательные параметры:
- `projectId`
- `tagName`

Опциональные параметры:
- `status`
- `assignee`
- `page`, `size`, `sort`

Native поиск: `GET /api/tasks/overdue`

Обязательные параметры:
- `projectId`
- `tagName`

Опциональные параметры:
- `status`
- `assignee`
- `dueBefore` (Instant в ISO-8601, по умолчанию текущее время)

Фильтры:
- задача связана с указанным тегом
- `dueDate < dueBefore`
- статус задачи не COMPLETED

## Кэширование
In-memory кэши построены на HashMap с составными ключами:
- `TaskSearchCache` использует `TaskSearchKey` (projectId, tagName, status, assignee, dueBefore, page, size, sort, nativeQuery)
- `PageKey` используется для постраничных списков пользователей, проектов и тегов
- `CommentPageKey` добавляет taskId для комментариев конкретной задачи

Кэши инвалидируются при create, update, patch, delete, а также при назначении/удалении тега у задачи.

## Дефолты и валидация
- В POST и PUT `/tasks` при null-статусе применяется TODO.
- Валидация обеспечивается `@Valid` и аннотациями bean validation.
