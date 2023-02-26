import com.ibm.db2.jcc.DB2Driver;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Scanner;
import java.sql.*;

public class Soccer {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static Connection connection;

    public static void main(String[] args) throws SQLException {
        connectToDatabase();
        boolean shouldExit = false;
        while (!shouldExit) {
            shouldExit = handleMainMenuInput();
        }

        System.out.println("Exiting application");
        if (!connection.isClosed()) {
            connection.close();
        }
    }

    private static void connectToDatabase() throws SQLException {
        DriverManager.registerDriver(new DB2Driver());
        connection = DriverManager.getConnection("jdbc:db2://winter2023-comp421.cs.mcgill.ca:50000/cs421", "cs421g54", "Z4VbUw3pq54");
    }

    private static boolean handleMainMenuInput() {
        boolean shouldExit = false;
        switch (getFinalMainMenuInput()) {
            case EXIT:
                return true;
            case LIST_MATCHES:
                while (!shouldExit) {
                    shouldExit = handleListMatches();
                }
                break;
            case TODO:
                while (!shouldExit) {
                    shouldExit = handleTODO();
                }
                break;
            case INSERT_PLAYER:
                while (!shouldExit) {
                    shouldExit = handleInsertPlayer();
                }
                break;
        }
        return false;
    }

    // TODO
    private static boolean handleTODO() {
        return true;
    }

    // TODO
    private static boolean handleInsertPlayer() {
        return true;
    }

    private static boolean handleListMatches() {
        switch (getListMatchesInput().toUpperCase(Locale.ROOT)) {
            case "A":
                performListMatches();
                break;
            case "P":
                return true;
            default:
                System.out.println("Invalid input, try again");
        }
        return false;
    }

    private static void performListMatches() {
        System.out.print("Enter the country: ");
        final String country = SCANNER.nextLine();
        while (!listMatches(country)) {
            System.out.println("Failed to execute query");
        }
    }

    private static boolean listMatches(String country) {
        try {
            final Statement statement = connection.createStatement();
            final ResultSet rs = statement.executeQuery(String.format("select m.COUNTRY1,\n" +
                                                                      "       m.COUNTRY2,\n" +
                                                                      "       m.DATE,\n" +
                                                                      "       m.ROUND,\n" +
                                                                      "       case\n" +
                                                                      "           when m.LENGTH is not null then\n" +
                                                                      "               coalesce(goals1.goals_scored, 0)\n" +
                                                                      "           else goals1.goals_scored end goals1,\n" +
                                                                      "       case\n" +
                                                                      "           when m.LENGTH is not null then\n" +
                                                                      "               coalesce(goals2.goals_scored, 0)\n" +
                                                                      "           else goals2.goals_scored end goals2,\n" +
                                                                      "       coalesce(seats_bought, 0)        seats_sold\n" +
                                                                      "from MATCH m\n" +
                                                                      "         left join (select m.MATCH_NUMBER, count(*) seats_bought\n" +
                                                                      "                    from MATCH m\n" +
                                                                      "                             join PURCHASE p on m.MATCH_NUMBER = p.MATCH_NUMBER\n" +
                                                                      "                             join SELECTED s on p.EMAIL = s.EMAIL and p.PID = s.PID\n" +
                                                                      "                    group by m.MATCH_NUMBER) seats on m.MATCH_NUMBER = seats.MATCH_NUMBER\n" +
                                                                      "         left join (select g.MATCH_NUMBER, count(*) goals_scored\n" +
                                                                      "                    from MATCH m\n" +
                                                                      "                             join GOAL g on m.MATCH_NUMBER = g.MATCH_NUMBER\n" +
                                                                      "                    where g.COUNTRY = m.COUNTRY1\n" +
                                                                      "                    group by g.MATCH_NUMBER) goals1 on m.MATCH_NUMBER = goals1.MATCH_NUMBER\n" +
                                                                      "         left join (select g.MATCH_NUMBER, count(*) goals_scored\n" +
                                                                      "                    from MATCH m\n" +
                                                                      "                             join GOAL g on m.MATCH_NUMBER = g.MATCH_NUMBER\n" +
                                                                      "                    where g.COUNTRY = m.COUNTRY2\n" +
                                                                      "                    group by g.MATCH_NUMBER) goals2 on m.MATCH_NUMBER = goals2.MATCH_NUMBER\n" +
                                                                      "where m.COUNTRY1 = '%s'\n" +
                                                                      "   or m.COUNTRY2 = '%s'\n" +
                                                                      "order by DATE", country, country));
            System.out.println("[Country one]\t[Country two]\t[Date]\t[Group Round]\t[Goals from team one]\t[Goals from team two]\t[Seats sold]");
            while (rs.next()) {
                Integer goals1 = rs.getInt("goals1");
                goals1 = rs.wasNull() ? null : goals1;
                Integer goals2 = rs.getInt("goals2");
                goals2 = rs.wasNull() ? null : goals2;
                System.out.printf("\t%s\t\t\t%s\t%s\t\t%s\t\t\t%s\t\t\t%s\t\t\t\t%s\n",
                        rs.getString("COUNTRY1"),
                        rs.getString("COUNTRY2"),
                        rs.getDate("DATE"),
                        rs.getString("ROUND"),
                        goals1, goals2, rs.getInt("seats_sold"));
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    private static String getListMatchesInput() {
        System.out.print("Enter 'A' to find matches of a country, 'P' to go to the previous menu: ");
        return SCANNER.nextLine();
    }

    private static MainMenuInput getFinalMainMenuInput() {
        MainMenuInput input = MainMenuInput.EXIT;
        do {
            if (input != MainMenuInput.EXIT) {
                System.out.println("Number must be on of the following options:");
            }
            printMessage();
        } while ((input = tryGetMainMenuInput()) == null);
        return input;
    }

    private static void printMessage() {
        System.out.printf("Soccer Main Menu\n\t\t%d. List information of matches of a country\n\t\t%d. Insert initial player information for a match\n\t\t%d. TODO\n\t\t%d. Exit application\nPlease enter your option: ",
                MainMenuInput.LIST_MATCHES.value,
                MainMenuInput.INSERT_PLAYER.value,
                MainMenuInput.TODO.value,
                MainMenuInput.EXIT.value);
    }

    private static MainMenuInput tryGetMainMenuInput() {
        try {
            final int result = SCANNER.nextInt();
            SCANNER.nextLine();
            return MainMenuInput.getInputFromValue(result);
        } catch (Exception ignored) {
            SCANNER.nextLine();
            return null;
        }
    }

    private enum MainMenuInput {
        LIST_MATCHES(1), INSERT_PLAYER(2), TODO(3), EXIT(4);

        public final int value;

        MainMenuInput(int value) {
            this.value = value;
        }

        public static MainMenuInput getInputFromValue(int value) {
            if (value < Arrays.stream(MainMenuInput.values()).map(e -> e.value).min(Comparator.comparingInt(o -> o)).orElse(Integer.MIN_VALUE)
                || value > Arrays.stream(MainMenuInput.values()).map(e -> e.value).max(Comparator.comparingInt(o -> o)).orElse(Integer.MAX_VALUE)) {
                return null;
            }

            if (value == LIST_MATCHES.value) {
                return LIST_MATCHES;
            } else if (value == INSERT_PLAYER.value) {
                return INSERT_PLAYER;
            } else if (value == TODO.value) {
                return TODO;
            } else if (value == EXIT.value) {
                return EXIT;
            }

            return null;
        }
    }
}
