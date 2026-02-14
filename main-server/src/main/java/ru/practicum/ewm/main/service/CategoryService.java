package ru.practicum.ewm.main.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.category.NewCategoryDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CategoryMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryDto saveCategory(NewCategoryDto newCategoryDto) {
        checkCategoryNameUniqueness(newCategoryDto.getName());

        Category category = CategoryMapper.toNewCategory(newCategoryDto);
        category = categoryRepository.save(category);

        log.info("Category сохранен с id: {}, имя: {}", category.getId(), category.getName());
        return CategoryMapper.toCategoryDto(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = getCategoryEntity(categoryId);

        categoryRepository.delete(category);

        log.info("Category с id: {} удалена", categoryId);

    }

    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto updateCategoryDto) {
        Category existingCategory = getCategoryEntity(catId);

        String newName = updateCategoryDto.getName();
        String currentName = existingCategory.getName();

        if (!currentName.equals(newName)) {
            checkCategoryNameUniqueness(newName);
            existingCategory.setName(newName);
        }

        log.info("Категория с id={} обновлена. Новое имя: {}", catId, newName);

        return CategoryMapper.toCategoryDto(existingCategory);
    }

    public List<CategoryDto> getAllCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<CategoryDto> categories = categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toCategoryDto)
                .collect(Collectors.toList());

        log.info("Получено {} категорий (from={}, size={})", categories.size(), from, size);
        return categories;
    }

    public CategoryDto getCategoryDto(Long categoryId) {
        Category category = getCategoryEntity(categoryId);
        log.info("Получена категория с id: {}", categoryId);
        return CategoryMapper.toCategoryDto(category);
    }

    public Category getCategoryEntity(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
    }

    private void checkCategoryNameUniqueness(String categoryName) {
        if (categoryRepository.existsByName(categoryName)) {
            throw new ConflictException("Категория с именем " + categoryName + " уже существует");
        }
    }
}
