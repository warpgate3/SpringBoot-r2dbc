package info.m2sj.reactivepostgresql;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import org.springframework.data.r2dbc.core.DatabaseClient;

import java.util.concurrent.TimeUnit;

public class RNumberClient {
    public static void main(String[] args) throws InterruptedException {
        final PostgresqlConnectionFactory postgresqlConnectionFactory = new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host("localhost")
                        .database("test_db")
                        .username("test_user")
                        .password("1234").build());

        DatabaseClient client = DatabaseClient.create(postgresqlConnectionFactory);

        client.select()
                .from(RNumber.class)
                .fetch().all()
                .subscribe(System.out::println);

        TimeUnit.SECONDS.sleep(10);
    }
}
