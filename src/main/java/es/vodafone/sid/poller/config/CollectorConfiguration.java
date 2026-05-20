package es.vodafone.sid.poller.config;

import lombok.Data;

@Data
public class CollectorConfiguration {
  private String name;
  private String cron;
  private long collectorTimeout;
  private long workerTimeout;
  private int size;
  private int queue;
}