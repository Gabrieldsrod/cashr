package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.CreditCardRequest;
import com.gabrieldsrod.cashr.api.dto.response.CreditCardResponse;
import com.gabrieldsrod.cashr.api.dto.response.InvoiceResponse;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.service.CreditCardService;
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
@RequestMapping("/api/credit-cards")
@RequiredArgsConstructor
@Validated
@Tag(name = "Credit Cards", description = "Cartões de crédito e geração de faturas")
public class CreditCardController {

    private final CreditCardService creditCardService;

    @GetMapping
    @Operation(summary = "Lista os cartões de crédito do usuário autenticado")
    public ResponseEntity<List<CreditCardResponse>> findAllByUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(creditCardService.findAllByUser(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cartão de crédito por ID")
    public ResponseEntity<CreditCardResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(creditCardService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cria um novo cartão de crédito")
    public ResponseEntity<CreditCardResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreditCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.create(request, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cartão de crédito")
    public ResponseEntity<CreditCardResponse> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreditCardRequest request) {
        return ResponseEntity.ok(creditCardService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um cartão de crédito")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        creditCardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Devolve a fatura do cartão para um mês/ano específico")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID id,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(creditCardService.getInvoice(id, year, month));
    }
}
