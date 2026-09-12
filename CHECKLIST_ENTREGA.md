# Checklist final de entrega

## Obligatorio según la consigna
- [x] Proyecto ejecutable
- [x] Java 21
- [x] Apache Camel 4.5+ (usado 4.14.2)
- [x] ActiveMQ Artemis JMS
- [x] Gradle 9.6+ (usado 9.7.1)
- [x] JBang instalado y verificado (0.141.0)
- [x] PostgreSQL
- [x] `build.gradle`
- [x] Docker Compose
- [x] Script SQL `database/init.sql`
- [x] README con instalación, configuración y ejecución
- [x] Diagrama de arquitectura
- [x] Descripción del flujo principal
- [x] Identificación y justificación de EIP
- [x] Evidencias de pruebas
- [x] Ejemplos JSON
- [x] Caso normal
- [x] Casos de alerta
- [x] Caso excepcional / DLQ
- [x] Comunicación asíncrona JMS
- [x] Comunicación síncrona HTTP

## Antes de subir
- [ ] Confirmar integrantes con el docente: la consigna dice máximo 2; se informaron 3 nombres.
- [ ] Copiar `README.md`, `docs/`, `evidencias/` y `ejemplos/` a la raíz real de `agroalert-py`.
- [ ] Agregar capturas adicionales recomendadas en `evidencias/README_EVIDENCIAS.md`.
- [ ] Ejecutar `docker compose up -d`.
- [ ] Ejecutar `docker compose ps`.
- [ ] Ejecutar `.\gradlew.bat build`.
- [ ] Ejecutar `.\gradlew.bat run`.
- [ ] Probar `http://localhost:8085/agroalert/estado`.
- [ ] Verificar Artemis en `http://localhost:8161/console/artemis`.
- [ ] Revisar que no se suban contraseñas reales; las actuales son credenciales de laboratorio.
- [ ] Crear commit final.
- [ ] Subir a GitHub.
- [ ] Entregar la URL del repositorio en la plataforma.

## Recomendación de commit final
```text
Entrega final AgroAlert PY: EIP, Artemis, PostgreSQL, evidencias y documentación
```
