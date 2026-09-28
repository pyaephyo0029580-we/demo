package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository
        extends CrudRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndFriendCode(
            String username,
            String friendCode
    );

    Optional<User> findByFriendCode(
            String friendCode
    );

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByFriendCode(
            String friendCode
    );
}