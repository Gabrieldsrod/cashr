package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.CategoryRequest;
import com.gabrieldsrod.cashr.api.dto.response.CategoryResponse;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Categorias de receita e despesa")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lista as categorias do usuário autenticado")
    public ResponseEntity<List<CategoryResponse>> findAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryService.findAll(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma categoria por ID")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryService.findById(id, user.getId()));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova categoria")
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma categoria")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma categoria (só permitido se não houver transações vinculadas)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        categoryService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
