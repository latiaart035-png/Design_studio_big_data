package com.designstudio.domain.repository;

import com.designstudio.domain.entity.User;
import com.designstudio.domain.entity.UserMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {
    List<UserMessage> findByUserOrderBySentAtDesc(User user);
    List<UserMessage> findByUserAndIsReadFalse(User user);
    long countByUserAndIsReadFalse(User user);
    List<UserMessage> findBySentAtAfter(LocalDateTime dateTime);
}