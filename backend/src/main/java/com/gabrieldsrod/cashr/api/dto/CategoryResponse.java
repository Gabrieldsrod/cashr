package com.gabrieldsrod.cashr.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private String color;
}
