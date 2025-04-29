package br.com.siecola.productsservice.events.dto;

public record ProductEventDto(
    String id,
    String code,
    String email,
    float price
) { }
