package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.TopUpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TopUpCodeRepository extends JpaRepository<TopUpCode, Long> {
    Optional<TopUpCode> findByCode(String code);
}
