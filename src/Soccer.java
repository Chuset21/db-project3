import com.ibm.db2.jcc.DB2Driver;

import java.util.Arrays;
import java.util.Comparator;
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
        switch (getMainMenuInput()) {
            case EXIT:
                return true;
            case TODO: // TODO
            case LIST_MATCHES: // TODO
            case INSERT_PLAYER: // TODO
        }
        return false;
    }

    private static Input getMainMenuInput() {
        Input input = Input.EXIT;
        do {
            if (input != Input.EXIT) {
                System.out.println("Number must be on of the following options:");
            }
            printMessage();
        } while ((input = takeInput()) == null);
        return input;
    }

    private static void printMessage() {
        System.out.printf("Soccer Main Menu\n\t\t%d. List information of matches of a country\n\t\t%d. Insert initial player information for a match\n\t\t%d. TODO\n\t\t%d. Exit application\nPlease enter your option: ",
                Input.LIST_MATCHES.value,
                Input.INSERT_PLAYER.value,
                Input.TODO.value,
                Input.EXIT.value);
    }

    private static Input takeInput() {
        try {
            final int result = SCANNER.nextInt();
            SCANNER.nextLine();
            return Input.getInputFromValue(result);
        } catch (Exception ignored) {
            SCANNER.nextLine();
            return null;
        }
    }

    private enum Input {
        LIST_MATCHES(1), INSERT_PLAYER(2), TODO(3), EXIT(4);

        public final int value;

        Input(int value) {
            this.value = value;
        }

        public static Input getInputFromValue(int value) {
            if (value < Arrays.stream(Input.values()).map(e -> e.value).min(Comparator.comparingInt(o -> o)).orElse(Integer.MIN_VALUE)
                || value > Arrays.stream(Input.values()).map(e -> e.value).max(Comparator.comparingInt(o -> o)).orElse(Integer.MAX_VALUE)) {
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
