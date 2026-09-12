# AgroAlert PY

## Trabajo Autónomo de Investigación 1 (P1)
**Implementación de patrones de integración empresarial en arquitecturas modernas basadas en mensajería**

AgroAlert PY es una solución de integración para monitoreo de sensores agrícolas. Recibe eventos de sensores mediante ActiveMQ Artemis, transforma y valida los mensajes con Apache Camel, persiste la información en PostgreSQL, clasifica eventos normales y alertas, distribuye notificaciones a diferentes canales y maneja errores mediante una cola DLQ.

> **IMPORTANTE SOBRE LOS INTEGRANTES:** durante el desarrollo se informaron los nombres **Gustavo Acosta, Abraham Ledezma y Saulo Caceres**. Sin embargo, la consigna recuperada indica que los grupos pueden ser de **hasta 2 integrantes**. Antes de la entrega, se debe confirmar con el profesor si existe una excepción. No conviene presentar tres integrantes sin esa confirmación.

## Integrantes informados
- Gustavo Acosta
- Abraham Ledezma
- Saulo Caceres

## 1. Problema de negocio

En un entorno agrícola, diferentes sensores generan lecturas de temperatura y humedad de suelo. El sistema necesita recibir eventos sin acoplar directamente los sensores con los servicios consumidores, guardar el historial, detectar condiciones críticas, distribuir notificaciones y conservar mensajes problemáticos para análisis posterior.

AgroAlert PY resuelve este problema usando una arquitectura distribuida orientada a mensajería.

## 2. Tecnologías

- Java 21
- Apache Camel 4.14.2
- Apache ActiveMQ Artemis
- JMS
- Gradle Wrapper 9.7.1
- JBang 0.141.0
- PostgreSQL 17
- Docker / Docker Compose
- Jetty para endpoint HTTP síncrono

## 3. Arquitectura

Ver:
- `docs/diagrama_arquitectura.png`
- `docs/diagrama_arquitectura.mmd`

Flujo principal simplificado:

```text
Sensores / Productor
        |
        v
Artemis: agroalert.eventos
        |
        v
Apache Camel
  |-- Message Translator (JSON -> EventoSensor)
  |-- Idempotent Receiver
  |-- Persistencia PostgreSQL
  |-- Message Filter (eventos extremos)
  |-- Content-Based Router
        |
        +--> CRITICA -> Recipient List -> email + sms
        |                Wire Tap -> auditoria
        |                queue -> alertas.criticas
        |                topic -> notificaciones
        |
        +--> ALTA    -> Recipient List -> email
        |                Wire Tap -> auditoria
        |                queue -> alertas
        |                topic -> notificaciones
        |
        +--> NORMAL  -> eventos.normales

Error de procesamiento -> agroalert.dlq
HTTP síncrono -> GET /agroalert/estado
```

## 4. Requisitos previos

Verificar:

```powershell
java -version
.\gradlew.bat -v
docker --version
docker compose version
jbang --version
```

Versiones verificadas durante el desarrollo:
- Java: 21.0.12 LTS
- Gradle: 9.7.1
- JBang: 0.141.0

## 5. Configuración

### ActiveMQ Artemis
- JMS: `tcp://localhost:61616`
- Consola web: `http://localhost:8161/console/artemis`
- Usuario de laboratorio: `admin`
- Contraseña de laboratorio: `admin123`

### PostgreSQL
- Host: `localhost`
- Puerto host: `5433`
- Puerto contenedor: `5432`
- Base de datos: `agroalert`
- Usuario: `agroalert`
- Contraseña de laboratorio: `agroalert123`

> Para una entrega pública en GitHub se recomienda mover credenciales a variables de entorno antes de publicar el repositorio.

## 6. Ejecución

Desde la raíz del proyecto:

```powershell
docker compose up -d
docker compose ps
.\gradlew.bat build
.\gradlew.bat run
```

Para detener la aplicación:

```text
Ctrl + C
```

Para detener infraestructura:

```powershell
docker compose down
```

## 7. Endpoint HTTP síncrono

```http
GET http://localhost:8085/agroalert/estado
```

Respuesta:

```json
{
  "servicio": "AgroAlert PY",
  "estado": "ACTIVO",
  "comunicacion": "SINCRONA"
}
```

## 8. Canales JMS

| Canal | Tipo | Propósito |
|---|---|---|
| `agroalert.eventos` | Queue / ANYCAST | Entrada principal de eventos |
| `agroalert.alertas.criticas` | Queue / ANYCAST | Alertas críticas |
| `agroalert.alertas` | Queue / ANYCAST | Alertas altas |
| `agroalert.eventos.normales` | Queue / ANYCAST | Eventos sin alerta |
| `agroalert.eventos.extremos` | Queue / ANYCAST | Eventos detectados por Message Filter |
| `agroalert.auditoria` | Queue / ANYCAST | Copia asíncrona mediante Wire Tap |
| `agroalert.notificacion.email` | Queue / ANYCAST | Destino dinámico de Recipient List |
| `agroalert.notificacion.sms` | Queue / ANYCAST | Destino dinámico de Recipient List |
| `agroalert.dlq` | Queue / ANYCAST | Mensajes con error de procesamiento |
| `agroalert.notificaciones` | Topic / MULTICAST | Publish-Subscribe para monitoreo y auditoría |

