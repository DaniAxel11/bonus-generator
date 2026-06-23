# Kafka en Bonus Generator

Esta guia explica como se implemento Kafka en el proyecto y como probarlo paso por paso.

## Objetivo

Kafka se agrego para procesar solicitudes de analisis de commits de forma asincrona.

Antes:

```text
API -> CommitAnalysisService -> PostgreSQL -> IA -> Email
```

Ese flujo sigue disponible en:

```text
POST /v1/report/commits/analysis/manual
```

Ahora tambien existe un flujo asincrono:

```text
API -> Kafka topic -> Kafka consumer -> CommitAnalysisService -> PostgreSQL -> IA -> Email
```

Este flujo nuevo esta disponible en:

```text
POST /v1/report/commits/analysis/async
```

## Piezas agregadas

### 1. Dependencia Maven

Se agrego `spring-kafka` en `pom.xml`.

Spring Kafka permite usar:

- `KafkaTemplate` para publicar mensajes.
- `@KafkaListener` para consumir mensajes.
- Serializacion JSON para enviar objetos Java como eventos.

### 2. Configuracion

La configuracion principal esta en `src/main/resources/application.yaml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP_ID:bonus-generator}
      auto-offset-reset: ${KAFKA_AUTO_OFFSET_RESET:earliest}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

app:
  kafka:
    topics:
      commit-analysis-requested: ${KAFKA_TOPIC_COMMIT_ANALYSIS_REQUESTED:bonus.commit-analysis.requested}
```

Variables importantes:

- `KAFKA_BOOTSTRAP_SERVERS`: direccion del broker Kafka.
- `KAFKA_CONSUMER_GROUP_ID`: grupo del consumidor.
- `KAFKA_TOPIC_COMMIT_ANALYSIS_REQUESTED`: topico donde se publican solicitudes de analisis.

Ademas existe una configuracion Java explicita:

```text
src/main/java/com/truper/bonusgenerator/infrastructure/config/KafkaProducerConfig.java
src/main/java/com/truper/bonusgenerator/infrastructure/config/KafkaConsumerConfig.java
```

Estas clases registran beans tipados:

```text
KafkaTemplate<String, CommitAnalysisRequestedEvent>
ConsumerFactory<String, CommitAnalysisRequestedEvent>
kafkaListenerContainerFactory
```

Se agregaron para que Spring y el IDE puedan resolver sin ambiguedad el template usado por el producer y la fabrica usada por `@KafkaListener`.

### 3. Evento

Archivo:

```text
src/main/java/com/truper/bonusgenerator/infrastructure/kafka/event/CommitAnalysisRequestedEvent.java
```

Representa el mensaje que viaja por Kafka:

```json
{
  "startDate": "2026-06-01",
  "endDate": "2026-06-07",
  "source": "manual-api"
}
```

### 4. Producer

Archivo:

```text
src/main/java/com/truper/bonusgenerator/infrastructure/kafka/producer/CommitAnalysisEventProducer.java
```

Responsabilidad:

- Recibir un `CommitAnalysisRequestedEvent`.
- Publicarlo en el topico `bonus.commit-analysis.requested`.
- Registrar en logs el topico, particion, offset y llave del mensaje.

La llave usada es:

```text
startDate:endDate
```

Ejemplo:

```text
2026-06-01:2026-06-07
```

### 5. Consumer

Archivo:

```text
src/main/java/com/truper/bonusgenerator/infrastructure/kafka/consumer/CommitAnalysisEventConsumer.java
```

Responsabilidad:

- Escuchar el topico `bonus.commit-analysis.requested`.
- Recibir el evento.
- Ejecutar `commitAnalysisService.analyzeByDateRange(startDate, endDate)`.

Esto reutiliza la logica existente de analisis, IA y correo.

Los listeners se activan con `@EnableKafka` en:

```text
src/main/java/com/truper/bonusgenerator/BonusGeneratorApplication.java
```

Sin `@EnableKafka`, el producer puede publicar mensajes, pero el consumer group no se registra y `kafka-consumer-groups.sh --describe --group bonus-generator` puede mostrar que el grupo no existe.

