package com.library.spd.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class LoggerService {

    public void log(String message) {
        String librarian = "System";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("librarianName") != null) {
                    librarian = (String) session.getAttribute("librarianName");
                }
            }
        } catch (Exception e) {}
        
        LocalDate today = LocalDate.now();
        String filename = "logs/" + today.toString() + ".log";
        
        java.io.File directory = new java.io.File("logs");
        if (!directory.exists()) {
            directory.mkdir();
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(filename, true))) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (new java.io.File(filename).length() == 0) {
                 out.println("Day: " + today.format(DateTimeFormatter.ofPattern("d/MM/yyyy")));
            }
            out.println("[Librarian: " + librarian + "] " + message.replace("{time}", time));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
