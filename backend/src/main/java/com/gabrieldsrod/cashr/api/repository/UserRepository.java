package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
