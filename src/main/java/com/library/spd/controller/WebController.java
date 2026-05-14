package com.library.spd.controller;

import com.library.spd.model.Book;
import com.library.spd.model.User;
import com.library.spd.model.Loan;
import com.library.spd.model.Librarian;
import com.library.spd.repository.LibrarianRepository;
import com.library.spd.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class WebController {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private LibrarianRepository librarianRepository;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        model.addAttribute("librarianName", session.getAttribute("librarianName"));
        return "index";
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String name, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<Librarian> librarian = librarianRepository.findByNameIgnoreCase(name.trim());
        if (librarian.isPresent()) {
            session.setAttribute("librarianName", librarian.get().getName());
            return "redirect:/";
        }
        redirectAttributes.addFlashAttribute("error", "Librarian name not recognized!");
        return "redirect:/login";
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- BOOKS ---
    @GetMapping("/books")
    public String viewBooks(@RequestParam(value = "search", required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("books", libraryService.searchBooks(search));
            model.addAttribute("searchKeyword", search);
        } else {
            model.addAttribute("books", libraryService.getAllBooks());
        }
        return "books";
    }

    @GetMapping("/books/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "add-book";
    }

    @PostMapping("/books/add")
    public String addBook(@ModelAttribute Book book, RedirectAttributes redirectAttributes) {
        try {
            libraryService.addBook(book);
            redirectAttributes.addFlashAttribute("success", "Book added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }

    // --- USERS ---
    @GetMapping("/users")
    public String viewUsers(Model model) {
        model.addAttribute("users", libraryService.getAllUsers());
        return "users";
    }

    @GetMapping("/users/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        return "add-user";
    }

    @PostMapping("/users/add")
    public String addUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            User saved = libraryService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "User registered successfully! ID: " + saved.getUserId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    @GetMapping("/users/{userId}/edit")
    public String showEditUserForm(@PathVariable String userId, Model model) {
        User user = libraryService.getUser(userId);
        if (user == null) {
            return "redirect:/users";
        }
        model.addAttribute("user", user);
        return "edit-user";
    }

    @PostMapping("/users/{userId}/edit")
    public String editUser(@PathVariable String userId, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            libraryService.updateUser(userId, user);
            redirectAttributes.addFlashAttribute("success", "User profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/" + userId;
    }

    @GetMapping("/users/{userId}")
    public String viewUserProfile(@PathVariable String userId, Model model) {
        User user = libraryService.getUser(userId);
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("loans", libraryService.getUserLoans(user));
            model.addAttribute("unpaid", libraryService.getUserUnpaidLoans(user));
            model.addAttribute("rentHistory", libraryService.getUserRentHistory(user));
            return "user-profile";
        }
        return "redirect:/users";
    }

    // --- LOANS ---
    @GetMapping("/loans/rent")
    public String showRentForm(Model model) {
        model.addAttribute("users", libraryService.getAllUsers());
        model.addAttribute("books", libraryService.getAllBooks());
        return "rent-book";
    }

    @PostMapping("/loans/rent")
    public String rentBook(@RequestParam String userId, @RequestParam String bookId, 
                           @RequestParam(required=false) LocalDate expectedReturnDate,
                           RedirectAttributes redirectAttributes) {
        try {
            libraryService.rentBook(userId, bookId, expectedReturnDate);
            redirectAttributes.addFlashAttribute("success", "Book rented successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/" + userId;
    }

    @GetMapping("/loans/return")
    public String showReturnForm(Model model) {
        model.addAttribute("activeLoans", libraryService.getAllActiveLoans());
        return "return-book";
    }

    @PostMapping("/loans/return")
    public String returnBook(@RequestParam String loanId, @RequestParam(required = false) String userId, RedirectAttributes redirectAttributes) {
        try {
            Loan loan = libraryService.returnBook(loanId);
            if ("RETURNED_UNPAID".equals(loan.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Book returned late. Overdue fee: " + loan.getOverdueFee() + " rupees.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Book returned on time!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        if (userId != null && !userId.isEmpty()) {
            return "redirect:/users/" + userId;
        }
        return "redirect:/loans/return";
    }

    @PostMapping("/loans/pay")
    public String payLoanFee(@RequestParam String loanId, @RequestParam String userId, RedirectAttributes redirectAttributes) {
        try {
            libraryService.payLoanFee(loanId);
            redirectAttributes.addFlashAttribute("success", "Loan fee paid successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/" + userId;
    }

    // --- ANALYTICS ---
    @GetMapping("/analytics")
    public String viewAnalytics(Model model) {
        model.addAttribute("booksByGenre", libraryService.getBooksByGenre());
        model.addAttribute("usersByGrade", libraryService.getUsersByGrade());
        model.addAttribute("loansByStatus", libraryService.getLoansByStatus());
        return "analytics";
    }
}
