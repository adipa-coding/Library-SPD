package com.library.spd.repository;

import com.library.spd.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findByTitleContainingIgnoreCaseOrBookIdContainingIgnoreCaseOrGenreContainingIgnoreCase(String title, String bookId, String genre);
}
