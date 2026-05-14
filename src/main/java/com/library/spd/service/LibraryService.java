package com.library.spd.service;

import com.library.spd.model.Book;
import com.library.spd.model.Loan;
import com.library.spd.model.User;
import com.library.spd.repository.BookRepository;
import com.library.spd.repository.LoanRepository;
import com.library.spd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LibraryService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoggerService loggerService;

    // Helper to populate transient fields for UI views
    private Loan enrichLoan(Loan loan) {
        if (loan != null) {
            if (loan.getBookId() != null) {
                loan.setBook(bookRepository.findById(loan.getBookId()).orElse(null));
            }
            if (loan.getUserId() != null) {
                loan.setUser(userRepository.findById(loan.getUserId()).orElse(null));
            }
        }
        return loan;
    }

    private List<Loan> enrichLoans(List<Loan> loans) {
        if (loans != null) {
            loans.forEach(this::enrichLoan);
        }
        return loans;
    }

    // BOOK OPERATIONS
    public Book addBook(Book book) {
        if (bookRepository.existsById(book.getBookId())) {
            throw new RuntimeException("Book with ID " + book.getBookId() + " already exists!");
        }
        Book savedBook = bookRepository.save(book);
        loggerService.log("New book added. <{time}>BookID=" + savedBook.getBookId() + ",Title=" + savedBook.getTitle());
        return savedBook;
    }

    public List<Book> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        syncBookStatuses(books);
        return books;
    }

    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooks();
        }
        List<Book> books = bookRepository.findByTitleContainingIgnoreCaseOrBookIdContainingIgnoreCaseOrGenreContainingIgnoreCase(keyword, keyword, keyword);
        syncBookStatuses(books);
        return books;
    }

    private void syncBookStatuses(List<Book> books) {
        LocalDate today = LocalDate.now();
        for (Book book : books) {
            String status = book.getStatus();
            if (status == null) {
                book.setStatus("Available");
                bookRepository.save(book);
            } else if ("Rented".equals(status) || "Overdue-not returned".equals(status)) {
                List<Loan> activeLoans = loanRepository.findByBookIdAndStatus(book.getBookId(), "RENTED");
                if (!activeLoans.isEmpty()) {
                    Loan activeLoan = activeLoans.get(0);
                    if (today.isAfter(activeLoan.getExpectedReturnDate())) {
                        if (!"Overdue-not returned".equals(status)) {
                            book.setStatus("Overdue-not returned");
                            bookRepository.save(book);
                        }
                    } else {
                        if (!"Rented".equals(status)) {
                            book.setStatus("Rented");
                            bookRepository.save(book);
                        }
                    }
                } else {
                    book.setStatus("Available");
                    bookRepository.save(book);
                }
            }
        }
    }

    // USER OPERATIONS
    public User registerUser(User user) {
        String nextId = generateNextUserId();
        user.setUserId(nextId);
        User savedUser = userRepository.save(user);
        loggerService.log("new user registered. <{time}>UserID=" + savedUser.getUserId() + ",userName="
                + savedUser.getName() + ".");
        return savedUser;
    }

    public User updateUser(String userId, User updatedDetails) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        existingUser.setName(updatedDetails.getName());
        existingUser.setGrade(updatedDetails.getGrade());
        existingUser.setPhoneNumber(updatedDetails.getPhoneNumber());
        existingUser.setGuardianPhoneNumber(updatedDetails.getGuardianPhoneNumber());
        existingUser.setAddress(updatedDetails.getAddress());
        
        User savedUser = userRepository.save(existingUser);
        loggerService.log("User details manually updated. <{time}>UserID=" + savedUser.getUserId() + ",userName=" + savedUser.getName() + ".");
        return savedUser;
    }

    private String generateNextUserId() {
        java.util.List<User> users = userRepository.findAll();
        int maxId = 0;
        for (User u : users) {
            String uid = u.getUserId();
            if (uid != null && uid.startsWith("spdLib")) {
                try {
                    int num = Integer.parseInt(uid.substring(6));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException e) {
                    // skip non-numeric suffixes
                }
            }
        }
        return String.format("spdLib%03d", maxId + 1);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public List<Loan> getUserLoans(User user) {
        return enrichLoans(loanRepository.findByUserIdAndStatus(user.getUserId(), "RENTED"));
    }

    public List<Loan> getUserRentHistory(User user) {
        List<Loan> history = loanRepository.findByUserId(user.getUserId());
        history.sort((a,b) -> {
            if (a.getRentDate() == null) return 1;
            if (b.getRentDate() == null) return -1;
            return b.getRentDate().compareTo(a.getRentDate());
        });
        return enrichLoans(history);
    }

    public List<Loan> getUserUnpaidLoans(User user) {
        return enrichLoans(loanRepository.findByUserIdAndStatus(user.getUserId(), "RETURNED_UNPAID"));
    }

    // LOAN OPERATIONS
    public List<Loan> getAllActiveLoans() {
        return enrichLoans(loanRepository.findByStatus("RENTED"));
    }

    public Loan rentBook(String userId, String bookId, LocalDate expectedReturnDate) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Book not found"));

        if (!"Available".equals(book.getStatus()) && book.getStatus() != null) {
            throw new RuntimeException("Book is currently rented and not available!");
        }

        if (!getUserUnpaidLoans(user).isEmpty()) {
            throw new RuntimeException("User must clear unpaid fines before renting new books!");
        }

        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setUser(user);
        loan.setBook(book);
        loan.setRentDate(LocalDate.now());
        loan.setExpectedReturnDate(expectedReturnDate != null ? expectedReturnDate : LocalDate.now().plusWeeks(2));
        loan.setStatus("RENTED");
        loan.setOverdueFee(0.0);

        book.setStatus("Rented");
        bookRepository.save(book);

        Loan savedLoan = loanRepository.save(loan);
        enrichLoan(savedLoan);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        loggerService.log("UserID " + user.getUserId() + " rented a book. <{time}>BookID=" + book.getBookId()
                + " return date=" + loan.getExpectedReturnDate().format(dtf));

        return savedLoan;
    }

    public Loan returnBook(String loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (!"RENTED".equals(loan.getStatus())) {
            throw new RuntimeException("Book already returned");
        }
        
        enrichLoan(loan);

        if (loan.getBook() != null) {
            Book book = loan.getBook();
            book.setStatus("Available");
            bookRepository.save(book);
        }

        loan.setActualReturnDate(LocalDate.now());

        long daysHeld = 0;
        if (loan.getRentDate() != null && loan.getActualReturnDate() != null) {
            daysHeld = ChronoUnit.DAYS.between(loan.getRentDate(), loan.getActualReturnDate());
        }
        if (daysHeld < 0) daysHeld = 0;

        double fine = 0.0;
        if (daysHeld > 14) {
            long overdueDays = daysHeld - 14;
            long overdueWeeks = (long) Math.ceil((double) overdueDays / 7.0);
            fine = overdueWeeks * 20.0;
        }

        loan.setOverdueFee(fine);

        if (fine > 0) {
            loan.setStatus("RETURNED_UNPAID");
            loggerService.log("UserID " + loan.getUserId() + " returned a book past due. <{time}>BookID="
                    + loan.getBookId() + ". Fine generated: " + fine);
        } else {
            loan.setStatus("DONE");
            loggerService.log("UserID " + loan.getUserId() + " returned a book on time. <{time}>BookID="
                    + loan.getBookId());
        }

        return enrichLoan(loanRepository.save(loan));
    }

    public Loan payLoanFee(String loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if ("RETURNED_UNPAID".equals(loan.getStatus())) {
            loan.setStatus("DONE");
            loanRepository.save(loan);
            loggerService
                    .log("UserID " + loan.getUserId() + " paid the loan amount. <{time}>setStatus=done.");
        }
        return enrichLoan(loan);
    }

    // --- ANALYTICS ---
    public Map<String, Long> getBooksByGenre() {
        return bookRepository.findAll().stream()
            .filter(b -> b.getGenre() != null && !b.getGenre().trim().isEmpty())
            .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()));
    }

    public Map<String, Long> getUsersByGrade() {
        return userRepository.findAll().stream()
            .filter(u -> u.getGrade() != null && !u.getGrade().trim().isEmpty())
            .collect(Collectors.groupingBy(User::getGrade, Collectors.counting()));
    }

    public Map<String, Long> getLoansByStatus() {
        return loanRepository.findAll().stream()
            .filter(l -> l.getStatus() != null && !l.getStatus().trim().isEmpty())
            .collect(Collectors.groupingBy(Loan::getStatus, Collectors.counting()));
    }
}