### 6. Endpoint asincrono

Archivo:

```text
src/main/java/com/truper/bonusgenerator/controller/CommitController.java
```

Endpoint nuevo:

```text
POST /v1/report/commits/analysis/async
```

Este endpoint:

- Valida `startDate` y `endDate`.
- Publica un evento en Kafka.
- Responde `202 Accepted`.
- No espera a que termine Gemini ni el envio de correo.

Ejemplo de request:

```json
{
  "startDate": "2026-06-01",
  "endDate": "2026-06-07"
}
```

Ejemplo de response:

```json
{
  "startDate": "2026-06-01",
  "endDate": "2026-06-07",
  "topic": "bonus.commit-analysis.requested",
  "status": "queued"
}
```

## Levantar Kafka local

El proyecto incluye `docker-compose.kafka.yml` para levantar un broker Kafka local.

Ejecuta:

```bash
docker compose -f docker-compose.kafka.yml up -d
```

Si aparece este error:

```text
unknown shorthand flag: 'f' in -f
```

significa que tienes Docker Engine, pero no tienes instalado el plugin de Docker Compose.

En Ubuntu/WSL intenta instalarlo con:

```bash
sudo apt update
sudo apt install docker-compose-v2
```

Despues valida:

```bash
docker compose version
```

Si tu distribucion usa los paquetes oficiales de Docker, el paquete puede llamarse:

```bash
sudo apt install docker-compose-plugin
```

Verifica que el contenedor este arriba:

```bash
docker ps
```

### Alternativa sin Docker Compose

Si no quieres instalar Compose, puedes levantar Kafka directamente con `docker run`:

```bash
docker run -d --name bonus-generator-kafka \
  -p 9092:9092 \
  -p 9094:9094 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,DOCKER://:9094,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092,DOCKER://host.docker.internal:9094 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,DOCKER:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
  -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
  apache/kafka:3.9.0
```

## Ejecutar la aplicacion localmente

Si ejecutas la app directamente con Maven en tu maquina:

```bash
set KAFKA_BOOTSTRAP_SERVERS=localhost:9092
mvnw.cmd spring-boot:run
```

En PowerShell:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
.\mvnw.cmd spring-boot:run
```

## Ejecutar la aplicacion en Docker

Si la app corre dentro de Docker y Kafka esta publicado en el host:

```bash
docker run --name bonus-generator -p 8084:8084 --env-file enviroment.env -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9094 bonus-generator:1.0
```

## Probar el flujo asincrono

Con la app corriendo:

```bash
curl -u bonus-admin:change-me -X POST http://localhost:8084/v1/report/commits/analysis/async \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"2026-06-01\",\"endDate\":\"2026-06-07\"}"
```

Resultado esperado:

```text
HTTP 202 Accepted
```

Despues de eso:

- El producer publica el evento en Kafka.
- El consumer recibe el evento.
- Se ejecuta el analisis.
- Se envia el correo.

## Como inspeccionar mensajes

Entrar al contenedor Kafka:

```bash
docker exec -it bonus-generator-kafka bash
```

Listar topicos:

```bash
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Leer mensajes desde el inicio:

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic bonus.commit-analysis.requested \
  --from-beginning
```

## Notas importantes

- Kafka no reemplaza PostgreSQL. Kafka transporta eventos; PostgreSQL sigue siendo la fuente persistente de commits.
- El endpoint sincronico sigue existiendo para comparar comportamiento.
- Si Kafka no esta disponible, el endpoint asincrono fallara al publicar el evento.
- El consumer procesa en segundo plano dentro de la misma aplicacion.
- Si en el futuro hay varias replicas de la app, todas podran competir dentro del mismo `group-id`; Kafka entregara cada mensaje a una sola instancia del grupo.

## Siguiente mejora recomendada

Agregar manejo explicito de errores para el consumer:

- Reintentos.
- Dead letter topic.
- Persistencia del estado del analisis.
- Endpoint para consultar si un analisis asincrono ya termino.
