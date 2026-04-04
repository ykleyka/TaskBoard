# TaskBoard


`TaskBoard` — REST API на Spring Boot для управления:
- пользователями
- проектами
- участниками проектов (ролями)
- задачами
- тегами
- комментариями

Проект сделан как учебный backend с упором на чистую архитектуру сервисного слоя, валидацию, обработку ошибок, кэширование и покрытие тестами.

## Проверка Sonar
[Ccылка на проект](https://sonarcloud.io/summary/new_code?id=ykleyka_TaskBoard&branch=master)

## Возможности

- Полный CRUD для `users`, `projects`, `tasks`, `comments`
- Управление участниками проекта:
- `GET /api/projects/{id}/members`
- `GET /api/projects/{id}/members/{userId}`
- `POST /api/projects/{id}/members`
- `POST /api/projects/{id}/members/bulk`
- `PUT /api/projects/{id}/members/{userId}`
- `DELETE /api/projects/{id}/members/{userId}`
- Поиск задач:
- `GET /api/tasks/search` (JPQL)
- `GET /api/tasks/overdue` (native SQL)
- Глобальная обработка ошибок в едином формате (`@RestControllerAdvice`)
- Валидация входных данных (`@Valid`, `@Validated`, Bean Validation)
- In-memory кэширование на `HashMap`
- AOP-логирование времени выполнения сервисных методов
- Swagger / OpenAPI документация

## Технологии

- Java 21
- Spring Boot 4.0.2
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring AOP
- PostgreSQL
- Lombok
- springdoc-openapi
- JUnit 5 + Mockito
- JaCoCo + SonarQube Cloud

## Структура проекта

- `src/main/java/com/ykleyka/taskboard/controller` — REST-контроллеры
- `src/main/java/com/ykleyka/taskboard/service` — бизнес-логика
- `src/main/java/com/ykleyka/taskboard/repository` — доступ к БД
- `src/main/java/com/ykleyka/taskboard/dto` — DTO (request/response)
- `src/main/java/com/ykleyka/taskboard/mapper` — преобразование entity ↔ DTO
- `src/main/java/com/ykleyka/taskboard/exception` — исключения и глобальный обработчик
- `src/main/java/com/ykleyka/taskboard/cache` — in-memory кэши
- `src/main/java/com/ykleyka/taskboard/aop` — аспект логирования

## Быстрый старт

1. Установите:
- JDK 21
- PostgreSQL

2. Создайте `.env` в корне проекта:

```properties
DB_PASSWORD=your_password
```

3. Проверьте `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

4. Запустите приложение:

```bash
mvn spring-boot:run
```

## Swagger / OpenAPI

После запуска:
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

## Ключевые API эндпоинты

### Users

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}`
- `DELETE /api/users/{id}`

### Projects

- `GET /api/projects`
- `GET /api/projects/{id}`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `PATCH /api/projects/{id}`
- `DELETE /api/projects/{id}`

### Project Members

- `GET /api/projects/{id}/members`
- `GET /api/projects/{id}/members/{userId}`
- `POST /api/projects/{id}/members`
- `POST /api/projects/{id}/members/bulk`
- `PUT /api/projects/{id}/members/{userId}`
- `DELETE /api/projects/{id}/members/{userId}`

### Tasks

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}`
- `DELETE /api/tasks/{id}`
- `GET /api/tasks/search`
- `GET /api/tasks/overdue`

### Tags

- `GET /api/tags`
- `POST /api/tags`
- `POST /api/tasks/{taskId}/tags/{tagId}`
- `DELETE /api/tasks/{taskId}/tags/{tagId}`

### Comments

- `GET /api/tasks/{taskId}/comments`
- `POST /api/tasks/{taskId}/comments`
- `PUT /api/comments/{id}`
- `DELETE /api/comments/{id}`

## Bulk-операция участников проекта

Реализован endpoint:
- `POST /api/projects/{id}/members/bulk`

Назначение:
- массово добавить список участников в проект
- поддерживается проверка дублей в одном запросе
- операция обернута в транзакцию (`@Transactional`)

Это позволяет на защите показать разницу поведения БД с/без транзакции.

## Кэширование

Используются кэши:
- `ProjectCache`
- `TaskCache`
- `TagCache`
- `CommentCache`

Составные ключи:
- `PageKey`
- `CommentPageKey`
- `TaskQueryKey`

Инвалидация выполняется в сервисах при изменении связанных данных.

## Логирование

- Конфигурация: `src/main/resources/logback-spring.xml`
- Логи:
- `logs/taskboard.log`
- `logs/archive/*`
- AOP пишет время выполнения методов сервисного слоя

## Обработка ошибок

Единый формат ответа:
- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `errors`

## Тесты

Запуск тестов:

```bash
mvn test
```

Проект содержит unit-тесты сервисного слоя на Mockito.

## Coverage и SonarQube Cloud

JaCoCo отчет генерируется на фазе `verify`, Sonar берет XML из:
- `target/site/jacoco/jacoco.xml`

## Примечание

В проекте не реализована аутентификация/авторизация — это чистый учебный REST backend для демонстрации архитектуры и качества кода.
