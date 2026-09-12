package py.edu.agroalert;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import py.edu.agroalert.config.DatabaseConfig;
import py.edu.agroalert.config.JmsConfig;
import py.edu.agroalert.routes.AgroAlertRoute;

public class Main {

    public static void main(String[] args) throws Exception {

        CamelContext camelContext = new DefaultCamelContext();

        JmsConfig.configure(camelContext);
        DatabaseConfig.configure(camelContext);

        camelContext.addRoutes(new AgroAlertRoute());

        System.out.println("AgroAlert PY iniciado correctamente");

        camelContext.start();

        Thread.currentThread().join();
    }
}