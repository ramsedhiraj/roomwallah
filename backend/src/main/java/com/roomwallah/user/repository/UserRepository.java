package com.roomwallah.user.repository;

import com.roomwallah.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    @Override
    @EntityGraph(attributePaths = {"preferences"})
    Optional<User> findById(UUID id);

    @EntityGraph(attributePaths = {"preferences"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"preferences"})
    Optional<User> findByPhone(String phone);
}
