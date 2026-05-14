package com.library.spd.config;

import com.library.spd.model.Librarian;
import com.library.spd.repository.LibrarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private LibrarianRepository librarianRepository;

    @Override
    public void run(String... args) throws Exception {
        if (librarianRepository.findByNameIgnoreCase("Adipa").isEmpty()) {
            Librarian adipa = new Librarian("Adipa");
            librarianRepository.save(adipa);
        }
    }
}
