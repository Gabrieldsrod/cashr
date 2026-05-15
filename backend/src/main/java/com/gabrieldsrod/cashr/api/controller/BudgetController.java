package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.BudgetRequest;
import com.gabrieldsrod.cashr.api.dto.response.BudgetResponse;
import com.gabrieldsrod.cashr.api.dto.response.BudgetStatusResponse;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Validated
@Tag(name = "Budgets", description = "Orçamentos mensais por categoria")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "Lista orçamentos do usuário, opcionalmente filtrados por mês")
    public ResponseEntity<List<BudgetResponse>> findAll(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return ResponseEntity.ok(budgetService.findAll(user.getId(), year, month));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um orçamento por ID")
    public ResponseEntity<BudgetResponse> findById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.findById(id, user.getId()));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Calcula o status do orçamento (gasto, % usado e situação OK/ATENCAO/ESTOURADO)")
    public ResponseEntity<BudgetStatusResponse> getStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.getStatus(id, user.getId()));
    }

    @PostMapping
    @Operation(summary = "Cria um orçamento para uma categoria/mês")
    public ResponseEntity<BudgetResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um orçamento")
    public ResponseEntity<BudgetResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um orçamento")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        budgetService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
