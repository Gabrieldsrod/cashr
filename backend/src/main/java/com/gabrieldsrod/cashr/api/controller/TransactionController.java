package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.InstallmentGroupUpdateRequest;
import com.gabrieldsrod.cashr.api.dto.request.InstallmentRequest;
import com.gabrieldsrod.cashr.api.dto.request.TransactionRequest;
import com.gabrieldsrod.cashr.api.dto.response.TransactionResponse;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transactions", description = "Lançamentos financeiros, parcelas e saldo mensal")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Lista transações paginadas, com filtros opcionais por tipo, status e mês")
    public ResponseEntity<Page<TransactionResponse>> findAll(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @PageableDefault(size = 20, sort = "competenceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.findAll(user.getId(), type, status, year, month, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma transação por ID")
    public ResponseEntity<TransactionResponse> findById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(id, user.getId()));
    }

    @GetMapping("/installment-group/{groupId}")
    @Operation(summary = "Lista todas as parcelas de um grupo de parcelamento")
    public ResponseEntity<List<TransactionResponse>> findByInstallmentGroup(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(transactionService.findByInstallmentGroup(groupId, user.getId()));
    }

    @PostMapping
    @Operation(summary = "Cria uma transação isolada")
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request, user.getId()));
    }

    @PostMapping("/installments")
    @Operation(summary = "Cria N parcelas com mesmo grupo, dividindo o valor total igualmente")
    public ResponseEntity<List<TransactionResponse>> createInstallments(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InstallmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createInstallments(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma transação (paymentMethod é imutável; afeta apenas a parcela atual)")
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma transação")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        transactionService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/installment-group/{groupId}")
    @Operation(summary = "Atualiza todas as parcelas de um grupo em bulk (preserva competenceDate de cada parcela)")
    public ResponseEntity<List<TransactionResponse>> updateInstallmentGroup(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @Valid @RequestBody InstallmentGroupUpdateRequest request) {
        return ResponseEntity.ok(transactionService.updateInstallmentGroup(groupId, request, user.getId()));
    }

    @GetMapping("/balance")
    @Operation(summary = "Saldo líquido (income − expense) do mês, considerando apenas transações PAID")
    public ResponseEntity<BigDecimal> getMonthlyBalance(
            @AuthenticationPrincipal User user,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(transactionService.getMonthlyBalance(user.getId(), year, month));
    }
}
