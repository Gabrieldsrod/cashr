package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    List<CreditCard> findAllByUserId(UUID userId);

    boolean existsByNameAndUserId(String name, UUID userId);
}
