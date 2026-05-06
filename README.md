# TaskBoard

TaskBoard - full-stack приложение для управления проектами и задачами.

Проект состоит из:

- backend на Spring Boot;
- frontend SPA на React + Vite;
- PostgreSQL в качестве основной базы данных;
- JWT-аутентификации;
- Swagger / OpenAPI для ручной проверки API;
- Docker-сборки для production-запуска;
- GitHub Actions workflow для сборки, тестов, Docker build, деплоя и healthcheck.

## Возможности

- регистрация и вход по JWT Bearer token;
- CRUD для пользователей, проектов, задач, тегов и комментариев;
- роли участников проекта: `OWNER`, `MANAGER`, `MEMBER`;
- управление участниками проекта, включая bulk-добавление;
- связь `Task <-> Tag` many-to-many;
- связи `Project -> Task` и `Task -> Comment`;
- фильтрация задач и search endpoints;
- асинхронная генерация project summary report;
- runtime-метрики async report task;
- React SPA для работы с проектами и задачами;
- healthcheck через Spring Boot Actuator.

## Стек

### Backend

- Java 21;
- Spring Boot 4.0.2;
- Spring Web;
- Spring Security;
- Spring Data JPA;
- Spring Boot Actuator;
- Bean Validation;
- Spring AOP;
- PostgreSQL;
- Lombok;
- springdoc-openapi;
- JUnit 5, Mockito, JaCoCo.

### Frontend

- React 19;
- TypeScript;
- Vite 6;
- lucide-react.

### Infrastructure

- Dockerfile с multi-stage build;
- Docker Compose для запуска приложения с существующей БД в Docker;
- опциональный Compose override для полного локального стека app + Postgres;
- PaaS-деплой Docker-приложения через ручную настройку в панели хостинга;
- GitHub Actions CI/CD.

## Структура

```text
TaskBoard
|-- .github/workflows
|   |-- ci-cd.yml
|   `-- sonar.yml
|-- frontend
|   `-- src
|-- src/main/java/com/ykleyka/taskboard
|   |-- aop
|   |-- cache
|   |-- config
|   |-- controller
|   |-- dto
|   |-- exception
|   |-- mapper
|   |-- model
|   |-- repository
|   |-- security
|   |-- service
|   `-- validation
|-- src/main/resources
|-- src/test/java/com/ykleyka/taskboard
|-- Dockerfile
|-- docker-compose.yml
|-- docker-compose.with-db.yml
|-- DEPLOYMENT.md
`-- pom.xml
```

## Переменные окружения

Пример локальных значений находится в `.env.example`.

Основные переменные backend:

```properties
PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/taskboard
SPRING_DATASOURCE_USERNAME=taskboard
SPRING_DATASOURCE_PASSWORD=change-me
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
FRONTEND_URL=http://localhost:8080
FRONTEND_ORIGINS=http://localhost:8080
```

Для удобства Docker Compose также поддерживает:

```properties
POSTGRES_DB=taskboard
POSTGRES_USER=taskboard
DB_PASSWORD=change-me
DB_HOST=host.docker.internal
DB_PORT=5432
APP_PORT=8080
```

Если задан `SPRING_DATASOURCE_URL`, он используется напрямую.

## Локальный запуск без Docker

Нужна уже запущенная PostgreSQL.

Создай `.env` в корне проекта:

```properties
DB_PASSWORD=change-me
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/taskboard
SPRING_DATASOURCE_USERNAME=taskboard
SPRING_DATASOURCE_PASSWORD=change-me
FRONTEND_URL=http://localhost:5173
FRONTEND_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

Запуск backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Если Maven wrapper на Windows не запускается, можно использовать установленный Maven:

```powershell
mvn spring-boot:run
```

Запуск frontend:

```powershell
cd frontend
npm install
npm run dev
```

Адреса по умолчанию:

- backend: `http://localhost:8080`;
- frontend dev server: `http://localhost:5173`;
- Swagger UI: `http://localhost:8080/swagger-ui.html`;
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`;
- healthcheck: `http://localhost:8080/actuator/health`.

