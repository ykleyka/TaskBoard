# TaskBoard

TaskBoard — это full-stack система для управления проектами и задачами.

Проект состоит из:

- backend на Spring Boot
- frontend SPA на React + Vite
- PostgreSQL как основной БД
- Swagger / OpenAPI для ручного тестирования API

Система поддерживает пользователей, проекты, участников проекта, задачи, теги, комментарии, JWT-аутентификацию и асинхронную генерацию project summary report.

## Что умеет проект

- регистрация и логин с JWT Bearer token
- SPA-клиент для работы с проектами и задачами
- CRUD для пользователей, проектов, задач, тегов и комментариев
- роли в проекте: `OWNER`, `MANAGER`, `MEMBER`
- назначение участников в проект и bulk-добавление участников
- many-to-many связь `Task <-> Tag`
- one-to-many связи `Project -> Task`, `Task -> Comment`
- фильтрация задач на фронте и search endpoints на backend
- асинхронная генерация summary report по проекту
- runtime-метрики async report task
- Swagger UI для проверки API

## Стек

### Backend

- Java 21
- Spring Boot 4.0.2
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- Spring AOP
- PostgreSQL
- Lombok
- springdoc-openapi

### Frontend

- React 19
- TypeScript
- Vite 6
- lucide-react

### Тесты и качество

- JUnit 5
- Mockito
- JaCoCo
- SonarCloud
- JMeter

## Структура репозитория

```text
TaskBoard
├── docs                    # документация проекта и API
├── frontend                # React SPA
├── load-tests              # JMeter сценарии
├── src/main/java/com/ykleyka/taskboard
│   ├── aop
│   ├── cache
│   ├── config
│   ├── controller
│   ├── dto
│   ├── exception
│   ├── mapper
│   ├── model
│   ├── repository
│   ├── security
│   ├── service
│   └── validation
├── src/main/resources
├── logs
├── pom.xml
└── README.md
```

## Архитектура на уровне идей

### Backend

Backend построен по классической слоистой схеме:

- `controller` — HTTP endpoints
- `service` — бизнес-логика
- `repository` — доступ к БД
- `mapper` — преобразование entity <-> DTO
- `security` — JWT и текущий пользователь
- `cache` — in-memory кэши

### Frontend

Frontend — это SPA, которая:

- логинится через `/api/auth/login` или `/api/auth/register`
- хранит `accessToken` на клиенте
- подставляет `Authorization: Bearer <token>` во все API-запросы
- работает с проектами, задачами, комментариями, тегами и отчетами

## Аутентификация и авторизация

TaskBoard использует stateless JWT-аутентификацию.

### Публичные endpoints

Без токена доступны:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /swagger-ui.html`
- `GET /v3/api-docs`

### Защищенные endpoints

Все остальные endpoints требуют Bearer token:

```http
Authorization: Bearer <accessToken>
```

### Как это работает

1. Пользователь регистрируется или логинится.
2. Backend возвращает `accessToken`.
3. Клиент отправляет токен в заголовке `Authorization`.
4. `JwtAuthenticationFilter` валидирует токен и кладет текущего пользователя в security context.
5. Контроллеры получают пользователя через `@AuthenticationPrincipal`.

### Роли в проекте

- `OWNER` — полный контроль над проектом, удаление проекта, управление ролями
- `MANAGER` — редактирование проекта и задач, управление обычными участниками
- `MEMBER` — доступ на чтение и ограниченные действия внутри проекта

Важно: membership-проверки делаются на backend. Для чужих проектов и задач backend часто возвращает `404`, а не `403`, чтобы не раскрывать существование ресурса.

## Асинхронные отчеты

В проекте есть специализированная async-задача для генерации `PROJECT_SUMMARY_REPORT`.

### Запуск отчета

`POST /api/projects/{id}/summary-report`

Ответ:

```json
{
  "asyncTaskId": "4d7e8147-0a50-44c7-a6b1-90dbf4187c07",
  "status": "SUBMITTED",
  "createdAt": "2026-04-21T09:00:00Z"
}
```

### Проверка статуса

`GET /api/async-tasks/{asyncTaskId}`

Жизненный цикл async-задачи:

```text
SUBMITTED -> RUNNING -> COMPLETED | FAILED
```

### Метрики async report task

`GET /api/async-tasks/metrics`

Возвращает:

- `runningCount`
- `completedCount`
- `failedCount`
- `projectSummaryUnsafeCounter`

Status counters рассчитываются по текущему in-memory task store при вызове endpoint.
`projectSummaryUnsafeCounter` инкрементируется при submit async report.

## Основные сущности

### User

Пользователь системы.

### Project

Проект, внутри которого живут участники и задачи.

### ProjectMember

Связь пользователя и проекта с ролью `OWNER` / `MANAGER` / `MEMBER`.

### Task

Задача проекта. Может иметь:

- создателя
- исполнителя
- дедлайн
- приоритет
- статус
- набор тегов
- комментарии

### Tag

Тег задачи. Связь с задачами many-to-many.

### Comment

Комментарий к задаче. Связь one-to-many от задачи.

## Быстрый старт

### Что нужно

- Java 21
- Node.js 20+ и npm
- PostgreSQL

### 1. Настрой переменные

В корне проекта создай `.env`:

```properties
DB_PASSWORD=your_password
JWT_SECRET=replace-with-a-long-random-secret
FRONTEND_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
FRONTEND_URL=http://localhost:5173
```

### 2. Проверь backend-конфиг

Основные настройки находятся в [src/main/resources/application.properties](/C:/Users/Administrator/IdeaProjects/TaskBoard/src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

taskboard.security.jwt-secret=${JWT_SECRET:change-me-taskboard-dev-secret}
taskboard.security.token-ttl=PT24H
taskboard.cors.allowed-origins=${FRONTEND_ORIGINS:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000}
taskboard.frontend.url=${FRONTEND_URL:http://localhost:5173}
```

