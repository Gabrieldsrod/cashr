package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndCompetenceDateBetween(UUID userId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.type = :type AND t.status = :status")
    BigDecimal sumAmountByAccountIdAndTypeAndStatus(UUID accountId, TransactionType type, TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.type = :type AND t.status = :status AND t.competenceDate BETWEEN :start AND :end")
    BigDecimal sumAmountByAccountIdAndTypeAndStatusAndPeriod(UUID accountId, TransactionType type, TransactionStatus status, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.category.id = :categoryId AND t.type = :type AND t.status = :status AND t.competenceDate BETWEEN :start AND :end")
    BigDecimal sumAmountByCategoryIdAndTypeAndStatusAndPeriod(UUID categoryId, TransactionType type, TransactionStatus status, LocalDate start, LocalDate end);

    boolean existsByAccountId(UUID accountId);

    boolean existsByCategoryId(UUID categoryId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.type = :type AND t.status = :status AND t.competenceDate < :date")
    BigDecimal sumAmountByAccountIdAndTypeAndStatusBefore(@Param("accountId") UUID accountId, @Param("type") TransactionType type, @Param("status") TransactionStatus status, @Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.account.id = :accountId AND t.competenceDate BETWEEN :start AND :end AND (:status IS NULL OR t.status = :status) ORDER BY t.competenceDate ASC")
    List<Transaction> findStatement(@Param("accountId") UUID accountId, @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.creditCard.id = :creditCardId AND t.invoiceDate BETWEEN :start AND :end ORDER BY t.competenceDate ASC")
    List<Transaction> findByCreditCardAndInvoicePeriod(@Param("creditCardId") UUID creditCardId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = "SELECT t FROM Transaction t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.creditCard " +
                   "WHERE t.user.id = :userId " +
                   "AND (:type IS NULL OR t.type = :type) " +
                   "AND (:status IS NULL OR t.status = :status) " +
                   "AND (:start IS NULL OR t.competenceDate >= :start) " +
                   "AND (:end IS NULL OR t.competenceDate <= :end)",
           countQuery = "SELECT COUNT(t) FROM Transaction t " +
                        "WHERE t.user.id = :userId " +
                        "AND (:type IS NULL OR t.type = :type) " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND (:start IS NULL OR t.competenceDate >= :start) " +
                        "AND (:end IS NULL OR t.competenceDate <= :end)")
    Page<Transaction> findWithFilters(@Param("userId") UUID userId,
                                      @Param("type") TransactionType type,
                                      @Param("status") TransactionStatus status,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end,
                                      Pageable pageable);

    List<Transaction> findByInstallmentGroupIdOrderByInstallmentNumberAsc(UUID installmentGroupId);
}
