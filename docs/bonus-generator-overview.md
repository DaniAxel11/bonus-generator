# Bonus Generator

## Proposito

Bonus Generator es un backend para registrar commits, consultarlos por periodos semanales y generar reportes de analisis relacionados con bonos.

La aplicacion toma commits almacenados en PostgreSQL, los envia a un proveedor de IA para generar un analisis y posteriormente manda el resultado por correo electronico.

## Que Problema Resuelve

El proyecto automatiza el proceso de revisar commits de trabajo y convertirlos en un reporte entendible para seguimiento, evaluacion o soporte de bonos.

En lugar de revisar manualmente commits por repositorio, rama o rango de fechas, la aplicacion:

- Guarda commits en base de datos.
- Consulta commits por semana o rango de fechas.
- Genera analisis con IA.
- Envia reportes por correo.
- Permite ejecutar el analisis de forma manual, programada o asincrona con Kafka.

## Tecnologias Principales

- Java 21.
- Spring Boot 4.
- Spring Web MVC para API REST.
- Spring Data JPA e Hibernate para persistencia.
- PostgreSQL como base de datos.
- Spring Mail para envio de correos.
- WebClient para consumir la API externa de IA.
- Spring Kafka para procesamiento asincrono.
- Spring Security con Basic Auth.
- Springdoc OpenAPI para Swagger UI.
- Maven para build.
- Docker para empaquetado y ejecucion.

## Modulos Principales

### API REST

Los controladores estan en:

```text
src/main/java/com/truper/bonusgenerator/controller
```

Responsabilidad:

- Recibir peticiones HTTP.
- Validar datos basicos.
- Delegar la logica a servicios.
- Responder al cliente con DTOs.

### Servicios De Negocio

Los servicios estan en:

```text
src/main/java/com/truper/bonusgenerator/service
```

Responsabilidad:

- Registrar commits.
- Consultar commits por rango de fechas.
- Calcular la ultima semana completa.
- Generar analisis con IA.
- Enviar correos.

### Persistencia

La capa de persistencia esta en:

```text
src/main/java/com/truper/bonusgenerator/repository
src/main/java/com/truper/bonusgenerator/model/entity
```

La entidad principal es `Commit`, persistida en la tabla:

```text
commits
```

Campos principales:

- `repo`
- `branch`
- `hash`
- `author`
- `message`
- `createdAt`

### Kafka

La integracion Kafka esta en:

```text
src/main/java/com/truper/bonusgenerator/infrastructure/kafka
```

Responsabilidad:

- Publicar solicitudes de analisis en un topico.
- Consumir solicitudes pendientes.
- Ejecutar analisis en segundo plano.

El topico principal es:

```text
bonus.commit-analysis.requested
```

### Scheduler

El scheduler esta en:

```text
src/main/java/com/truper/bonusgenerator/infrastructure/scheduler/CommitAnalysisScheduler.java
```

Responsabilidad:

- Ejecutar automaticamente el analisis de la ultima semana completa.
- Usar una expresion cron configurable.
- Enviar el reporte por correo.

## Flujos Principales

### 1. Registro De Commit

Endpoint:

```text
POST /v1/report/commits/insert-commit
```

Flujo:

```text
Cliente -> API REST -> CommitService -> PostgreSQL
```

Este endpoint recibe un `CommitDto` y lo guarda en la base de datos.

### 2. Consulta De Semana Actual Cerrada

Endpoint:

```text
GET /v1/report/commits/current-month/weeks
```

Flujo:

```text
Cliente -> API REST -> CommitService -> PostgreSQL
```

La aplicacion calcula la ultima semana completa de domingo a sabado y devuelve los commits encontrados.

Ejemplo:

- Si hoy es jueves, no analiza la semana en curso.
- Toma la semana completa anterior.

### 3. Analisis Manual Sincronico

Endpoint:

```text
POST /v1/report/commits/analysis/manual
```

Flujo:

```text
Cliente -> API REST -> CommitAnalysisService -> PostgreSQL -> IA -> Email
```

Este endpoint:

- Recibe `startDate` y `endDate`.
- Consulta commits en ese rango.
- Genera el analisis con IA.
- Envia el resultado por correo.
- Responde cuando todo el proceso termina.

### 4. Analisis Manual Asincrono Con Kafka

Endpoint:

```text
POST /v1/report/commits/analysis/async
```

Flujo:

```text
Cliente -> API REST -> Kafka -> Kafka Consumer -> CommitAnalysisService -> PostgreSQL -> IA -> Email
```

Este endpoint:

- Recibe `startDate` y `endDate`.
- Publica un evento en Kafka.
- Responde `202 Accepted`.
- No espera la respuesta de IA ni el envio de correo.

