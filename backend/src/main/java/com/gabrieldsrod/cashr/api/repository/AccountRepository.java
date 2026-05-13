package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByNameAndUserId(String name, UUID userId);

    List<Account> findAllByUserId(UUID userId);
}
