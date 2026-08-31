package es.vodafone.sid.poller.model;

import tools.jackson.databind.JsonNode;

public record Protocol(
    short elementTypeId,
    String protocol,
    JsonNode config
) {
}
