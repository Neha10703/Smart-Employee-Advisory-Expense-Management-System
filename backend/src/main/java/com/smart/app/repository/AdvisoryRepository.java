package com.smart.app.repository;

import com.smart.app.model.Advisory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisoryRepository extends JpaRepository<Advisory, Long> {
}
