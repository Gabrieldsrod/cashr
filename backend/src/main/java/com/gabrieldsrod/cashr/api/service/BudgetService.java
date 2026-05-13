package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.response.BudgetStatusResponse;
import com.gabrieldsrod.cashr.api.model.*;
import com.gabrieldsrod.cashr.api.repository.BudgetRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetStatusResponse calculateBudgetStatus(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        YearMonth yearMonth = YearMonth.from(budget.getMonth());

        BigDecimal spent = transactionRepository.sumAmountByCategoryIdAndTypeAndStatusAndPeriod(
                budget.getCategory().getId(),
                TransactionType.EXPENSE,
                TransactionStatus.PAID,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );

        BigDecimal percentage = spent
                .divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        BudgetStatus status;
        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            status = BudgetStatus.ESTOURADO;
        } else if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
            status = BudgetStatus.ATENCAO;
        } else {
            status = BudgetStatus.OK;
        }

        return BudgetStatusResponse.builder()
                .budgetId(budget.getId())
                .limitAmount(budget.getLimitAmount())
                .spent(spent)
                .percentageUsed(percentage)
                .status(status)
                .build();
    }
}
