package com.designstudio.domain.repository;

import com.designstudio.domain.entity.User;
import com.designstudio.domain.entity.UserMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}