package com.library.spd.repository;

import com.library.spd.model.Librarian;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibrarianRepository extends MongoRepository<Librarian, String> {
    Optional<Librarian> findByNameIgnoreCase(String name);
}
