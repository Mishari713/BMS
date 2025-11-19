package com.inspire.tasks.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByTitle(String title);

    Boolean existsByTitle(String title);

    List<Book> findAllByAuthorName(String author);

    // Join Fetch using JPQL
//    @Query("SELECT u FROM User u JOIN FETCH u.roles")

    // Join Fetch using native SQL
//    @Query(value = "SELECT u FROM User u JOIN FETCH u.roles", nativeQuery = true)

    // Entity graph
//    @EntityGraph(attributePaths = {"roles"})
//    List<User> findAll();

}