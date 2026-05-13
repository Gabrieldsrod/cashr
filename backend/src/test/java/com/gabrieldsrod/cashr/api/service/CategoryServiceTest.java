package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.CategoryRequest;
import com.gabrieldsrod.cashr.api.dto.CategoryResponse;
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

    private Category buildCategory(UUID id) {
        return Category.builder()
                .id(id)
                .name("Alimentação")
                .description("Gastos com comida")
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

        when(categoryRepository.existsByName("Alimentação")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(buildCategory(id));

        CategoryResponse response = categoryService.create(request);

        assertNotNull(response.getId());
        assertEquals("Alimentação", response.getName());
        assertEquals("Gastos com comida", response.getDescription());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_shouldThrowWhenNameAlreadyExists() {
        CategoryRequest request = buildRequest("Alimentação", "Gastos com comida");

        when(categoryRepository.existsByName("Alimentação")).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.create(request));
        verify(categoryRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldChangeNameAndDescription() {
        UUID id = UUID.randomUUID();
        Category existing = buildCategory(id);

        CategoryRequest request = buildRequest("Saúde", "Planos e medicamentos");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.update(id, request);

        assertEquals("Saúde", response.getName());
        assertEquals("Planos e medicamentos", response.getDescription());
    }

    @Test
    void update_shouldThrowWhenCategoryNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.update(id, buildRequest("X", null)));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldThrowWhenCategoryHasTransactions() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.existsByCategoryId(id)).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.delete(id));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_shouldCallRepositoryWhenNoTransactions() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.existsByCategoryId(id)).thenReturn(false);

        categoryService.delete(id);

        verify(categoryRepository).deleteById(id);
    }

    // ── findAll / findById ────────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnListOfCategories() {
        List<Category> categories = List.of(
                Category.builder().id(UUID.randomUUID()).name("Alimentação").description("Comida").build(),
                Category.builder().id(UUID.randomUUID()).name("Transporte").description("Mobilidade").build()
        );

        when(categoryRepository.findAll()).thenReturn(categories);

        List<CategoryResponse> result = categoryService.findAll();

        assertEquals(2, result.size());
        assertEquals("Alimentação", result.get(0).getName());
        assertEquals("Transporte", result.get(1).getName());
    }

    @Test
    void findById_shouldReturnCategoryWhenExists() {
        UUID id = UUID.randomUUID();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(buildCategory(id)));

        CategoryResponse response = categoryService.findById(id);

        assertEquals(id, response.getId());
        assertEquals("Alimentação", response.getName());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.findById(id));
    }
}
