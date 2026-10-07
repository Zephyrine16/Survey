package com.example.survey.controller;

import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class SurveyControllerTest {
    @Test
    void bulkDeletionIsDisabledByDefault() {
        var answers = Mockito.mock(AnswerRepository.class);
        var menu = Mockito.mock(MenuItemRepository.class);
        var controller = new AdminMaintenanceController(answers, menu);
        assertEquals(HttpStatus.FORBIDDEN, controller.clearAllData().getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.deleteAllMenuItems().getStatusCode());
        Mockito.verifyNoInteractions(answers, menu);
    }

    @Test
    void explicitlyEnabledDevelopmentMaintenanceDeletesData() {
        var answers = Mockito.mock(AnswerRepository.class);
        var menu = Mockito.mock(MenuItemRepository.class);
        var controller = new AdminMaintenanceController(answers, menu);
        ReflectionTestUtils.setField(controller, "enabled", true);
        assertEquals(HttpStatus.OK, controller.clearAllData().getStatusCode());
        Mockito.verify(answers).deleteAllInBatch();
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteAllMenuItems().getStatusCode());
        Mockito.verify(menu).detachAllAnswersFromMenuItems();
        Mockito.verify(menu).deleteAllInBatch();
    }

    @Test
    void productionDoesNotRegisterBulkDeletionController() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(AdminMaintenanceController.class);
            context.refresh();
            assertTrue(context.getBeansOfType(AdminMaintenanceController.class).isEmpty());
        }
    }
}