## 9. Reglas de negocio

- `HUMEDAD_SUELO < 20` -> alerta **CRITICA**.
- `TEMPERATURA > 35` -> alerta **ALTA**.
- Otro valor -> evento **NORMAL**.
- `valor < 10` o `valor > 45` -> además se considera **evento extremo**.
- Evento duplicado con el mismo `event_id` -> Idempotent Receiver evita reprocesamiento durante la ejecución.
- Error de deserialización o procesamiento -> 2 reintentos y luego `agroalert.dlq`.

## 10. Patrones EIP implementados

### Message Channel / Point-to-Point Channel
Las colas JMS desacoplan productores y consumidores. Cada mensaje de una queue se entrega a un consumidor.

### Publish-Subscribe Channel
Las alertas se publican en `agroalert.notificaciones`. Dos consumidores (`suscriptor-monitoreo` y `suscriptor-auditoria`) reciben el mismo mensaje.

### Message Translator
Los mensajes JSON se transforman a `EventoSensor` usando Jackson dentro de Camel y se vuelven a serializar como JSON para su publicación JMS.

### Content-Based Router
`choice()` y `when()` clasifican los eventos por tipo y valor en CRITICA, ALTA o NORMAL.

### Message Filter
Los valores extremos (`< 10` o `> 45`) se identifican y se envían a `agroalert.eventos.extremos`.

### Idempotent Receiver
Evita reprocesar el mismo `event_id` durante la ejecución usando `MemoryIdempotentRepository`. La restricción `UNIQUE` de PostgreSQL aporta una segunda protección para `sensor_event`.

### Wire Tap
Las alertas envían una copia a `agroalert.auditoria` sin interrumpir el flujo principal.

### Recipient List
Los destinos de notificación se determinan dinámicamente:
- crítica -> email + SMS
- alta -> email

### Dead Letter Channel / manejo de errores
`onException` reintenta dos veces y, si el error persiste, envía el mensaje original a `agroalert.dlq`.

## 11. Persistencia

Script:
```text
database/init.sql
```

Tablas principales:
- `sensor_event`
- `alert`

`event_id` se utiliza para trazabilidad del evento a través del procesamiento.

## 12. Pruebas manuales reproducibles

### Evento normal
Usar `ejemplos/evento_normal.json`.
Resultado:
- se persiste en `sensor_event`
- no crea registro en `alert`
- se publica en `agroalert.eventos.normales`

### Alerta alta
Usar `ejemplos/alerta_alta.json`.
Resultado:
- crea alerta ALTA
- se publica en `agroalert.alertas`
- notificación email
- Wire Tap de auditoría
- Publish-Subscribe

### Alerta crítica/extrema
Usar `ejemplos/alerta_critica_extrema.json`.
Resultado:
- crea alerta CRITICA
- pasa por Message Filter
- email + SMS por Recipient List
- Wire Tap de auditoría
- Publish-Subscribe

### Error / DLQ
Usar `ejemplos/error_dlq.json`.
Resultado:
- falla la transformación de `valor`
- Camel aplica reintentos
- termina en `agroalert.dlq`

## 13. Evidencias

Las capturas validadas durante el desarrollo se encuentran en `evidencias/`.

Consultar `evidencias/README_EVIDENCIAS.md` para la relación entre cada captura y el requisito demostrado.

## 14. Estructura sugerida del repositorio

```text
agroalert-py/
├── README.md
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── docker-compose.yml
├── database/
│   └── init.sql
├── docs/
│   ├── diagrama_arquitectura.png
│   ├── diagrama_arquitectura.mmd
│   └── INFORME_TECNICO.md
├── evidencias/
│   ├── README_EVIDENCIAS.md
│   └── *.png
├── ejemplos/
│   └── *.json
└── src/
    └── main/java/py/edu/agroalert/
        ├── Main.java
        ├── config/
        ├── model/
        └── routes/
```

## 15. Limitaciones y mejoras futuras

- `MemoryIdempotentRepository` se reinicia cuando se reinicia la aplicación. Una mejora sería usar un repositorio idempotente persistente.
- Las credenciales actuales son de laboratorio; deben externalizarse para ambientes reales.
- Las colas email y SMS representan canales de integración; no realizan todavía el envío real a proveedores externos.
- Se podría incorporar métricas y observabilidad.
- Se podría configurar TLS y autenticación para producción.

## 16. Referencias

Hohpe, G., & Woolf, B. (2003). *Enterprise Integration Patterns: Designing, Building, and Deploying Messaging Solutions*. Addison-Wesley.

Apache Camel. *Enterprise Integration Patterns*.

Apache ActiveMQ Artemis. *Documentation*.
