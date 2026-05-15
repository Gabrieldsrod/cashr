package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.category WHERE b.userId = :userId ORDER BY b.month DESC")
    List<Budget> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.category WHERE b.userId = :userId AND b.month = :month ORDER BY b.category.name ASC")
    List<Budget> findAllByUserIdAndMonth(@Param("userId") UUID userId, @Param("month") LocalDate month);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.category WHERE b.id = :id AND b.userId = :userId")
    Optional<Budget> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, LocalDate month);
}
