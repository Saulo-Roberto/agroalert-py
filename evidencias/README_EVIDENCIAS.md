# Índice de evidencias

| Nº | Archivo | Qué demuestra |
|---:|---|---|
| 01 | `01_dlq_terminal_evt009.png` | Error de deserialización y envío a DLQ |
| 02 | `02_dlq_artemis.png` | Cola `agroalert.dlq` con mensaje |
| 03 | `03_evento_normal_terminal_evt010.png` | Procesamiento de evento normal |
| 04 | `04_evento_normal_artemis.png` | Cola `agroalert.eventos.normales` |
| 05 | `05_publish_subscribe_terminal_evt011.png` | Dos suscriptores reciben EVT-011 |
| 06 | `06_publish_subscribe_artemis.png` | Suscripciones MULTICAST en Artemis |
| 07 | `07_http_sincrono_navegador.png` | Respuesta del endpoint HTTP síncrono |
| 08 | `08_http_sincrono_terminal.png` | Ruta Jetty/Camel y consulta HTTP recibida |
| 09 | `09_wiretap_terminal_evt012.png` | Flujo crítico EVT-012 |
| 10 | `10_wiretap_artemis_auditoria.png` | Cola `agroalert.auditoria` creada por Wire Tap |
| 11 | `11_filter_terminal_evt013.png` | `FILTER -> Evento extremo detectado` |
| 12 | `12_filter_artemis_extremos.png` | Cola `agroalert.eventos.extremos` |
| 13 | `13_recipient_list_terminal_evt014.png` | EVT-014 crítico/extremo |
| 14 | `14_recipient_list_artemis.png` | Destinos dinámicos email y SMS |

## Evidencias adicionales recomendadas antes de subir a GitHub

Aunque las evidencias funcionales principales ya están cubiertas, conviene agregar:
1. `15_versiones_java_gradle_jbang.png` — `java -version`, `.\gradlew.bat -v`, `jbang --version`.
2. `16_docker_compose_ps.png` — Artemis y PostgreSQL en ejecución.
3. `17_postgresql_sensor_event.png` — `SELECT` mostrando EVT-010/011/012/013/014.
4. `18_postgresql_alert.png` — `SELECT` mostrando alertas generadas.
5. `19_estructura_proyecto_vscode.png` — árbol final del repositorio.
6. `20_build_successful.png` — último `.\gradlew.bat build`.

Estas capturas no sustituyen a las ya existentes; complementan la reproducibilidad y la documentación.
