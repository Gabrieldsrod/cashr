package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.CategoryRequest;
import com.gabrieldsrod.cashr.api.dto.response.CategoryResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.Category;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private final UUID USER_A = UUID.randomUUID();
    private final UUID USER_B = UUID.randomUUID();

    private Category buildCategory(UUID id, UUID userId) {
        return Category.builder()
                .id(id)
                .name("Alimentação")
                .description("Gastos com comida")
                .userId(userId)
                .build();
    }

    private CategoryRequest buildRequest(String name, String description) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldReturnCategoryResponse() {
        UUID id = UUID.randomUUID();
        CategoryRequest request = buildRequest("Alimentação", "Gastos com comida");

        when(categoryRepository.existsByNameAndUserId("Alimentação", USER_A)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(buildCategory(id, USER_A));

        CategoryResponse response = categoryService.create(request, USER_A);

        assertNotNull(response.getId());
        assertEquals("Alimentação", response.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_shouldThrowWhenNameAlreadyExists() {
        CategoryRequest request = buildRequest("Alimentação", "Gastos com comida");

        when(categoryRepository.existsByNameAndUserId("Alimentação", USER_A)).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.create(request, USER_A));
        verify(categoryRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldChangeNameAndDescription() {
        UUID id = UUID.randomUUID();

        when(categoryRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildCategory(id, USER_A)));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.update(id, buildRequest("Saúde", "Planos e medicamentos"), USER_A);

        assertEquals("Saúde", response.getName());
        assertEquals("Planos e medicamentos", response.getDescription());
    }

    @Test
    void update_shouldThrowWhenCategoryNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.update(id, buildRequest("X", null), USER_A));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldThrowWhenCategoryHasTransactions() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildCategory(id, USER_A)));
        when(transactionRepository.existsByCategoryId(id)).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.delete(id, USER_A));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_shouldCallRepositoryWhenNoTransactions() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildCategory(id, USER_A)));
        when(transactionRepository.existsByCategoryId(id)).thenReturn(false);

        categoryService.delete(id, USER_A);

        verify(categoryRepository).deleteById(id);
    }

    // ── findAll / findById ────────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnOnlyCurrentUserCategories() {
        when(categoryRepository.findAllByUserId(USER_A)).thenReturn(List.of(
                buildCategory(UUID.randomUUID(), USER_A),
                buildCategory(UUID.randomUUID(), USER_A)
        ));

        List<CategoryResponse> result = categoryService.findAll(USER_A);

        assertEquals(2, result.size());
        result.forEach(r -> assertEquals(USER_A, r.getUserId()));
    }

    @Test
    void findById_shouldReturnCategoryWhenExists() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildCategory(id, USER_A)));

        CategoryResponse response = categoryService.findById(id, USER_A);

        assertEquals(id, response.getId());
        assertEquals(USER_A, response.getUserId());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.findById(id, USER_A));
    }

    // ── isolamento User A vs User B ───────────────────────────────────────────

    @Test
    void findById_userB_cannotReadUserA_category() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_B)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.findById(id, USER_B));
        verify(categoryRepository, never()).findByIdAndUserId(id, USER_A);
    }

    @Test
    void update_userB_cannotUpdateUserA_category() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_B)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.update(id, buildRequest("Hack", null), USER_B));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_userB_cannotDeleteUserA_category() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, USER_B)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.delete(id, USER_B));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void findAll_userB_doesNotSeeUserA_categories() {
        when(categoryRepository.findAllByUserId(USER_B)).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.findAll(USER_B);

        assertTrue(result.isEmpty());
        verify(categoryRepository, never()).findAllByUserId(USER_A);
    }

    @Test
    void create_sameNameAllowedForDifferentUsers() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        CategoryRequest request = buildRequest("Alimentação", "Comida");

        when(categoryRepository.existsByNameAndUserId("Alimentação", USER_A)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(buildCategory(idA, USER_A));
        categoryService.create(request, USER_A);

        when(categoryRepository.existsByNameAndUserId("Alimentação", USER_B)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(buildCategory(idB, USER_B));
        categoryService.create(request, USER_B);

        verify(categoryRepository, times(2)).save(any(Category.class));
    }
}
