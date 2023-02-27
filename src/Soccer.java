import com.ibm.db2.jcc.DB2Driver;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

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
            case SEE_MONEY_SPENT_FOR_TEAM:
                while (!shouldExit) {
                    shouldExit = handleSeeMoneySpentForTeam();
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

    private static boolean handleSeeMoneySpentForTeam() {
        switch (getSeeMoneySpentForTeamInput().toUpperCase(Locale.ROOT)) {
            case "A":
                performSeeMoneySpentForTeam();
                break;
            case "P":
                return true;
            default:
                System.out.println("Invalid input, try again");
        }
        return false;
    }

    private static void performSeeMoneySpentForTeam() {
        System.out.println("Clients:");
        final Map<String, String> clientMap = new HashMap<>();
        try {
            final Statement statement = connection.createStatement();
            final ResultSet rs = statement.executeQuery("select EMAIL, NAME from CLIENT order by NAME;");

            while (rs.next()) {
                final String email = rs.getString("EMAIL");
                final String name = rs.getString("NAME");
                clientMap.put(email, name);
                System.out.printf("\temail:\t%s\t\tname:%s\n", email, name);
            }
            statement.close();
        } catch (Exception ignored) {
        }
        try {
            System.out.println("\nCountries:");
            final Statement statement = connection.createStatement();
            final ResultSet rs = statement.executeQuery("select COUNTRY from TEAM order by COUNTRY;");

            while (rs.next()) {
                System.out.printf("%s\n", rs.getString("COUNTRY"));
            }
            statement.close();
        } catch (Exception ignored) {
        }

        System.out.print("\nEnter the client's email: ");
        final String email = SCANNER.nextLine();
        System.out.print("Enter the country: ");
        final String country = SCANNER.nextLine();

        try {
            final Statement statement = connection.createStatement();
            final ResultSet rs = statement.executeQuery(String.format("SELECT coalesce(sum(pr.PRICE), 0) MONEY_SPENT\n" +
                                                                      "from CLIENT c\n" +
                                                                      "         join PURCHASE p on c.EMAIL = p.EMAIL\n" +
                                                                      "         join SELECTED s on p.EMAIL = s.EMAIL and p.PID = s.PID\n" +
                                                                      "         join SEAT s2 on s.NAME = s2.NAME and s.NUMBER = s2.NUMBER\n" +
                                                                      "         join MATCH m on p.MATCH_NUMBER = m.MATCH_NUMBER\n" +
                                                                      "         join PRICE pr on s2.NAME = pr.NAME and s2.NUMBER = pr.NUMBER and m.MATCH_NUMBER = pr.MATCH_NUMBER\n" +
                                                                      "where c.EMAIL = '%s'\n" +
                                                                      "  and (m.COUNTRY1 = '%s' or m.COUNTRY2 = '%s');", email, country, country));

            if (rs.next()) {
                System.out.printf("\nTotal money spent by %s to see %s: $%s\n",
                        clientMap.get(email),
                        country,
                        BigDecimal.valueOf(rs.getInt("MONEY_SPENT"), 2));
            }
            statement.close();
        } catch (Exception ignored) {
        }
    }

    private static String getSeeMoneySpentForTeamInput() {
        System.out.print("Enter 'A' to see the money spent by a client to see matches from a specific team or 'P' to go to the previous menu: ");
        return SCANNER.nextLine();
    }

    private static boolean handleInsertPlayer() {
        printMatchesWithinNextThreeDays();
        switch (getFirstInsertPlayerInput().toUpperCase(Locale.ROOT)) {
            case "A":
                System.out.print("Enter the match number: ");
                final int matchNumber = SCANNER.nextInt();
                SCANNER.nextLine();
                System.out.print("Enter the country: ");
                final String country = SCANNER.nextLine();
                boolean shouldExit = false;
                while (!shouldExit) {
                    shouldExit = handleInsertingPlayer(matchNumber, country);
                }
                break;
            case "P":
                return true;
            default:
                System.out.println("Invalid input, try again");
        }
        return false;
    }

    private static void printMatchesWithinNextThreeDays() {
        System.out.println("Matches:");
        final Statement statement;
        try {
            statement = connection.createStatement();

            final Date today = new Date(System.currentTimeMillis());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today);
            calendar.add(Calendar.DATE, 3);
            final Date dateInThreeDays = new Date(calendar.getTimeInMillis());

            final ResultSet rs = statement.executeQuery(String.format("select MATCH_NUMBER, COUNTRY1, COUNTRY2, DATE, ROUND\n" +
                                                                      "    from MATCH\n" +
                                                                      "where DATE >= '%s' and DATE <= '%s' and COUNTRY2 is not null and COUNTRY1 is not null\n" +
                                                                      "order by DATE;", today, dateInThreeDays));
            while (rs.next()) {
                System.out.printf("\t\t%d\t%s\t%s\t%s\t%s\n",
                        rs.getInt("MATCH_NUMBER"),
                        rs.getString("COUNTRY1"),
                        rs.getString("COUNTRY2"),
                        rs.getDate("DATE"),
                        rs.getString("ROUND"));
            }
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean handleInsertingPlayer(int matchNumber, String country) {
        System.out.printf("The following players from %s are already entered for match %d:\n", country, matchNumber);
        int count = 0;
        try {
            final Statement statement = connection.createStatement();
            final ResultSet rs = statement.executeQuery(String.format("select NAME, NUMBER, DETAILED_POSITION, MINUTE_ENTERED, MINUTE_EXITED, YELLOW_CARDS, RECEIVED_RED_CARD\n" +
                                                                      "from PLAYIN pl\n" +
                                                                      "         join PLAYER p on pl.COUNTRY = p.COUNTRY and pl.PID = p.PID\n" +
                                                                      "where MATCH_NUMBER = %d\n" +
                                                                      "  and p.COUNTRY = '%s'\n" +
                                                                      "order by NAME;", matchNumber, country));

            while (rs.next()) {
                count++;
                Integer toMinute = rs.getInt("MINUTE_EXITED");
                toMinute = rs.wasNull() ? null : toMinute;

                System.out.printf("\t%s\t%d\t%s\tfrom minute %d\tto minute %s\tyellow: %d\tred: %b\n",
                        rs.getString("NAME"),
                        rs.getInt("NUMBER"),
                        rs.getString("DETAILED_POSITION"),
                        rs.getInt("MINUTE_ENTERED"),
                        toMinute,
                        rs.getInt("YELLOW_CARDS"),
                        rs.getBoolean("RECEIVED_RED_CARD"));
            }
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (count < 3) {
            List<Integer> pids = new ArrayList<>();
            System.out.printf("\nPossible players from %s not yet selected:\n", country);
            try {
                final Statement statement = connection.createStatement();
                final ResultSet rs = statement.executeQuery(String.format("select PID, NAME, NUMBER, POSITION\n" +
                                                                          "from PLAYER p\n" +
                                                                          "where p.COUNTRY = '%s'\n" +
                                                                          "except\n" +
                                                                          "select p.PID, NAME, NUMBER, p.POSITION\n" +
                                                                          "from PLAYIN pl\n" +
                                                                          "         join PLAYER p on pl.COUNTRY = p.COUNTRY and pl.PID = p.PID\n" +
                                                                          "where MATCH_NUMBER = %d\n" +
                                                                          "  and p.COUNTRY = '%s'\n" +
                                                                          "order by NAME;", country, matchNumber, country));

                int c = 1;
                while (rs.next()) {
                    pids.add(rs.getInt("PID"));
                    System.out.printf("%d.\t%s\t%d\t%s\n",
                            c,
                            rs.getString("NAME"),
                            rs.getInt("NUMBER"),
                            rs.getString("POSITION"));
                    c++;
                }
                statement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            System.out.print("\nEnter the number of the player you want to insert or 'P' to go to the previous menu: ");
            final String input = SCANNER.nextLine().toUpperCase(Locale.ROOT);
            if (input.equals("P")) {
                return true;
            } else {
                final int pid = pids.get(Integer.parseInt(input) - 1);
                System.out.print("Enter the player's detailed position: ");
                final String detailedPosition = SCANNER.nextLine();
                try {
                    final Statement statement = connection.createStatement();
                    statement.executeUpdate(String.format("insert into PLAYIN (PID, COUNTRY, MATCH_NUMBER, YELLOW_CARDS, RECEIVED_RED_CARD, DETAILED_POSITION, MINUTE_ENTERED,\n" +
                                                          "                    MINUTE_EXITED)\n" +
                                                          "values (%d, '%s', %d, 0, false, '%s', 0, null)", pid, country, matchNumber, detailedPosition));
                    statement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.out.println("\nOnly 3 players can play in one game.\n");
            return true;
        }
        return false;
    }

    private static String getFirstInsertPlayerInput() {
        System.out.print("Enter 'A' to insert a player, 'P' to go to the previous menu: ");
        return SCANNER.nextLine();
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
            statement.close();
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
        System.out.printf("Soccer Main Menu\n\t\t%d. List information of matches of a country\n\t\t%d. Insert initial player information for a match\n\t\t%d. See total money spent by a client to see a specific team play\n\t\t%d. Exit application\nPlease enter your option: ",
                MainMenuInput.LIST_MATCHES.value,
                MainMenuInput.INSERT_PLAYER.value,
                MainMenuInput.SEE_MONEY_SPENT_FOR_TEAM.value,
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
        LIST_MATCHES(1), INSERT_PLAYER(2), SEE_MONEY_SPENT_FOR_TEAM(3), EXIT(4);

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
            } else if (value == SEE_MONEY_SPENT_FOR_TEAM.value) {
                return SEE_MONEY_SPENT_FOR_TEAM;
            } else if (value == EXIT.value) {
                return EXIT;
            }

            return null;
        }
    }
}
