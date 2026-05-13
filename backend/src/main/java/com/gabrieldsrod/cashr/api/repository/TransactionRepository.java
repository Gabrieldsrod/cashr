package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCompetenceDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.type = :type AND t.status = :status")
    BigDecimal sumAmountByAccountIdAndTypeAndStatus(UUID accountId, TransactionType type, TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.type = :type AND t.status = :status AND t.competenceDate BETWEEN :start AND :end")
    BigDecimal sumAmountByAccountIdAndTypeAndStatusAndPeriod(UUID accountId, TransactionType type, TransactionStatus status, LocalDate start, LocalDate end);

    boolean existsByAccountId(UUID accountId);

    boolean existsByCategoryId(UUID categoryId);
}
