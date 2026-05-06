# Deployment

This project is prepared to be deployed as a Docker-based web service with a separate PostgreSQL database.

## 1. Prepare The Repository

Make sure these files are committed:

```text
Dockerfile
.dockerignore
.github/workflows/ci-cd.yml
README.md
DEPLOYMENT.md
```

Push the repository to GitHub:

```powershell
git add .
git commit -m "Prepare deployment"
git push
```

## 2. Prepare The Database

Create a PostgreSQL database that accepts external connections.

You need these values:

```text
host
port
database
username
password
```

Build the JDBC URL:

```text
jdbc:postgresql://HOST:PORT/DATABASE
```

## 3. Export Data From The Local Docker Container

Find the local PostgreSQL container:

```powershell
docker ps
```

Create a dump. Replace the container name, user, and database name:

```powershell
docker exec taskboard-postgres pg_dump `
  -U taskboard `
  -d taskboard `
  --format=custom `
  --no-owner `
  --no-privileges `
  --file=/tmp/taskboard.dump
```

Copy the dump to the project directory:

```powershell
docker cp taskboard-postgres:/tmp/taskboard.dump .\taskboard.dump
```

## 4. Restore Data To The Hosted PostgreSQL Database

If PostgreSQL client tools are installed locally:

```powershell
$env:REMOTE_DATABASE_URL="postgresql://USER:PASSWORD@HOST:PORT/DATABASE"

pg_restore `
  --verbose `
  --clean `
  --if-exists `
  --no-owner `
  --no-privileges `
  --dbname "$env:REMOTE_DATABASE_URL" `
  .\taskboard.dump
```

If PostgreSQL client tools are not installed, use Docker:

```powershell
$env:REMOTE_DATABASE_URL="postgresql://USER:PASSWORD@HOST:PORT/DATABASE"

docker run --rm `
  -e REMOTE_DATABASE_URL="$env:REMOTE_DATABASE_URL" `
  -v "${PWD}:/backup" `
  postgres:16-alpine `
  sh -c 'pg_restore --verbose --clean --if-exists --no-owner --no-privileges --dbname "$REMOTE_DATABASE_URL" /backup/taskboard.dump'
```

Use `--clean --if-exists` only when restoring into a new or disposable database.

## 5. Configure The Web Service

Create a Docker-based web service in your hosting dashboard and connect it to the GitHub repository.

Typical settings:

- root directory: repository root;
- runtime: Docker;
- Dockerfile path: `Dockerfile`;
- start command: use the `CMD` from the Dockerfile unless your platform asks for a custom one;
- health check path: `/actuator/health`;
- auto deploy: optional, but convenient for pushes to `main` or `master`.

## 6. Environment Variables

Set these variables in the hosting dashboard:

```properties
PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:PORT/DATABASE
SPRING_DATASOURCE_USERNAME=USER
SPRING_DATASOURCE_PASSWORD=PASSWORD
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
FRONTEND_URL=https://YOUR-APP-URL
FRONTEND_ORIGINS=https://YOUR-APP-URL
JAVA_OPTS=-XX:MaxRAMPercentage=75.0
```

If your platform provides a deploy hook, store it in `DEPLOY_HOOK_URL`. If you want GitHub Actions to wait for the published app and check its readiness, store its public URL in `DEPLOYMENT_URL`.

## 7. GitHub CI/CD

The GitHub Actions workflow does:

- frontend build;
- backend tests;
- Docker image build;
- deployment trigger when `DEPLOY_HOOK_URL` is configured;
- healthcheck of the deployed app when `DEPLOYMENT_URL` is configured.

Optional repository variables or secrets:

```text
DEPLOY_HOOK_URL
DEPLOYMENT_URL
DEPLOY_WAIT_SECONDS
```

## 8. Healthcheck

The health endpoint is:

```text
/actuator/health
```

It is public and does not require JWT. It lets the hosting platform, Docker, and GitHub Actions check whether the application started correctly and can serve traffic.
