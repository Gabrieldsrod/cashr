package com.gabrieldsrod.cashr.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TagResponse {
    private UUID id;
    private UUID userId;
    private String name;
}
