package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.TagRequest;
import com.gabrieldsrod.cashr.api.dto.response.TagResponse;
import com.gabrieldsrod.cashr.api.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagResponse>> findAll(@RequestParam UUID userId) {
        return ResponseEntity.ok(tagService.findAll(userId));
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transaction/{transactionId}")
    public ResponseEntity<Void> attachToTransaction(@PathVariable UUID transactionId, @RequestBody List<String> tagNames) {
        tagService.attachToTransaction(transactionId, tagNames);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/transaction/{transactionId}/{tagId}")
    public ResponseEntity<Void> detachFromTransaction(@PathVariable UUID transactionId, @PathVariable UUID tagId) {
        tagService.detachFromTransaction(transactionId, tagId);
        return ResponseEntity.noContent().build();
    }
}
