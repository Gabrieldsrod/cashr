package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.CreditCardRequest;
import com.gabrieldsrod.cashr.api.dto.response.CreditCardResponse;
import com.gabrieldsrod.cashr.api.dto.response.InvoiceResponse;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.service.CreditCardService;
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
public class CreditCardController {

    private final CreditCardService creditCardService;

    @GetMapping
    public ResponseEntity<List<CreditCardResponse>> findAllByUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(creditCardService.findAllByUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditCardResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(creditCardService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CreditCardResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreditCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.create(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditCardResponse> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreditCardRequest request) {
        return ResponseEntity.ok(creditCardService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        creditCardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID id,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(creditCardService.getInvoice(id, year, month));
    }
}
