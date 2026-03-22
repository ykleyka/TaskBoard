# TaskBoard

TaskBoard — это REST API на Spring Boot для управления пользователями, проектами, задачами, тегами и комментариями.

## Проверка SonarQube Cloud 

[Ссылка на Sonar](https://sonarcloud.io/summary/new_code?id=ykleyka_TaskBoard&branch=master)

## Возможности

- CRUD-операции для пользователей, проектов, задач, тегов и комментариев
- Добавление участников проекта с ролями
- Поиск задач по проекту и тегу через JPQL
- Поиск просроченных задач через native SQL
- Валидация входных данных через Bean Validation
- Глобальная обработка ошибок с единым форматом ответа
- Swagger / OpenAPI-документация
- Логирование через Logback с ротацией файлов
- AOP-логирование времени выполнения сервисных методов
- In-memory кэширование на основе `HashMap`

## Технологии

- Java 21
- Spring Boot 4.0.2
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring AOP
- PostgreSQL
- Lombok
- Springdoc OpenAPI

## Структура проекта

- `controller` — REST-контроллеры
- `service` — бизнес-логика
- `repository` — слой доступа к данным
- `dto` — request/response модели
- `mapper` — преобразование entity <-> DTO
- `exception` — пользовательские исключения и глобальный обработчик ошибок
- `cache` — in-memory индексы на `HashMap`
- `aop` — аспект логирования времени выполнения
- `config` — конфигурация OpenAPI и приложения

## Реализованные требования

В проекте реализованы:

- глобальная обработка ошибок через `@RestControllerAdvice`
- валидация входных данных через `@Valid`, `@Validated` и аннотации Bean Validation
- единый формат ошибки для всех endpoint
- логирование через Logback с уровнями и ротацией
- аспект для логирования времени выполнения сервисных методов
- Swagger / OpenAPI с описанием endpoint и DTO
- in-memory кэширование с составными ключами и корректными `equals()` / `hashCode()`

## Конфигурация

Основные настройки находятся в:

- `src/main/resources/application.properties`

Проект поддерживает загрузку переменных из `.env` в корне:

```properties
DB_PASSWORD=your_password
```

Актуальные настройки БД:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

## Запуск

Через Maven:

```bash
mvn spring-boot:run
```

Через Maven Wrapper:

```bash
./mvnw spring-boot:run
```

## Swagger / OpenAPI

После запуска приложения документация доступна по адресам:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui/index.html`

OpenAPI JSON:

- `http://localhost:8080/v3/api-docs`

## Логирование

Конфигурация Logback находится в:

- `src/main/resources/logback-spring.xml`

Настроено:

- логирование в консоль
- логирование в файл `logs/taskboard.log`
- архивирование логов в `logs/archive`
- ротация по дате и размеру
- хранение архивов 30 дней

Дополнительно:

- SQL-логи через `spring.jpa.show-sql` отключены
- время выполнения сервисных методов логируется через AOP

Пример записи:

```text
Executed TaskService.searchTasksByProjectIdAndTag(..) in 31 ms
```

## Валидация и обработка ошибок

Валидация применяется к:

- request body DTO
- `@PathVariable`
- `@RequestParam`

В проекте используется единый формат ошибки со следующими полями:

- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `errors`

Ошибки валидации возвращаются в виде списка полей с сообщениями и rejected value.

## In-Memory Кэширование

Кэширование реализовано вручную на основе `HashMap`, как того требуют условия.

Используемые кэши:

- `ProjectCache`
- `TaskCache`
- `TagCache`
- `CommentCache`

Составные ключи:

- `PageKey` — для постраничных списков
- `CommentPageKey` — для комментариев конкретной задачи
- `TaskQueryKey` — для списков и поисковых запросов по задачам

Ключи включают параметры запроса, например:

- тип запроса
- `projectId`
- `tagName`
- `status`
- `assignee`
- фильтр по дате
- `page`
- `size`
- `sort`

Корректная работа кэша обеспечивается через `equals()` и `hashCode()`, сгенерированные Lombok `@EqualsAndHashCode`.

### Что кэшируется

- списки проектов
- детали проекта
- списки тегов
- детали задачи
- списки и результаты поиска задач
- комментарии задачи

### Инвалидация кэша

Кэш очищается при изменении связанных данных:

- создание, обновление, patch и удаление задач
- создание, обновление, patch и удаление проектов
- назначение и удаление тегов у задач
- создание, обновление и удаление комментариев
- обновление и удаление пользователей, если это влияет на связанные проекты, задачи и комментарии

## Обзор API

Базовый путь:

```text
/api
```

### Пользователи

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}`
- `DELETE /api/users/{id}`

### Проекты

- `GET /api/projects`
- `GET /api/projects/{id}`
- `POST /api/projects`
- `POST /api/projects/{id}/members`
- `PUT /api/projects/{id}`
- `PATCH /api/projects/{id}`
- `DELETE /api/projects/{id}`

### Задачи

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}`
- `DELETE /api/tasks/{id}`
- `GET /api/tasks/search`
- `GET /api/tasks/overdue`

### Теги

- `GET /api/tags`
- `POST /api/tags`
- `POST /api/tasks/{taskId}/tags/{tagId}`
- `DELETE /api/tasks/{taskId}/tags/{tagId}`

### Комментарии

- `GET /api/tasks/{taskId}/comments`
- `POST /api/tasks/{taskId}/comments`
- `PUT /api/comments/{id}`
- `DELETE /api/comments/{id}`

## Пагинация и сортировка

Обычные списочные endpoint поддерживают стандартные параметры Spring `Pageable`:

- `page`
- `size`
- `sort`

Значения по умолчанию:

- `page=0`
- `size=20`

Поисковые endpoint упрощены:

- `GET /api/tasks/search` принимает только `page` и `size`, а сортировка фиксируется в сервисе
- `GET /api/tasks/overdue` принимает только `page` и `size`, а сортировка фиксируется в сервисе

Фиксированный порядок сортировки:

- поиск задач по проекту и тегу: `id ASC`
- поиск просроченных задач: `due_date ASC, id ASC`

## Поиск задач

### `GET /api/tasks/search`

Обязательные параметры:

- `projectId`
- `tagName`

Необязательные параметры:

- `status`
- `assignee`
- `page`
- `size`

### `GET /api/tasks/overdue`

Обязательные параметры:

- `projectId`
- `tagName`

Необязательные параметры:

- `status`
- `assignee`
- `dueBefore`
- `page`
- `size`

Дополнительные условия для overdue-поиска:

- у задачи должен быть `dueDate`
- `dueDate < dueBefore`
- статус задачи не должен быть `COMPLETED`

## Сборка

Компиляция:

```bash
mvn compile
```

Запуск тестов:

```bash
mvn test
```
