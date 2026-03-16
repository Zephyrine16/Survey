package com.example.survey.config;

import com.example.survey.model.MenuItem;
import com.example.survey.repository.MenuItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MenuSeeder implements CommandLineRunner {
    private final MenuItemRepository menuItemRepository;

    public MenuSeeder(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if(menuItemRepository.count() == 0) {
            List<MenuItem> fullMenu = new ArrayList<>();

            // MEALS
            fullMenu.add(createItem("Chicken Creamy Mushroom n Aglio Olio Rice", "Meal"));
            fullMenu.add(createItem("Salisbury Steak n Mushroom Sauce", "Meal"));
            fullMenu.add(createItem("Fish Fillet n Buttermilk Sauce", "Meal"));
            fullMenu.add(createItem("Crispy Chicken Fingers n Aglio Olio Rice", "Meal"));
            fullMenu.add(createItem("Hungarian Sausage n Aglio Olio Rice", "Meal"));
            fullMenu.add(createItem("Corned Beef and Eggs", "Meal"));
            fullMenu.add(createItem("Spam n Eggs", "Meal"));
            fullMenu.add(createItem("Italian Meatballs n Basil Tomato Sauce", "Meal"));

            // BREAD
            fullMenu.add(createItem("Bacon Sandwich", "Bread"));
            fullMenu.add(createItem("Ham Sandwich", "Bread"));
            fullMenu.add(createItem("Spam Sandwich", "Bread"));
            fullMenu.add(createItem("Chicken Sandwich", "Bread"));
            fullMenu.add(createItem("Pepperoni Cheese Panini", "Bread"));
            fullMenu.add(createItem("Chicken Pesto Panini", "Bread"));

            // PASTA
            fullMenu.add(createItem("Aglio e Olio", "Pasta"));
            fullMenu.add(createItem("Chicken Alfredo", "Pasta"));
            fullMenu.add(createItem("Tomato Pesto", "Pasta"));
            fullMenu.add(createItem("Carbonara", "Pasta"));
            fullMenu.add(createItem("Spaghetti", "Pasta"));
            fullMenu.add(createItem("Tuna Pesto", "Pasta"));

            // WAFFLE
            fullMenu.add(createItem("Classic Waffle", "Waffle"));
            fullMenu.add(createItem("Apple Waffle", "Waffle"));
            fullMenu.add(createItem("Biscoff Waffle", "Waffle"));
            fullMenu.add(createItem("Ice Cream Waffle", "Waffle"));
            fullMenu.add(createItem("Blueberry Waffle", "Waffle"));
            fullMenu.add(createItem("Cheesy Waffle", "Waffle"));
            fullMenu.add(createItem("Choco Waffle", "Waffle"));
            fullMenu.add(createItem("Mango Waffle", "Waffle"));
            fullMenu.add(createItem("Bacon Waffle", "Waffle"));
            fullMenu.add(createItem("Nutella Waffle", "Waffle"));
            fullMenu.add(createItem("Strawberry Waffle", "Waffle"));

            // COFFEE
            fullMenu.add(createItem("Hot Americano", "Coffee"));
            fullMenu.add(createItem("Iced Americano", "Coffee"));
            fullMenu.add(createItem("Hot Caffe Latte", "Coffee"));
            fullMenu.add(createItem("Iced Caffe Latte", "Coffee"));
            fullMenu.add(createItem("Hot Vanilla Latte", "Coffee"));
            fullMenu.add(createItem("Iced Vanilla Latte", "Coffee"));
            fullMenu.add(createItem("Hot Spanish Latte", "Coffee"));
            fullMenu.add(createItem("Iced Spanish Latte", "Coffee"));
            fullMenu.add(createItem("Hot Cappuccino", "Coffee"));
            fullMenu.add(createItem("Iced Cappuccino", "Coffee"));
            fullMenu.add(createItem("Hot Caramel Macchiato", "Coffee"));
            fullMenu.add(createItem("Iced Caramel Macchiato", "Coffee"));
            fullMenu.add(createItem("Hot Salted Caramel", "Coffee"));
            fullMenu.add(createItem("Iced Salted Caramel", "Coffee"));
            fullMenu.add(createItem("Hot Cafe Mocha", "Coffee"));
            fullMenu.add(createItem("Iced Cafe Mocha", "Coffee"));
            fullMenu.add(createItem("Hot White Chocolate", "Coffee"));
            fullMenu.add(createItem("Iced White Chocolate", "Coffee"));
            fullMenu.add(createItem("Hot Caramel Oat Latte", "Coffee"));
            fullMenu.add(createItem("Iced Caramel Oat Latte", "Coffee"));
            fullMenu.add(createItem("Iced Biscoff Latte", "Coffee"));
            fullMenu.add(createItem("Iced Shaken Affogato", "Coffee"));

            // NON-COFFEE
            fullMenu.add(createItem("Hot Chocolate", "Non-coffee"));
            fullMenu.add(createItem("Iced Chocolate", "Non-coffee"));
            fullMenu.add(createItem("Hot Matcha", "Non-coffee"));
            fullMenu.add(createItem("Iced Matcha", "Non-coffee"));

            // FRAPPE SERIES
            fullMenu.add(createItem("Caramel Macchiato", "Frappe Series"));
            fullMenu.add(createItem("Salted Caramel", "Frappe Series"));
            fullMenu.add(createItem("Cafe Mocha", "Frappe Series"));
            fullMenu.add(createItem("White Chocolate", "Frappe Series"));
            fullMenu.add(createItem("Coffee Jelly", "Frappe Series"));
            fullMenu.add(createItem("Strawberry Ice cream", "Frappe Series"));
            fullMenu.add(createItem("Vanilla", "Frappe Series"));
            fullMenu.add(createItem("Biscoff", "Frappe Series"));
            fullMenu.add(createItem("Chocolate Chip Cream", "Frappe Series"));
            fullMenu.add(createItem("Matcha", "Frappe Series"));
            fullMenu.add(createItem("Caramel Oreo", "Frappe Series"));
            fullMenu.add(createItem("Mango Cheesecake", "Frappe Series"));
            fullMenu.add(createItem("Avocado Creamcheese", "Frappe Series"));

            // FLOAT
            fullMenu.add(createItem("Caramel Float Cereal", "Float"));
            fullMenu.add(createItem("Matcha Float", "Float"));
            fullMenu.add(createItem("Chocolate Float", "Float"));
            fullMenu.add(createItem("Mocha Float", "Float"));
            fullMenu.add(createItem("Salted Caramel Float", "Float"));

            // MILKTEA
            fullMenu.add(createItem("Okinawa", "Milktea"));
            fullMenu.add(createItem("Cookies and Cream", "Milktea"));
            fullMenu.add(createItem("Wintermelon", "Milktea"));
            fullMenu.add(createItem("Chocolate", "Milktea"));
            fullMenu.add(createItem("Cheese Cake", "Milktea"));

            // SPARKLING SODA
            fullMenu.add(createItem("Blue Ocean", "Sparkling Soda"));
            fullMenu.add(createItem("Sunset Dream", "Sparkling Soda"));
            fullMenu.add(createItem("Strawberry Yakult", "Sparkling Soda"));
            fullMenu.add(createItem("Peach Soda", "Sparkling Soda"));
            fullMenu.add(createItem("Green Apple", "Sparkling Soda"));
            fullMenu.add(createItem("Blueberry", "Sparkling Soda"));

            // FRUIT TEA
            fullMenu.add(createItem("Peach Tea", "Fruit Tea"));
            fullMenu.add(createItem("Lemon Fruit Tea", "Fruit Tea"));
            fullMenu.add(createItem("Strawberry Fruit Tea", "Fruit Tea"));
            fullMenu.add(createItem("Kiwi Fruit Tea", "Fruit Tea"));
            fullMenu.add(createItem("Blueberry Fruit Tea", "Fruit Tea"));

            menuItemRepository.saveAll(fullMenu);
            System.out.println("✅ Successfully added " + fullMenu.size() + " items to the database!");
        }
    }

    private MenuItem createItem(String name, String category) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setCategory(category);
        return item;
    }
}