Если у тебя другая БД или другой порт, меняй здесь.

## Запуск backend

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

По умолчанию backend будет доступен на:

- [http://localhost:8080](http://localhost:8080)

## Запуск frontend

```powershell
cd frontend
npm install
npm run dev
```

По умолчанию frontend будет доступен на:

- [http://localhost:5173](http://localhost:5173)

Если backend работает не на `http://localhost:8080`, задай переменную:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8081"
npm run dev
```

## Важный нюанс про запуск

В режиме разработки обычно нужно поднимать **два процесса**:

1. Spring Boot backend
2. Vite dev server для frontend

Это нормальная схема для dev-режима. Vite нужен для HMR и быстрой фронтенд-разработки.

## Swagger и API docs

После запуска backend:

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Подробное описание API для проекта лежит в:

- [docs/API.md](/C:/Users/Administrator/IdeaProjects/TaskBoard/docs/API.md)

## Основные endpoints

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

### Projects

- `GET /api/projects`
- `GET /api/projects/{id}`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `PATCH /api/projects/{id}`
- `DELETE /api/projects/{id}`

### Project members

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

### Async tasks

- `POST /api/projects/{id}/summary-report`
- `GET /api/async-tasks/{asyncTaskId}`
- `GET /api/async-tasks/metrics`

## Тесты

Полный запуск тестов:

```powershell
.\mvnw.cmd test
```

Проверка с JaCoCo:

```powershell
.\mvnw.cmd verify
```

Отчет JaCoCo:

```text
target/site/jacoco/jacoco.xml
```

## Нагрузочное тестирование

В проекте есть JMeter-сценарий для async report:

- [load-tests/project-summary-report-loadtest.jmx](/C:/Users/Administrator/IdeaProjects/TaskBoard/load-tests/project-summary-report-loadtest.jmx)

## Логи

- конфиг логирования: [src/main/resources/logback-spring.xml](/C:/Users/Administrator/IdeaProjects/TaskBoard/src/main/resources/logback-spring.xml)
- текущий лог: [logs/taskboard.log](/C:/Users/Administrator/IdeaProjects/TaskBoard/logs/taskboard.log)
- архив логов: [logs/archive](/C:/Users/Administrator/IdeaProjects/TaskBoard/logs/archive)

Дополнительно в проекте есть AOP-логирование времени выполнения service-методов.

## Известные свойства текущей реализации

- токены stateless, refresh token нет
- кэши in-memory, не распределенные
- async counters частично сделаны специально для демонстрации concurrency behavior
- frontend в dev-режиме запускается отдельно от backend
- built frontend пока не встроен в Spring Boot как production bundle

## Полезные файлы

- API contract: [docs/API.md](/C:/Users/Administrator/IdeaProjects/TaskBoard/docs/API.md)
- backend entrypoint: [src/main/java/com/ykleyka/taskboard/TaskBoardApplication.java](/C:/Users/Administrator/IdeaProjects/TaskBoard/src/main/java/com/ykleyka/taskboard/TaskBoardApplication.java)
- frontend app: [frontend/src/App.tsx](/C:/Users/Administrator/IdeaProjects/TaskBoard/frontend/src/App.tsx)
- frontend API client: [frontend/src/api.ts](/C:/Users/Administrator/IdeaProjects/TaskBoard/frontend/src/api.ts)

## SonarCloud

[TaskBoard in SonarCloud](https://sonarcloud.io/summary/new_code?id=ykleyka_TaskBoard&branch=master)
