package com.designstudio.domain.repository;

import com.designstudio.domain.entity.DesignOrder;
import com.designstudio.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignOrderRepository extends JpaRepository<DesignOrder, Long> {
    List<DesignOrder> findByUser(User user);
}