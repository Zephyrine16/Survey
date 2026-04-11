package com.example.survey.config;

import com.example.survey.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Override
    public void run(String... args) throws Exception {
        // Fix any existing text for Question 5
        jdbcTemplate.execute("UPDATE questions SET text = 'How would you describe this dish to the AI Chatbot?' WHERE question_type = 'TEXT'");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions", Integer.class);

        if (count != null && count == 0) {
            System.out.println("Database is empty. Seeding initial survey data...");

            // UPDATED SQL: Now includes short_text for questions and icon for options!
            String sql = """
                INSERT INTO questions (text, short_text, question_type) VALUES 
                ('Which emotion or physical state most strongly makes you want to order this item?', 'Which emotion makes you crave it?', 'RADIO'),
                ('In what weather condition does this item feel most satisfying?', 'What weather suits it best?', 'RADIO'),
                ('What is the "vibe" of this specific dish?', 'What''s the vibe of the dish?', 'RADIO'),
                ('Looking at this item, what do you think is a fair "Student-Friendly" price for it?', 'What''s a fair student price?', 'RADIO'),
                ('How would you describe this dish to the AI chatbot?', 'Describe it to an AI.', 'TEXT');
                
                INSERT INTO options (label, sub_description, icon, question_id) VALUES
                ('Stressed/Overwhelmed', '(I need comfort/energy)', '😩', 1),
                ('Happy/Celebratory', '(I want to reward myself)', '😊', 1),
                ('Tired/Low Energy', '(I need a boost/caffeine)', '😴', 1),
                ('Relaxed/Chilling', '(I want to enjoy the rooftop/alfresco vibe)', '😌', 1),
                ('Focused/Working', '(I need something light/non-distracting)', '🎯', 1),
                
                ('Hot/Sunny', '(Refreshing/Cold items)', '☀️', 2),
                ('Cold/Rainy', '(Warm/Hearty items)', '🌧️', 2),
                ('Any Weather', '(All-around staples)', '⛅', 2),
                
                ('Heavy Meal', '(Lunch/Dinner)', '🍽️', 3),
                ('Light Snack', '(Pica-pica/Quick bite)', '🥗', 3),
                ('Drink/Refreshment', NULL, '🥤', 3),
                
                ('Under ₱150', '(Budget)', NULL, 4),
                ('₱150 - ₱249', '(Standard)', NULL, 4),
                ('₱250 and above', '(Premium)', NULL, 4);
            """;

            jdbcTemplate.execute(sql);
            System.out.println("EYE-DINE Survey data seeded successfully!");
        }
    }
}