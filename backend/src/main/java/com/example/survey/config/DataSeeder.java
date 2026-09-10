package com.example.survey.config;

import com.example.survey.model.Option;
import com.example.survey.model.Question;
import com.example.survey.repository.OptionRepository;
import com.example.survey.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.jspecify.annotations.NullMarked;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@NullMarked
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SEED_KEY = "survey_questions";

    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ObjectMapper objectMapper;
    private final SeederProperties seederProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        if(!seederProperties.isEnabled()) {
            log.info("Seeders disabled. Skipping question seed.");
            return;
        }

        // Ensure the tracking table exists before we query it.
        ensureMetadataTable();

        // Only seed once — if a seed_metadata row already exists for "survey_questions"
        // the admin may have intentionally deleted questions; we must not re-add them.
        Integer alreadySeeded = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seed_metadata WHERE seed_key = ?",
                Integer.class, SEED_KEY);
        if (alreadySeeded != null && alreadySeeded > 0) {
            log.info("Survey questions already seeded previously. Skipping.");
            return;
        }

        // If questions already exist in DB (e.g. seeded before this logic was added),
        // record that fact and skip — don't duplicate them.
        long count = questionRepository.count();
        if (count > 0) {
            log.info("Survey questions already present in database. Recording seed marker and skipping.");
            jdbcTemplate.update(
                    "INSERT INTO seed_metadata (seed_key) VALUES (?) ON CONFLICT (seed_key) DO NOTHING",
                    SEED_KEY);
            return;
        }

        log.info("Database is empty. Seeding initial survey questions via JPA...");

        List<SeedQuestion> seedQuestions = loadSeedQuestions();
        if(seedQuestions.isEmpty()) {
            log.warn("No seed questions found! Check if seed/questions.json exists in resources.");
            return;
        }

        for(SeedQuestion seedQuestion : seedQuestions) {
            if(seedQuestion.text() == null || seedQuestion.text().isBlank()) {
                continue;
            }

            Question question = new Question();
            question.setText(seedQuestion.text().trim());
            question.setQuestionType(seedQuestion.questionType());
            question = questionRepository.save(question);

            if (seedQuestion.options() != null) {
                for(SeedOption option : seedQuestion.options()) {
                    if(option.label() == null || option.label().isBlank()) {
                        continue;
                    }
                    createOption(option.label().trim(), option.sub(), option.icon(), question);
                }
            }
        }

        // Mark seeding as done so it never repeats on restart
        jdbcTemplate.update(
                "INSERT INTO seed_metadata (seed_key) VALUES (?) ON CONFLICT (seed_key) DO NOTHING",
                SEED_KEY);

        log.info("EYE-DINE Survey questions seeded successfully.");
    }

    private void ensureMetadataTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS seed_metadata (" +
                "    seed_key  VARCHAR(100) PRIMARY KEY," +
                "    seeded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")"
        );
    }

    private void createOption(String label, @org.jspecify.annotations.Nullable String subDesc, @org.jspecify.annotations.Nullable String icon, Question question) {
        Option option = new Option();
        option.setLabel(label);
        option.setSubDescription(subDesc);
        option.setIcon(icon);
        option.setQuestion(question);
        optionRepository.save(option);
    }

    private List<SeedQuestion> loadSeedQuestions() {
        ClassPathResource resource = new ClassPathResource("seed/questions.json");

        if(!resource.exists()) {
            return Collections.emptyList();
        }

        try(InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch(Exception e) {
            log.error("Failed to load seed questions", e);
            return Collections.emptyList();
        }
    }

    private record SeedQuestion(
            @org.jspecify.annotations.Nullable String text,
            String questionType,
            @org.jspecify.annotations.Nullable List<SeedOption> options
    ) {}

    private record SeedOption(
            @org.jspecify.annotations.Nullable String label,
            @com.fasterxml.jackson.annotation.JsonProperty("sub") @org.jspecify.annotations.Nullable String sub,
            @org.jspecify.annotations.Nullable String icon
    ) {}
}