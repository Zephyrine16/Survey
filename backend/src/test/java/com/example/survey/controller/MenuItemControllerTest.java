package com.example.survey.controller;

import com.example.survey.config.SurveyProperties;
import com.example.survey.model.MenuItem;
import com.example.survey.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuItemControllerTest {

    private MenuItemRepository menuItemRepository;
    private SurveyProperties surveyProperties;
    private MenuItemController controller;

    @BeforeEach
    void setUp() {
        menuItemRepository = Mockito.mock(MenuItemRepository.class);
        surveyProperties = new SurveyProperties();
        surveyProperties.setItemRespondentLimit(30L);
        controller = new MenuItemController(menuItemRepository, surveyProperties);
    }

    @Test
    void testGetAllMenuItemsDefaultReturnsAll() {
        MenuItem item1 = new MenuItem();
        item1.setId(1L);
        item1.setName("Adobo");
        when(menuItemRepository.findAll()).thenReturn(List.of(item1));

        List<MenuItem> result = controller.getAllMenuItems(false);
        assertEquals(1, result.size());
        assertEquals("Adobo", result.get(0).getName());
        verify(menuItemRepository).findAll();
    }

    @Test
    void testGetAllMenuItemsAvailableOnlyFiltersOverLimit() {
        MenuItem item1 = new MenuItem();
        item1.setId(2L);
        item1.setName("Sinigang");
        when(menuItemRepository.findAvailableMenuItems(30L)).thenReturn(List.of(item1));

        List<MenuItem> result = controller.getAllMenuItems(true);
        assertEquals(1, result.size());
        assertEquals("Sinigang", result.get(0).getName());
        verify(menuItemRepository).findAvailableMenuItems(30L);
    }

    @Test
    void testGetAvailableMenuItems() {
        MenuItem item1 = new MenuItem();
        item1.setId(3L);
        item1.setName("Kare-Kare");
        when(menuItemRepository.findAvailableMenuItems(30L)).thenReturn(List.of(item1));

        List<MenuItem> result = controller.getAvailableMenuItems();
        assertEquals(1, result.size());
        assertEquals("Kare-Kare", result.get(0).getName());
        verify(menuItemRepository).findAvailableMenuItems(30L);
    }
}
