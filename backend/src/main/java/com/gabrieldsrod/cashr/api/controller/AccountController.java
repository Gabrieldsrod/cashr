package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.request.StatementLineResponse;
import com.gabrieldsrod.cashr.api.dto.response.AccountResponse;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> findAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accountService.findAllByUserId(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        return ResponseEntity.ok(accountService.findByIdAndUserId(id, user.getId(), start, end));
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<List<StatementLineResponse>> getStatement(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) TransactionStatus status) {
        return ResponseEntity.ok(accountService.getStatement(id, user.getId(), startDate, endDate, status));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.updateByIdAndUserId(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        accountService.deleteByIdAndUserId(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
