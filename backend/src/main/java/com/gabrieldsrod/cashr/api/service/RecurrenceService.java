package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.model.*;
import com.gabrieldsrod.cashr.api.repository.RecurrenceRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurrenceService {

    private final RecurrenceRepository recurrenceRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void processRecurrences() {
        List<Recurrence> due = recurrenceRepository.findByNextOccurrenceLessThanEqualAndIsActiveTrue(LocalDate.now());

        for (Recurrence recurrence : due) {
            Transaction transaction = Transaction.builder()
                    .type(recurrence.getType())
                    .status(TransactionStatus.PENDING)
                    .amount(recurrence.getAmount())
                    .competenceDate(recurrence.getNextOccurrence())
                    .description(recurrence.getDescription())
                    .category(recurrence.getCategory())
                    .account(recurrence.getAccount())
                    .build();

            transactionRepository.save(transaction);

            recurrence.setNextOccurrence(calculateNextOccurrence(recurrence));
            recurrenceRepository.save(recurrence);
        }
    }

    private LocalDate calculateNextOccurrence(Recurrence recurrence) {
        LocalDate current = recurrence.getNextOccurrence();
        return switch (recurrence.getFrequency()) {
            case DAILY   -> current.plusDays(1);
            case WEEKLY  -> current.plusWeeks(1);
            case MONTHLY -> advanceByMonth(current, recurrence.getDayOfMonth(), 1);
            case YEARLY  -> advanceByMonth(current, recurrence.getDayOfMonth(), 12);
        };
    }

    private LocalDate advanceByMonth(LocalDate current, Integer dayOfMonth, int months) {
        LocalDate next = current.plusMonths(months);
        if (dayOfMonth == null) {
            return next;
        }
        return next.withDayOfMonth(Math.min(dayOfMonth, next.lengthOfMonth()));
    }
}
