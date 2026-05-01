package com.example.survey.service;

import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class SurveyService {

    @Autowired
    private AnswerRepository answerRepository;

    // This annotation locks the transaction at the database level.
    // It guarantees the count check and inserts happen atomically.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean saveSurveyIfUnderLimit(List<CategorySubmissionDTO> payload) {

        Long totalParticipants = answerRepository.countTotalParticipants();
        if(totalParticipants != null && totalParticipants >= 30) {
            return false;
        }

        for(CategorySubmissionDTO dto : payload) {
            if(dto.getTextResponse() != null && !dto.getTextResponse().isEmpty()) {
                String cleanText = HtmlUtils.htmlUnescape(dto.getTextResponse());
                if(cleanText.length() > 250) {
                    cleanText = cleanText.substring(0, 250);
                }
                dto.setTextResponse(cleanText);
            }

            answerRepository.saveRawAnswer(
                    dto.getUserId(),
                    dto.getMenuItemId(),
                    dto.getQuestionId(),
                    dto.getSelectedOptionId(),
                    dto.getTextResponse()
            );
        }

        return true;
    }
}
