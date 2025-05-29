package org.example;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class CaseOpener {

    private static final String DB_URL = "jdbc:postgresql://localhost:#/#";
    private static final String DB_USER = "#";
    private static final String DB_PASSWORD = "#";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Scanner scanner = new Scanner(System.in);
            boolean continueChoosing = true;

            while (continueChoosing) {
                List<String> cases = getCases(connection);
                System.out.println("Доступные кейсы:");
                for (int i = 0; i < cases.size(); i++) {
                    System.out.println((i + 1) + ": " + cases.get(i));
                }

                System.out.println("Выберите номер кейса, который хотите открыть:");
                int selectedCaseIndex = scanner.nextInt() - 1;
                scanner.nextLine();

                if (selectedCaseIndex >= 0 && selectedCaseIndex < cases.size()) {
                    String selectedCase = cases.get(selectedCaseIndex);
                    List<Item> items = getItemsInCase(connection, selectedCase);

                    System.out.println("Предметы в кейсе '" + selectedCase + "':");
                    for (Item item : items) {
                        System.out.println(item.name + " - " + item.rarity + " (" + item.dropChance + "%)");
                    }

                    System.out.println("Хотите открыть этот кейс? (да/нет)");
                    String choice = scanner.nextLine().trim().toLowerCase();

                    if (choice.equals("да")) {
                        String item = openCase(connection, selectedCase);
                        System.out.println("Вы получили: " + item);
                        continueChoosing = false;
                    } else if (choice.equals("нет")) {
                        continueChoosing = true;
                    } else {
                        System.out.println("Неверный ввод. Пожалуйста, введите 'да' или 'нет'.");
                    }
                } else {
                    System.out.println("Неверный выбор кейса.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getCases(Connection connection) throws SQLException {
        List<String> cases = new ArrayList<>();
        String query = "SELECT name FROM cases";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                cases.add(rs.getString("name"));
            }
        }
        return cases;
    }

    private static List<Item> getItemsInCase(Connection connection, String caseName) throws SQLException {
        List<Item> items = new ArrayList<>();
        String query = "SELECT name, rarity, drop_chance FROM items WHERE case_id = (SELECT id FROM cases WHERE name = ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, caseName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(new Item(rs.getString("name"), rs.getString("rarity"), rs.getDouble("drop_chance")));
            }
        }
        return items;
    }

    private static String openCase(Connection connection, String caseName) throws SQLException {
        String query = "SELECT name, drop_chance FROM items WHERE case_id = (SELECT id FROM cases WHERE name = ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, caseName);
            ResultSet rs = pstmt.executeQuery();

            List<Item> items = new ArrayList<>();
            while (rs.next()) {
                items.add(new Item(rs.getString("name"), "", rs.getDouble("drop_chance")));
            }

            if (!items.isEmpty()) {
                double randomValue = new Random().nextDouble() * 100;
                double cumulativeProbability = 0.0;
                for (Item item : items) {
                    cumulativeProbability += item.dropChance;
                    if (randomValue <= cumulativeProbability) {
                        return item.name;
                    }
                }
            }
        }
        return "Ничего не найдено";
    }

    static class Item {
        String name;
        String rarity;
        double dropChance;

        Item(String name, String rarity, double dropChance) {
            this.name = name;
            this.rarity = rarity;
            this.dropChance = dropChance;
        }
    }
}