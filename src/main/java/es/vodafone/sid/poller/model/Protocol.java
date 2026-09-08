package es.vodafone.sid.poller.model;

import tools.jackson.databind.JsonNode;

public record Protocol(
    Short elementTypeId,
    String protocol,
    JsonNode config
) {
}
