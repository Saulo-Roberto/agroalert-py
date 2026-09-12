package py.edu.agroalert.config;

import org.apache.camel.CamelContext;
import org.apache.camel.component.sql.SqlComponent;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {

    public static void configure(CamelContext context) {

        PGSimpleDataSource dataSource = new PGSimpleDataSource();

        dataSource.setServerNames(new String[]{"localhost"});
        dataSource.setPortNumbers(new int[]{5433});
        dataSource.setDatabaseName("agroalert");
        dataSource.setUser("agroalert");
        dataSource.setPassword("agroalert123");

        SqlComponent sqlComponent = new SqlComponent();
        sqlComponent.setDataSource(dataSource);

        context.addComponent("sql", sqlComponent);

        System.out.println("Conexión PostgreSQL configurada");
    }
}