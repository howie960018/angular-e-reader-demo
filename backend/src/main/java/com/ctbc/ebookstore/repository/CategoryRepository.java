package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
