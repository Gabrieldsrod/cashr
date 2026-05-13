package com.gabrieldsrod.cashr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String name;

    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String color;
}
