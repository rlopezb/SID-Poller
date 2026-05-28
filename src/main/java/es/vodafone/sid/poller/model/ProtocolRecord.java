package es.vodafone.sid.poller.model;

import tools.jackson.databind.JsonNode;

public record ProtocolRecord(
    short elementTypeId,
    String protocol,
    JsonNode config
) {
}
