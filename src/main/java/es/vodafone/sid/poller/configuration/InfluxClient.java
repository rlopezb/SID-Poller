package es.vodafone.sid.poller.configuration;


import com.influxdb.v3.client.InfluxDBClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class InfluxClient {
  @Value("${sid.influxDB.url}")
  private String url;
  @Value("${sid.influxDB.database}")
  private String database;
  @Value("${sid.influxDB.token}")
  private char[] token;

  @Bean
  public InfluxDBClient influxDBClient() {
    return InfluxDBClient.getInstance(url, token, database);
  }
}
