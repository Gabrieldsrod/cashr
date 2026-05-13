package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
}
