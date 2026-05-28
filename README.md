# Bonus Generator

Servicio backend para registrar commits y preparar reportes relacionados con la generacion de bonos. La aplicacion expone una API REST construida con Spring Boot y persiste la informacion de commits en PostgreSQL.

## Tecnologias

- Java 21
- Spring Boot 4.0.5
- Spring Web / Spring MVC para endpoints REST
- Spring Data JPA e Hibernate para persistencia
- PostgreSQL como base de datos
- Spring WebFlux WebClient para integraciones HTTP externas
- Springdoc OpenAPI / Swagger UI para documentacion de la API
- Lombok para reducir codigo repetitivo en DTOs, servicios y entidades
- MapStruct para mapeo entre entidades y DTOs
- Maven como gestor de dependencias y herramienta de build
- Docker para empaquetado de la aplicacion
- Kubernetes para despliegue mediante manifiestos en `k8s/`

## Estructura principal

- `src/main/java/com/truper/bonusgenerator/controller`: controladores REST.
- `src/main/java/com/truper/bonusgenerator/service`: logica de negocio.
- `src/main/java/com/truper/bonusgenerator/repository`: repositorios JPA.
- `src/main/java/com/truper/bonusgenerator/model`: entidades, DTOs y mappers.
- `src/main/java/com/truper/bonusgenerator/infrastructure`: configuracion e integraciones externas.
- `src/main/resources/application.yaml`: configuracion de Spring Boot.
- `k8s/`: manifiestos de Kubernetes.

## Configuracion

La aplicacion utiliza variables de entorno para configurar la base de datos, el puerto y el cliente de IA:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bonus_generator
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
AI_API_URL=https://generativelanguage.googleapis.com/v1beta
AI_API_KEY=your-api-key
AI_MODEL=gemini-2.0-flash
PORT=8084
COMMIT_ANALYSIS_CRON=0 0 8 * * MON
COMMIT_ANALYSIS_ZONE=America/Mexico_City
```

Para Docker local, el archivo de variables que se esta usando es `enviroment.env`.

El cliente de IA usa Gemini API `generateContent`. La API key se envia como `x-goog-api-key: ${AI_API_KEY}` y el modelo se toma de `AI_MODEL`.

## Automatizacion

La aplicacion incluye un scheduler de Spring que ejecuta el analisis automatico de commits cada lunes.

Por defecto corre los lunes a las 08:00 en la zona `America/Mexico_City`:

```bash
COMMIT_ANALYSIS_CRON=0 0 8 * * MON
COMMIT_ANALYSIS_ZONE=America/Mexico_City
```

El job consulta la ultima semana completa de commits, serializa la respuesta y llama al cliente de IA para obtener un arreglo con 3 strings.

## Ejecucion local

Compilar el proyecto:

```bash
./mvnw clean package
```

Ejecutar la aplicacion:

```bash
./mvnw spring-boot:run
```

En Windows tambien se puede usar:

```bash
mvnw.cmd spring-boot:run
```

## Docker

El proyecto incluye un `Dockerfile` multi-stage. Primero descarga dependencias Maven, despues compila la aplicacion y finalmente genera una imagen ligera con Eclipse Temurin 21.

Construir la imagen manualmente:

```bash
docker build -t bonus-generator:1.0 .
```

Ejecutar el contenedor:

```bash
docker run --name bonus-generator -p 8084:8084 --env-file enviroment.env -e PORT=8084 bonus-generator:1.0
```

Si ya existe un contenedor con el mismo nombre, se puede reemplazar con:

```bash
docker rm -f bonus-generator
docker run --name bonus-generator -p 8084:8084 --env-file enviroment.env -e PORT=8084 bonus-generator:1.0
```

Tambien se puede usar el script externo configurado en:

```bash
~/Documents/scripts/docker/run-docker.sh
```

Ese script debe cambiar al directorio del proyecto antes de construir la imagen:

```bash
/mnt/c/Users/dagonzalezm/Documents/Dani/bonus-generator
```

Ejecutarlo:

```bash
cd ~/Documents/scripts/docker
./run-docker.sh
```

## Kubernetes

Los manifiestos se encuentran en `k8s/`:

```bash
kubectl apply -f k8s/
```

El deployment actual usa la imagen local `bonus-generator:1.0` con `imagePullPolicy: Never`.

El `Deployment` expone el contenedor en el puerto `8084` y el `Service` redirige al mismo `targetPort`.

## Endpoints

- `POST /v1/report/commits/insert-commit`: registra un commit en base de datos.
- `GET /v1/report/commits/current-month/weeks`: consulta los commits de la ultima semana completa.
- `GET /actuator/health`: health check provisto por Spring Boot Actuator.

El endpoint semanal calcula semanas completas de domingo a sabado y regresa la ultima semana cerrada, no la semana en curso. Por ejemplo, si se consulta el `2026-05-28`, regresa el rango `2026-05-17` a `2026-05-23`; si se consulta el lunes `2026-06-01`, regresa el rango `2026-05-24` a `2026-05-30`.

La documentacion OpenAPI queda disponible mediante Springdoc cuando la aplicacion esta en ejecucion:

```text
http://localhost:8084/swagger-ui/index.html
```
