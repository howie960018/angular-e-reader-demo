package com.ctbc.ebookstore.service;

import com.ctbc.ebookstore.bean.Category;
import com.ctbc.ebookstore.exception.ResourceNotFoundException;
import com.ctbc.ebookstore.repository.BookRepository;
import com.ctbc.ebookstore.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final BookRepository bookRepo;

    public CategoryService(CategoryRepository categoryRepo, BookRepository bookRepo) {
        this.categoryRepo = categoryRepo;
        this.bookRepo = bookRepo;
    }

    public List<Category> findAll() {
        return categoryRepo.findAll();
    }

    public Category findById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    public Category create(String name, String description) {
        return categoryRepo.save(new Category(name, description));
    }

    public void delete(Long id) {
        Category category = findById(id);
        boolean hasBooks = bookRepo.findAll().stream()
                .anyMatch(b -> b.getCategory() != null && b.getCategory().getId().equals(id));
        if (hasBooks) {
            throw new com.ctbc.ebookstore.exception.BadRequestException(
                    "Cannot delete category with existing books");
        }
        categoryRepo.delete(category);
    }
}
