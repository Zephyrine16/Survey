package com.example.survey.config;

import com.example.survey.model.Option;
import com.example.survey.model.Question;
import com.example.survey.repository.OptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.survey.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private SeederProperties seederProperties;

    @Override
    public void run(String... args) throws Exception {
        if(!seederProperties.isEnabled()) {
            log.info("Seeders disabled. Skipping question seed.");
            return;
        }

        // Use JPA to check if the table is empty
        long count = questionRepository.count();

        if (count == 0) {
            log.info("Database is empty. Sending initial survey data safely via JPA...");

            List<SeedQuestion> seedQuestions = loadSeedQuestions();
            if(seedQuestions.isEmpty()) {
                log.warn("No seed questions found at {}", seederProperties.getQuestionsPath());
                return;
            }

            for(SeedQuestion seedQuestion : seedQuestions) {
                if(seedQuestion == null || seedQuestion.text() == null || seedQuestion.text(). isBlank()) {
                    continue;
                }

                Question question = new Question();
                question.setText(seedQuestion.text().trim());
                question.setQuestionType(seedQuestion.questionType());
                question = questionRepository.save(question);

                if(seedQuestion.options() != null) {
                    for(SeedOption option : seedQuestion.options()) {
                        if(option == null || option.label() == null || option.label().isBlank()) {
                            continue;
                        }
                        createOption(option.label().trim(), option.sub(), option.icon(), question);
                    }
                }
            }

            log.info("EYE-DINE Survey data seeded successfully.");
        }
    }

    private void createOption(String label, String subDesc, String icon, Question question) {
        Option opt = new Option();
        opt.setLabel(label);
        opt.setSub_description(subDesc);
        opt.setIcon(icon);
        opt.setQuestion(question);
        optionRepository.save(opt);
    }

    private List<SeedQuestion> loadSeedQuestions() {
        Resource resource = resourceLoader.getResource(seederProperties.getQuestionsPath());
        if(!resource.exists()) {
            return Collections.emptyList();
        }

        try(InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<SeedQuestion>>() {});
        } catch(Exception e) {
            log.error("Failed to load seed questions from {}", seederProperties.getQuestionsPath(), e);
            return Collections.emptyList();
        }
    }

    private record SeedQuestion(
            String text,
            String questionType,
            List<SeedOption> options
    ) {}

    private record SeedOption(
            String label,
            @com.fasterxml.jackson.annotation.JsonProperty("sub") String sub,
            String icon
    ) {}
}