package com.gabrieldsrod.cashr.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    private String name;

    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String color;
}
