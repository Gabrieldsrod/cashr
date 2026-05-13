package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByUserIdAndNameIgnoreCase(UUID userId, String name);
}
