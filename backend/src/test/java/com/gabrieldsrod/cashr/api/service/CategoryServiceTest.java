package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.CategoryRequest;
import com.gabrieldsrod.cashr.api.dto.CategoryResponse;
import com.gabrieldsrod.cashr.api.model.Category;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
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

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_shouldReturnCategoryResponse() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Alimentação");
        request.setDescription("Gastos com comida");

        Category saved = Category.builder()
                .id(UUID.randomUUID())
                .name("Alimentação")
                .description("Gastos com comida")
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.create(request);

        assertNotNull(response.getId());
        assertEquals("Alimentação", response.getName());
        assertEquals("Gastos com comida", response.getDescription());
        verify(categoryRepository).save(any(Category.class));
    }

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
        Category category = Category.builder()
                .id(id)
                .name("Saúde")
                .description("Planos e medicamentos")
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.findById(id);

        assertEquals(id, response.getId());
        assertEquals("Saúde", response.getName());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoryService.findById(id));
    }

    @Test
    void delete_shouldCallRepositoryDeleteById() {
        UUID id = UUID.randomUUID();

        categoryService.delete(id);

        verify(categoryRepository).deleteById(id);
    }
}
