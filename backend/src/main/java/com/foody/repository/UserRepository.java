package com.foody.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.foody.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByResetTokenHash(String resetTokenHash);
}
