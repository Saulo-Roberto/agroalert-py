package py.edu.agroalert.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import py.edu.agroalert.model.EventoSensor;

public class AgroAlertRoute extends RouteBuilder {

    @Override
    public void configure() {

        // =========================================================
        // MANEJO GLOBAL DE ERRORES
        // =========================================================
        onException(Exception.class)
            .maximumRedeliveries(2)
            .redeliveryDelay(1000)
            .useOriginalMessage()
            .handled(true)
            .log("ERROR DE PROCESAMIENTO -> ${exception.message}")
            .log("Mensaje enviado a agroalert.dlq")
            .to("jms:queue:agroalert.dlq")
        .end();


        // =========================================================
        // RUTA PRINCIPAL
        // =========================================================
        from("jms:queue:agroalert.eventos")
            .routeId("agroalert-eventos-route")

            .log("Mensaje recibido desde Artemis: ${body}")

            // =====================================================
            // MESSAGE TRANSLATOR
            // JSON -> EventoSensor
            // =====================================================
            .unmarshal().json(EventoSensor.class)

            .log("Evento transformado a objeto Java: ${body}")

            // =====================================================
            // IDEMPOTENT RECEIVER
            // =====================================================
            .idempotentConsumer(
                simple("${body.event_id}"),
                MemoryIdempotentRepository.memoryIdempotentRepository(1000)
            )
            .skipDuplicate(true)

                .log(
                    "Evento NO duplicado. Iniciando procesamiento: ${body.event_id}"
                )

                // =================================================
                // HEADERS PARA POSTGRESQL
                // =================================================
                .setHeader("event_id", simple("${body.event_id}"))
                .setHeader("sensor_id", simple("${body.sensor_id}"))
                .setHeader("parcela_id", simple("${body.parcela_id}"))
                .setHeader("tipo", simple("${body.tipo}"))
                .setHeader("valor", simple("${body.valor}"))
                .setHeader("unidad", simple("${body.unidad}"))
                .setHeader("timestamp", simple("${body.timestamp}"))

                // =================================================
                // PERSISTENCIA DEL EVENTO
                // =================================================
                .to(
                    "sql:INSERT INTO sensor_event " +
                    "(event_id, sensor_id, parcela_id, tipo, valor, unidad, event_timestamp) " +
                    "VALUES (:#event_id, :#sensor_id, :#parcela_id, :#tipo, " +
                    ":#valor, :#unidad, CAST(:#timestamp AS TIMESTAMP)) " +
                    "ON CONFLICT (event_id) DO NOTHING"
                )

                .log(
                    "Evento guardado en PostgreSQL: ${body.event_id}"
                )

                // =================================================
                // FILTER - EVENTOS EXTREMOS
                // =================================================
                .filter(
                    simple("${body.valor} < 10 || ${body.valor} > 45")
                )

                    .log(
                        "FILTER -> Evento extremo detectado: ${body.event_id}"
                    )

                    // Para enviarlo por JMS
                    .marshal().json()

                    .to("jms:queue:agroalert.eventos.extremos")

                    // Volvemos al objeto Java para continuar
                    .unmarshal().json(EventoSensor.class)

                .end()

                // =================================================
                // CONTENT-BASED ROUTER
                // =================================================
                .choice()

                    // =================================================
                    // HUMEDAD CRITICA
                    // =================================================
                    .when(
                        simple(
                            "${body.tipo} == 'HUMEDAD_SUELO' " +
                            "&& ${body.valor} < 20"
                        )
                    )

                        .log(
                            "ALERTA CRITICA -> Humedad de suelo muy baja"
                        )

                        .setHeader(
                            "tipo_alerta",
                            constant("HUMEDAD_BAJA")
                        )

                        .setHeader(
                            "nivel_alerta",
                            constant("CRITICA")
                        )

                        .setHeader(
                            "mensaje_alerta",
                            constant("Humedad de suelo muy baja")
                        )

                        // =============================================
                        // GUARDAR ALERTA EN POSTGRESQL
                        // =============================================
                        .to(
                            "sql:INSERT INTO alert " +
                            "(event_id, tipo_alerta, nivel, mensaje, estado) " +
                            "VALUES (:#event_id, :#tipo_alerta, " +
                            ":#nivel_alerta, :#mensaje_alerta, 'CREATED')"
                        )

                        .log(
                            "Alerta critica guardada en PostgreSQL"
                        )

                        // JSON para mensajería
                        .marshal().json()

                        // =============================================
                        // RECIPIENT LIST
                        // Alerta critica -> Email + SMS
                        // =============================================
                        .setHeader(
                            "destinosNotificacion",
                            constant(
                                "jms:queue:agroalert.notificacion.email," +
                                "jms:queue:agroalert.notificacion.sms"
                            )
                        )

                        .recipientList(
                            header("destinosNotificacion")
                        )
                            .delimiter(",")
                        .end()

                        // =============================================
                        // WIRE TAP
                        // Copia asincrona hacia auditoria
                        // =============================================
                        .wireTap(
                            "jms:queue:agroalert.auditoria"
                        )
                        .end()

                        // =============================================
                        // POINT-TO-POINT
                        // =============================================
                        .to(
                            "jms:queue:agroalert.alertas.criticas"
                        )

                        // =============================================
                        // PUBLISH-SUBSCRIBE
                        // =============================================
                        .to(
                            "jms:topic:agroalert.notificaciones"
                        )


                    // =================================================
                    // TEMPERATURA ALTA
                    // =================================================
                    .when(
                        simple(
                            "${body.tipo} == 'TEMPERATURA' " +
                            "&& ${body.valor} > 35"
                        )
                    )

                        .log(
                            "ALERTA ALTA -> Temperatura elevada"
                        )

                        .setHeader(
                            "tipo_alerta",
                            constant("TEMPERATURA_ALTA")
                        )

                        .setHeader(
                            "nivel_alerta",
                            constant("ALTA")
                        )

                        .setHeader(
                            "mensaje_alerta",
                            constant("Temperatura elevada")
                        )

                        // =============================================
                        // GUARDAR ALERTA EN POSTGRESQL
                        // =============================================
                        .to(
                            "sql:INSERT INTO alert " +
                            "(event_id, tipo_alerta, nivel, mensaje, estado) " +
                            "VALUES (:#event_id, :#tipo_alerta, " +
                            ":#nivel_alerta, :#mensaje_alerta, 'CREATED')"
                        )

                        .log(
                            "Alerta alta guardada en PostgreSQL"
                        )

                        // JSON para mensajería
                        .marshal().json()

                        // =============================================
                        // RECIPIENT LIST
                        // Alerta alta -> Email
                        // =============================================
                        .setHeader(
                            "destinosNotificacion",
                            constant(
                                "jms:queue:agroalert.notificacion.email"
                            )
                        )

                        .recipientList(
                            header("destinosNotificacion")
                        )
                            .delimiter(",")
                        .end()

                        // =============================================
                        // WIRE TAP
                        // Copia asincrona hacia auditoria
                        // =============================================
                        .wireTap(
                            "jms:queue:agroalert.auditoria"
                        )
                        .end()

                        // =============================================
                        // POINT-TO-POINT
                        // =============================================
                        .to(
                            "jms:queue:agroalert.alertas"
                        )

                        // =============================================
                        // PUBLISH-SUBSCRIBE
                        // =============================================
                        .to(
                            "jms:topic:agroalert.notificaciones"
                        )


                    // =================================================
                    // EVENTO NORMAL
                    // =================================================
                    .otherwise()

                        .log(
                            "Evento normal -> Sin alerta"
                        )

                        .marshal().json()

                        .to(
                            "jms:queue:agroalert.eventos.normales"
                        )

                .end()

            .end();


        // =========================================================
        // SUSCRIPTOR 1 - MONITOREO
        // PUBLISH-SUBSCRIBE
        // =========================================================
        from("jms:topic:agroalert.notificaciones")
            .routeId("suscriptor-monitoreo")

            .log(
                "PUB-SUB MONITOREO -> Notificacion recibida: ${body}"
            );


        // =========================================================
        // SUSCRIPTOR 2 - AUDITORIA
        // PUBLISH-SUBSCRIBE
        // =========================================================
        from("jms:topic:agroalert.notificaciones")
            .routeId("suscriptor-auditoria")

            .log(
                "PUB-SUB AUDITORIA -> Notificacion recibida: ${body}"
            );


        // =========================================================
        // COMUNICACION SINCRONA - HTTP
        // =========================================================
        from("jetty:http://0.0.0.0:8085/agroalert/estado")
            .routeId("agroalert-estado-http")

            .setHeader(
                "Content-Type",
                constant("application/json")
            )

            .setBody(
                constant(
                    "{"
                    + "\"servicio\":\"AgroAlert PY\","
                    + "\"estado\":\"ACTIVO\","
                    + "\"comunicacion\":\"SINCRONA\""
                    + "}"
                )
            )

            .log(
                "Consulta HTTP sincrona recibida en /agroalert/estado"
            );
    }
}