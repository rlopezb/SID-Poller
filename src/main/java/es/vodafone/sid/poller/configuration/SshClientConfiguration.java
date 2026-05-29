package es.vodafone.sid.poller.configuration;

import org.apache.sshd.client.SshClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SshClientConfiguration {
  @Bean(destroyMethod = "stop")
  public SshClient sshClient() {
    SshClient client = SshClient.setUpDefaultClient();
    client.start();
    return client;
  }
}
