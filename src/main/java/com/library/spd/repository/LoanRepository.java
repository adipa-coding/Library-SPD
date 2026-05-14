package com.library.spd.repository;

import com.library.spd.model.Loan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends MongoRepository<Loan, String> {
    List<Loan> findByUserIdAndStatus(String userId, String status);
    List<Loan> findByUserId(String userId);
    List<Loan> findByBookIdAndStatus(String bookId, String status);
    List<Loan> findByStatus(String status);
}
