package com.example.survey.config;

import com.example.survey.model.User;
import com.example.survey.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public UserSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if the database has 0 users. If so, create one!
        if (userRepository.count() == 0) {
            User dummyUser = new User();
            dummyUser.setUsername("testuser");
            dummyUser.setEmail("test@example.com");

            userRepository.save(dummyUser);
            System.out.println("✅ Successfully created dummy User #1 for testing!");
        }
    }
}