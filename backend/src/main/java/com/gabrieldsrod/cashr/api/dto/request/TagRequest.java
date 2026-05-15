package com.gabrieldsrod.cashr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank
    private String name;
}
