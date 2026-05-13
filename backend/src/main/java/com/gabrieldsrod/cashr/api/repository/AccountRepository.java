package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByName(String name);
}
