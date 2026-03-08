package com.example.survey.config;

import com.example.survey.model.MenuItem;
import com.example.survey.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Override
    public void run(String... args) throws Exception {
        // First, check if the table is already populated so we don't insert duplicates
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions", Integer.class);

        if (count != null && count == 0) {
            System.out.println("Database is empty. Seeding initial survey data...");

            String sql = """
                INSERT INTO questions (text, question_type) VALUES 
                ('Which emotion or physical state most strongly makes you want to order this item?', 'RADIO'),
                ('In what weather condition does this item feel most satisfying?', 'RADIO'),
                ('What is the "vibe" of this specific dish?', 'RADIO'),
                ('Looking at this item, what do you think is a fair "Student-Friendly" price for it?', 'RADIO'),
                ('If you had to describe this dish to the AI Chatbot in 3 words, what would they be?', 'TEXT');
                
                INSERT INTO options (label, sub_description, question_id) VALUES
                ('Stressed/Overwhelmed', '(I need comfort/energy)', 1),
                ('Happy/Celebratory', '(I want to reward myself)', 1),
                ('Tired/Low Energy', '(I need a boost/caffeine)', 1),
                ('Relaxed/Chilling', '(I want to enjoy the rooftop/alfresco vibe)', 1),
                ('Focused/Working', '(I need something light/non-distracting)', 1),
                ('Hot/Sunny', '(Refreshing/Cold items)', 2),
                ('Cold/Rainy', '(Warm/Hearty items)', 2),
                ('Any Weather', '(All-around staples)', 2),
                ('Heavy Meal', '(Lunch/Dinner)', 3),
                ('Light Snack', '(Pica-pica/Quick bite)', 3),
                ('Drink/Refreshment', NULL, 3),
                ('Under ₱150', '(Budget)', 4),
                ('₱150 - ₱249', '(Standard)', 4),
                ('₱250 and above', '(Premium)', 4);
            """;

            jdbcTemplate.execute(sql);
            System.out.println("EYE-DINE Survey data seeded successfully!");
        }

        if(menuItemRepository.count() == 0) {
            System.out.println("Seeding menu items...");

            List<MenuItem> meals = Arrays.asList(
                    new MenuItem("Chicken Creamy Mushroom n Aglio Olio Rice", "A comforting classic.", "Meal", 150.00, "placeholder.jpg"),
                    new MenuItem("Salisbury Steak n Mushroom Sauce", "Savory and filling.", "Meal", 180.00, "placeholder.jpg")
            );

            menuItemRepository.saveAll(meals);
            System.out.println("Meals seeded successfully!");
        }
    }
}