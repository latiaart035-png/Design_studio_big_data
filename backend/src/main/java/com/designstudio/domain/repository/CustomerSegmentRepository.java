// com.designstudio.domain.repository.CustomerSegmentRepository.java
package com.designstudio.domain.repository;

import com.designstudio.domain.entity.CustomerSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, Long> {
    Optional<CustomerSegment> findByName(String name);

    List<CustomerSegment> findByOrderByCreatedAtDesc();
}