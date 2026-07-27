package es.vodafone.sid.poller;

import org.snmp4j.log.JavaLogFactory;
import org.snmp4j.log.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PollerApplication {
  static {
    LogFactory.setLogFactory(new JavaLogFactory());
  }
  public static void main(String[] args) {
    SpringApplication.run(PollerApplication.class, args);
  }
}
