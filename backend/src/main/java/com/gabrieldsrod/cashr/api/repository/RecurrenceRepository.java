package com.gabrieldsrod.cashr.api.repository;

import com.gabrieldsrod.cashr.api.model.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecurrenceRepository extends JpaRepository<Recurrence, UUID> {

    List<Recurrence> findByNextOccurrenceLessThanEqualAndIsActiveTrue(LocalDate date);
}
