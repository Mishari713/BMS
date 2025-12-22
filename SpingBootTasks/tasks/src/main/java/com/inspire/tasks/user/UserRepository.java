package com.inspire.tasks.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);


    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    // Join Fetch using JPQL
//    @Query("SELECT u FROM User u JOIN FETCH u.roles")

    // Join Fetch using native SQL
//    @Query(value = "SELECT u FROM User u JOIN FETCH u.roles", nativeQuery = true)

    // Entity graph
//    @EntityGraph(attributePaths = {"roles"})
//    List<User> findAll();

}