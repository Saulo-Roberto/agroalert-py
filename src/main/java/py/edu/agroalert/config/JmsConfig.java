package py.edu.agroalert.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;

public class JmsConfig {

    public static void configure(CamelContext context) {

        ConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory(
                        "tcp://localhost:61616",
                        "admin",
                        "admin123"
                );

        JmsComponent jmsComponent =
                JmsComponent.jmsComponentAutoAcknowledge(connectionFactory);

        context.addComponent("jms", jmsComponent);

        System.out.println("Conexión JMS con ActiveMQ Artemis configurada");
    }
}