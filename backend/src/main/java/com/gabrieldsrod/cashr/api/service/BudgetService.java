package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.BudgetRequest;
import com.gabrieldsrod.cashr.api.dto.response.BudgetResponse;
import com.gabrieldsrod.cashr.api.dto.response.BudgetStatusResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.exception.ResourceNotFoundException;
import com.gabrieldsrod.cashr.api.model.*;
import com.gabrieldsrod.cashr.api.repository.BudgetRepository;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public BudgetResponse create(BudgetRequest request, UUID userId) {
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        LocalDate month = YearMonth.of(request.getYear(), request.getMonth()).atDay(1);

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, category.getId(), month)) {
            throw new BusinessException("Budget already exists for this category and month");
        }

        Budget budget = Budget.builder()
                .userId(userId)
                .category(category)
                .month(month)
                .limitAmount(request.getLimitAmount())
                .build();

        return toResponse(budgetRepository.save(budget));
    }

    public BudgetResponse update(UUID id, BudgetRequest request, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        LocalDate month = YearMonth.of(request.getYear(), request.getMonth()).atDay(1);

        boolean keyChanged = !budget.getCategory().getId().equals(category.getId())
                || !budget.getMonth().equals(month);

        if (keyChanged && budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, category.getId(), month)) {
            throw new BusinessException("Budget already exists for this category and month");
        }

        budget.setCategory(category);
        budget.setMonth(month);
        budget.setLimitAmount(request.getLimitAmount());

        return toResponse(budgetRepository.save(budget));
    }

    public List<BudgetResponse> findAll(UUID userId, Integer year, Integer month) {
        List<Budget> budgets = (year != null && month != null)
                ? budgetRepository.findAllByUserIdAndMonth(userId, YearMonth.of(year, month).atDay(1))
                : budgetRepository.findAllByUserId(userId);

        return budgets.stream().map(this::toResponse).toList();
    }

    public BudgetResponse findById(UUID id, UUID userId) {
        return toResponse(budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found")));
    }

    public void delete(UUID id, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    public BudgetStatusResponse getStatus(UUID id, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

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
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .month(budget.getMonth())
                .limitAmount(budget.getLimitAmount())
                .spent(spent)
                .percentageUsed(percentage)
                .status(status)
                .build();
    }

    private BudgetResponse toResponse(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .month(budget.getMonth())
                .limitAmount(budget.getLimitAmount())
                .build();
    }
}
