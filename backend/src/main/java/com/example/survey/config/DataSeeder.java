package com.example.survey.config;

import com.example.survey.model.Option;
import com.example.survey.model.Question;
import com.example.survey.repository.MenuItemRepository;
import com.example.survey.repository.OptionRepository;
import com.example.survey.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Inject your JPA Repositories
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Fix any existing text for Question 5
        jdbcTemplate.execute("UPDATE questions SET text = 'How would you describe this dish to the AI chatbot?' WHERE question_type = 'TEXT'");

        // Use JPA to check if the table is empty
        long count = questionRepository.count();

        if (count == 0) {
            System.out.println("Database is empty. Seeding initial survey data safely via JPA...");

            // --- QUESTION 1 ---
            Question q1 = new Question();
            q1.setText("Which emotion or physical state most strongly makes you want to order this item?");
            q1.setQuestionType("RADIO");
            q1 = questionRepository.save(q1); // Save it to generate the real ID!

            createOption("Stressed/Overwhelmed", "(I need comfort/energy)", "😩", q1);
            createOption("Happy/Celebratory", "(I want to reward myself)", "😊", q1);
            createOption("Tired/Low Energy", "(I need a boost/caffeine)", "😴", q1);
            createOption("Relaxed/Chilling", "(I want to enjoy the rooftop/alfresco vibe)", "😌", q1);
            createOption("Focused/Working", "(I need something light/non-distracting)", "🎯", q1);

            // --- QUESTION 2 ---
            Question q2 = new Question();
            q2.setText("In what weather condition does this item feel most satisfying?");
            q2.setQuestionType("RADIO");
            q2 = questionRepository.save(q2);

            createOption("Hot/Sunny", "(Refreshing/Cold items)", "☀️", q2);
            createOption("Cold/Rainy", "(Warm/Hearty items)", "🌧️", q2);
            createOption("Any Weather", "(All-around staples)", "⛅", q2);

            // --- QUESTION 3 ---
            Question q3 = new Question();
            q3.setText("What is the \"vibe\" of this specific dish?");
            q3.setQuestionType("RADIO");
            q3 = questionRepository.save(q3);

            createOption("Heavy Meal", "(Lunch/Dinner)", "🍽️", q3);
            createOption("Light Snack", "(Pica-pica/Quick bite)", "🥗", q3);
            createOption("Drink/Refreshment", null, "🥤", q3);

            // --- QUESTION 4 ---
            Question q4 = new Question();
            q4.setText("Looking at this item, what do you think is a fair \"Student-Friendly\" price for it?");
            q4.setQuestionType("RADIO");
            q4 = questionRepository.save(q4);

            createOption("Under ₱150", "(Budget)", null, q4);
            createOption("₱150 - ₱249", "(Standard)", null, q4);
            createOption("₱250 and above", "(Premium)", null, q4);

            // --- QUESTION 5 (TEXT) ---
            Question q5 = new Question();
            q5.setText("How would you describe this dish to the AI chatbot?");
            q5.setQuestionType("TEXT");
            questionRepository.save(q5); // No options needed for text!

            System.out.println("EYE-DINE Survey data seeded successfully!");
        }
    }

    // 🛠️ HELPER METHOD: Keeps your code clean and automatically links the Option to the Question
    private void createOption(String label, String subDesc, String icon, Question question) {
        Option opt = new Option();
        opt.setLabel(label);
        opt.setSub_description(subDesc);
        opt.setIcon(icon);
        opt.setQuestion(question); // Automatically maps the true Foreign Key!
        optionRepository.save(opt);
    }
}