## Запуск через Docker Compose

Текущий `docker-compose.yml` рассчитан на ситуацию, когда база данных уже находится в отдельном Docker-контейнере.

Если контейнер PostgreSQL публикует порт `5432` на хост, оставь:

```properties
DB_HOST=host.docker.internal
DB_PORT=5432
```

Затем запусти приложение:

```powershell
docker compose up --build
```

Приложение будет доступно на:

```text
http://localhost:8080
```

Если база доступна только внутри Docker-сети, подключи приложение к той же сети и укажи `DB_HOST` равным имени контейнера или service name базы.

## Опциональный полный стек

Если нужно поднять новую локальную PostgreSQL вместе с приложением:

```powershell
docker compose -f docker-compose.yml -f docker-compose.with-db.yml up --build
```

Этот режим использует volume `taskboard-postgres-data` и подключает приложение к сервису `db`.

## Production Docker Image

`Dockerfile` собирает приложение в три этапа:

1. собирает frontend через `npm ci` и `npm run build`;
2. копирует frontend bundle в `src/main/resources/static`;
3. собирает Spring Boot jar и запускает его на JRE 21.

В production-контейнере frontend и backend обслуживаются одним Spring Boot приложением.

Healthcheck внутри контейнера проверяет:

```text
/actuator/health
```

## PaaS Деплой

Приложение можно развернуть как Docker-based web service в панели хостинга.

Требуется:

- подключить GitHub-репозиторий;
- выбрать ветку `main` или `master`;
- указать root directory как корень репозитория;
- добавить переменные окружения для подключения к PostgreSQL;
- включить auto deploy, если нужен автоматический деплой после push.

Подробные шаги, включая перенос данных из локального Docker-контейнера PostgreSQL, описаны в `DEPLOYMENT.md`.

## GitHub CI/CD

Workflow:

```text
.github/workflows/ci-cd.yml
```

Выполняет:

- сборку frontend;
- запуск backend-тестов;
- сборку Docker image;
- деплой через deploy hook, если он задан в окружении;
- healthcheck опубликованного сервиса.

Для healthcheck нужно добавить GitHub repository secret:

```text
DEPLOYMENT_URL
```

Опциональная repository variable:

```text
DEPLOY_WAIT_SECONDS
```

Если `DEPLOYMENT_URL` не задан, workflow пропустит healthcheck.

## Тесты

Backend:

```powershell
.\mvnw.cmd test
```

или:

```powershell
mvn test
```

Проверка с JaCoCo:

```powershell
mvn verify
```

Frontend production build:

```powershell
cd frontend
npm run build
```

## Основные API Endpoints

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

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

### Async Tasks

- `POST /api/projects/{id}/summary-report`
- `GET /api/async-tasks/{asyncTaskId}`
- `GET /api/async-tasks/metrics`

### Health

- `GET /actuator/health`

## Swagger

После запуска backend:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

## Известные особенности

- токены stateless, refresh token пока нет;
- кэши in-memory и не распределены между инстансами;
- async task store in-memory;
- в dev-режиме frontend обычно запускается отдельно через Vite;
- в production Docker image frontend встроен в Spring Boot static resources;
- `spring.jpa.hibernate.ddl-auto=update` подходит для учебного проекта, но для production лучше перейти на миграции.

## Дополнительная документация

- `DEPLOYMENT.md` - Docker и GitHub CI/CD;
- `docs/API.md` - подробное описание API, если папка `docs` присутствует в рабочей копии;
- `load-tests/` - JMeter-сценарии, если папка присутствует в рабочей копии.

## SonarCloud

[TaskBoard in SonarCloud](https://sonarcloud.io/summary/new_code?id=ykleyka_TaskBoard&branch=master)
