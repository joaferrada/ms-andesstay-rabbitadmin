# ms-andesstay-rabbitadmin

API REST para administrar RabbitMQ (puerto **8084**): colas, exchanges, bindings y publicacion de mensajes de prueba. Toda la logica esta en `service/RabbitAdminService` (usa `RabbitAdmin` y `RabbitTemplate` de Spring AMQP); los controllers solo validan y delegan.

## Ejecutar

```powershell
docker compose -f ..\infra\mq\compose.yml up -d
.\mvnw.cmd spring-boot:run                                   # exige JWT con rol Admin
$env:ANDESSTAY_SECURITY_ENABLED="false"; .\mvnw.cmd spring-boot:run   # local / Postman sin token
```

| Variable | Default | Uso |
|---|---|---|
| `ANDESSTAY_SECURITY_ENABLED` | `true` | `false` abre la API (solo pruebas locales) |
| `RABBITMQ_ADDRESSES` | `localhost:5672` | nodos AMQP |
| `RABBITMQ_MANAGEMENT_URL` | `http://localhost:15672` | Management API (para listar colas) |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `guest` / `guest` | credenciales |

**Seguridad:** Resource Server JWT de Microsoft Entra ID; `JwtAuthConverter` (el mismo del BFF) mapea el claim `roles` a `ROLE_*`, y `/api/admin/**` exige `ROLE_Admin`. Sin token: 401. Con otro rol: 403.

## Endpoints

| Metodo | Ruta | Exito | Errores |
|---|---|---|---|
| GET | `/api/admin/queues` | 200 lista (nombre, mensajes, consumidores, argumentos) | 503 |
| GET | `/api/admin/queues/{name}` | 200 | 400, 404 |
| POST | `/api/admin/queues` | 201 + `Location` | 400, 404 (DLX inexistente), 409 |
| DELETE | `/api/admin/queues/{name}` | 204 | 400, 404 |
| DELETE | `/api/admin/queues/{name}/messages` | 200 (purga) | 404 |
| POST | `/api/admin/exchanges` | 201 + `Location` | 400, 409 |
| DELETE | `/api/admin/exchanges/{name}` | 204 | 400, 404 |
| POST | `/api/admin/bindings` | 201 | 400, 404 |
| DELETE | `/api/admin/bindings?exchange=&queue=&routingKey=` | 204 | 400, 404 |
| POST | `/api/admin/messages` | 202 (`eventId`, `confirmed`) | 400, 404, 422 (no enrutable) |

Si RabbitMQ no responde: 503. Formato de error:

```json
{"timestamp":"2026-10-18T22:00:00Z","status":400,"error":"Bad Request","message":"Validacion fallida",
 "path":"/api/admin/queues","details":[{"field":"name","message":"el prefijo 'amq.' esta reservado por RabbitMQ"}]}
```

### Validaciones (jakarta.validation)

- Nombres de cola/exchange: `@NotBlank` + `@AmqpName` = `^[a-zA-Z0-9._-]{3,100}$` y sin prefijo `amq.`.
- Tipo de exchange: enum `direct | topic | fanout | headers` (cualquier otro: 400 con los valores permitidos).
- `ttlMs` y `maxLength`: `@Positive`. `deadLetterRoutingKey` exige `deadLetterExchange`.
- Routing key: `@RoutingKey` (letras, numeros, `. _ -` y comodines `* #`, maximo 255).
- Mensajes: exactamente uno de `payload` o `rawBody`; `eventId` opcional con formato UUID.

## Ejemplos (curl, seguridad desactivada)

```bash
# Crear exchange, cola con DLX y binding
curl -i -X POST localhost:8084/api/admin/exchanges -H "Content-Type: application/json" \
  -d '{"name":"demo.topic","type":"topic","durable":true}'
curl -i -X POST localhost:8084/api/admin/queues -H "Content-Type: application/json" \
  -d '{"name":"q.demo","ttlMs":60000,"maxLength":1000,"deadLetterExchange":"cmd.dead.dlx","deadLetterRoutingKey":"email.dead"}'
curl -i -X POST localhost:8084/api/admin/bindings -H "Content-Type: application/json" \
  -d '{"exchange":"demo.topic","queue":"q.demo","routingKey":"demo.#"}'

# Listar / consultar
curl localhost:8084/api/admin/queues
curl localhost:8084/api/admin/queues/q.cmd.email.dlq

# Errores esperados
curl -i -X POST localhost:8084/api/admin/queues -H "Content-Type: application/json" -d '{"name":"amq.x"}'   # 400
curl -i -X POST localhost:8084/api/admin/queues -H "Content-Type: application/json" -d '{"name":"q.demo"}'  # 409
curl -i -X DELETE localhost:8084/api/admin/queues/q.noexiste                                                # 404

# Mensaje malformado -> q.cmd.email.dlq (con ms-andesstay-notify corriendo)
curl -i -X POST localhost:8084/api/admin/messages -H "Content-Type: application/json" \
  -d '{"exchange":"cmd.direct","routingKey":"email.send","rawBody":"{esto no es json"}'

# Limpieza
curl -i -X DELETE "localhost:8084/api/admin/bindings?exchange=demo.topic&queue=q.demo&routingKey=demo.%23"
curl -i -X DELETE localhost:8084/api/admin/queues/q.demo
curl -i -X DELETE localhost:8084/api/admin/exchanges/demo.topic
```

Con seguridad activa se agrega `-H "Authorization: Bearer $TOKEN"` (token de Entra ID de un usuario con rol Admin).

## Tests

```powershell
.\mvnw.cmd clean package
```

- `AdminDtoValidationTest`: nombres, `amq.`, enum de tipo, TTL/max-length, payload/rawBody.
- `AdminApiHttpStatusTest`: 201/204/400/404/409/422 con el servicio mockeado.
- `AdminApiSecurityTest`: 401 sin token, 403 con otro rol, 200 con `ROLE_Admin`.
