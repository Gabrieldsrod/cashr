package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByDateBetween(LocalDate start, LocalDate end);
}
