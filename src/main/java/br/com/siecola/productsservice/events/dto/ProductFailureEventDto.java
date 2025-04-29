package br.com.siecola.productsservice.events.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductFailureEventDto(
    String email,
    int status,
    String error,
    String id
) {
}
