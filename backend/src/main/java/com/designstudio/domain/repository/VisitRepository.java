package com.designstudio.domain.repository;

import com.designstudio.domain.entity.User;
import com.designstudio.domain.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUser(User user);
    Visit findTopByUserOrderByVisitTimeDesc(User user);
}