El consumer procesa el evento en segundo plano.

### 5. Analisis Automatico Programado

Componente:

```text
CommitAnalysisScheduler
```

Flujo:

```text
Scheduler -> CommitAnalysisService -> PostgreSQL -> IA -> Email
```

La frecuencia se configura con:

```text
COMMIT_ANALYSIS_CRON
COMMIT_ANALYSIS_ZONE
```

Por defecto se ejecuta los lunes por la manana usando la zona horaria configurada.

### 6. Prueba De Correo

Endpoint:

```text
POST /v1/report/email/test
```

Flujo:

```text
Cliente -> API REST -> EmailService -> SMTP
```

Sirve para validar que la configuracion SMTP funciona antes de ejecutar reportes reales.

## Integracion Con IA

La aplicacion usa un cliente HTTP para consumir una API compatible con Gemini `generateContent`.

Variables principales:

```text
AI_API_URL
AI_API_KEY
AI_MODEL
AI_FALLBACK_MODEL
```

Comportamiento:

- Se serializan los commits consultados.
- Se envian a la IA.
- Se espera una respuesta con analisis.
- Si el modelo principal responde limite de cuota, se puede intentar con el modelo fallback.

## Correo Electronico

El envio de reportes usa SMTP mediante Spring Mail.

Variables principales:

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
MAIL_TO
```

El correo contiene el analisis generado y metricas de uso de IA.

## Seguridad

Los endpoints bajo:

```text
/v1/report/**
```

estan protegidos con Basic Auth.

Variables:

```text
SECURITY_USERNAME
SECURITY_PASSWORD
```

Quedan publicos:

- `/actuator/health`
- Swagger UI
- `/v3/api-docs/**`

## Docker

El proyecto incluye un `Dockerfile` multi-stage.

Flujo del build:

```text
Descargar dependencias Maven -> Compilar app -> Crear imagen runtime con Java 21
```

La imagen esperada es:

```text
bonus-generator:1.0
```

La aplicacion usa variables de entorno desde:

```text
enviroment.env
```

## Kafka En El Proyecto

Kafka se usa para desacoplar la solicitud de analisis del procesamiento real.

Ventajas:

- El endpoint asincrono responde rapido.
- Si hay mensajes pendientes, Kafka los conserva.
- Si la app se apaga, al volver a levantar puede consumir mensajes pendientes segun los offsets del grupo.
- Permite escalar consumidores en el futuro.

Consumer group:

```text
bonus-generator
```

Topico:

```text
bonus.commit-analysis.requested
```

Documento tecnico:

```text
docs/kafka.md
```

## Endpoints

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/v1/report/commits/insert-commit` | Registra un commit en PostgreSQL. |
| `GET` | `/v1/report/commits/current-month/weeks` | Consulta commits de la ultima semana completa. |
| `POST` | `/v1/report/commits/analysis/manual` | Genera analisis manual de forma sincronica. |
| `POST` | `/v1/report/commits/analysis/async` | Encola el analisis en Kafka y responde `202 Accepted`. |
| `POST` | `/v1/report/email/test` | Envia un correo de prueba. |
| `GET` | `/actuator/health` | Health check de la aplicacion. |

## Variables De Entorno Clave

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

AI_API_URL
AI_API_KEY
AI_MODEL
AI_FALLBACK_MODEL

MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
MAIL_TO

KAFKA_BOOTSTRAP_SERVERS
KAFKA_CONSUMER_GROUP_ID
KAFKA_TOPIC_COMMIT_ANALYSIS_REQUESTED

SECURITY_USERNAME
SECURITY_PASSWORD

PORT
```

## Resumen De Arquitectura

```text
Cliente HTTP
   |
   v
Spring Boot API
   |
   +--> PostgreSQL
   |
   +--> Gemini API
   |
   +--> SMTP
   |
   +--> Kafka Producer
             |
             v
          Kafka Topic
             |
             v
        Kafka Consumer
             |
             v
      CommitAnalysisService
```

## Estado Actual

El proyecto soporta:

- Registro de commits.
- Consulta semanal de commits.
- Analisis manual sincronico.
- Analisis manual asincrono con Kafka.
- Analisis automatico programado.
- Envio de correos.
- Seguridad con Basic Auth.
- Ejecucion en Docker.

## Mejoras Futuras Recomendadas

- Agregar retries y dead letter topic para Kafka.
- Registrar el estado de cada analisis asincrono en base de datos.
- Crear un endpoint para consultar el estado de un analisis encolado.
- Hacer idempotente el procesamiento por hash o rango de fechas.
- Cambiar la serializacion de fechas Kafka a formato ISO `yyyy-MM-dd`.
- Agregar pruebas de integracion para Kafka, correo e IA.
