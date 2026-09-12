# Informe técnico — AgroAlert PY

## 1. Problema y requisitos

AgroAlert PY integra productores de datos de sensores agrícolas con persistencia, clasificación de alertas y múltiples consumidores. El objetivo es evitar una integración directa y rígida entre sensores, base de datos y canales de notificación.

### Sistemas participantes
1. Productores/sensores.
2. Apache ActiveMQ Artemis.
3. Apache Camel.
4. PostgreSQL.
5. Consumidores de alertas.
6. Suscriptores de monitoreo y auditoría.
7. Cliente HTTP para consulta síncrona de estado.

## 2. Diseño y arquitectura

La entrada principal es la cola `agroalert.eventos`. Camel consume los mensajes, transforma JSON a `EventoSensor`, controla duplicados y persiste cada evento. A continuación aplica filtrado y enrutamiento por contenido.

Las alertas usan colas Point-to-Point, un Topic Publish-Subscribe, Wire Tap para auditoría y Recipient List para distribución multicanal.

Los errores técnicos son derivados a `agroalert.dlq`.

## 3. Flujo principal

1. Un productor envía JSON a `agroalert.eventos`.
2. Camel deserializa el mensaje.
3. El Idempotent Receiver evalúa `event_id`.
4. El evento se almacena en PostgreSQL.
5. Message Filter identifica lecturas extremas.
6. Content-Based Router decide CRITICA, ALTA o NORMAL.
7. Las alertas se distribuyen por colas y Topic.
8. Wire Tap conserva una copia en auditoría.
9. Recipient List selecciona email y/o SMS.
10. Si existe una excepción no recuperable, el mensaje llega a DLQ.

## 4. Patrones EIP y justificación

| Patrón | Problema que resuelve | Aplicación | Justificación |
|---|---|---|---|
| Message Channel | Acoplamiento entre productor y consumidor | Colas JMS | Permite comunicación desacoplada |
| Point-to-Point Channel | Entrega a un consumidor lógico | `agroalert.eventos`, alertas | Adecuado para trabajo distribuido por colas |
| Publish-Subscribe Channel | Varios consumidores requieren la misma alerta | `agroalert.notificaciones` | Monitoreo y auditoría reciben el mismo evento |
| Message Translator | Formatos diferentes | JSON <-> `EventoSensor` | La lógica trabaja con un modelo Java |
| Content-Based Router | Diferentes destinos según contenido | `choice/when` | Clasifica normal, alta y crítica |
| Message Filter | Detectar subconjunto relevante | valores <10 o >45 | Separa eventos extremos |
| Idempotent Receiver | Mensajes repetidos | `event_id` | Evita reprocesamiento durante la ejecución |
| Wire Tap | Auditoría sin bloquear flujo | `agroalert.auditoria` | Copia asíncrona independiente |
| Recipient List | Destinos variables | email/SMS | Distribución dinámica por severidad |
| Dead Letter Channel | Fallos no recuperables | `agroalert.dlq` | Conserva mensajes fallidos para diagnóstico |

## 5. Transformación

Entrada:
```json
{
  "event_id": "EVT-014",
  "sensor_id": "SEN-18",
  "parcela_id": "PAR-31",
  "tipo": "HUMEDAD_SUELO",
  "valor": 8.5,
  "unidad": "%",
  "timestamp": "2026-09-11T23:40:00"
}
```

Camel utiliza Jackson para convertir el JSON a `EventoSensor`. Antes de publicar por JMS se vuelve a serializar a JSON.

## 6. Mensajería

Artemis utiliza:
- ANYCAST para queues.
- MULTICAST para el topic `agroalert.notificaciones`.

Esto permite combinar Point-to-Point y Publish-Subscribe dentro de la misma solución.

## 7. Persistencia

PostgreSQL almacena:
- eventos recibidos en `sensor_event`;
- alertas generadas en `alert`.

La columna `event_id` identifica el evento de negocio.

## 8. Comunicación síncrona y asíncrona

### Asíncrona
El flujo principal usa JMS/Artemis.

### Síncrona
`GET http://localhost:8085/agroalert/estado` responde inmediatamente el estado del servicio.

## 9. Manejo de errores

La configuración global `onException(Exception.class)`:
- realiza 2 reintentos;
- espera 1000 ms entre reintentos;
- usa el mensaje original;
- marca el error como manejado;
- envía el mensaje a `agroalert.dlq`.

## 10. Casos de prueba demostrados

| Caso | Evento | Resultado |
|---|---|---|
| Error de tipo | EVT-009 | DLQ |
| Evento normal | EVT-010 | `eventos.normales` |
| Publish-Subscribe | EVT-011 | 2 suscriptores |
| Wire Tap | EVT-012 | `auditoria` |
| Filter | EVT-013 | `eventos.extremos` |
| Recipient List | EVT-014 | email + SMS |

## 11. Evaluación contra la consigna

- Problema y requisitos: CUBIERTO.
- Diseño y documentación: CUBIERTO con README + diagrama + informe.
- Patrones EIP: CUBIERTO.
- Enrutamiento: CUBIERTO.
- Transformación: CUBIERTO.
- Mensajería: CUBIERTO.
- Apache Camel: CUBIERTO.
- Persistencia: CUBIERTO.
- Calidad de implementación: CUBIERTO, con observaciones de mejoras futuras.
- Pruebas: CUBIERTO con evidencia visual de flujo normal y excepcional.

## 12. Observación antes de entregar

La consigna recuperada indica grupos de hasta 2 integrantes. El proyecto fue desarrollado con tres nombres informados: Gustavo Acosta, Abraham Ledezma y Saulo Caceres. Debe confirmarse con el docente si el grupo de tres está autorizado.
