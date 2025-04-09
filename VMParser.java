import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Handles the parsing of a single .vm file.
 * Reads VM commands, parses them, and provides access to their components.
 * Removes all white space and comments.
 */
public class VMParser {
    private Scanner cmds;
    private String currentCmd;
    private int argType;
    private String argument1;
    private int argument2;

    public static final int ARITHMETIC = 0;
    public static final int PUSH = 1;
    public static final int POP = 2;
    public static final int LABEL = 3;
    public static final int GOTO = 4;
    public static final int IF = 5;
    public static final int FUNCTION = 6;
    public static final int RETURN = 7;
    public static final int CALL = 8;

    private static final ArrayList<String> arithmeticCmds = new ArrayList<>();

    static {
        arithmeticCmds.add("add");
        arithmeticCmds.add("sub");
        arithmeticCmds.add("neg");
        arithmeticCmds.add("eq");
        arithmeticCmds.add("gt");
        arithmeticCmds.add("lt");
        arithmeticCmds.add("and");
        arithmeticCmds.add("or");
        arithmeticCmds.add("not");
    }

    /**
     * Opens the input file and prepares to parse it.
     * 
     * @param fileIn Input .vm file
     */
    public VMParser(File fileIn) {
        argType = -1;
        argument1 = "";
        argument2 = -1;

        try {
            String preprocessed = preprocessFile(fileIn);
            cmds = new Scanner(preprocessed.trim());
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + fileIn.getName());
        }
    }

    /**
     * Preprocesses the file by removing comments and empty lines.
     * 
     * @param fileIn Input .vm file
     * @return Preprocessed content as a single string
     * @throws FileNotFoundException If the file is not found
     */
    private String preprocessFile(File fileIn) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(fileIn);
        StringBuilder preprocessed = new StringBuilder();

        while (fileScanner.hasNextLine()) {
            String line = noComments(fileScanner.nextLine()).trim();
            if (!line.isEmpty()) {
                preprocessed.append(line).append("\n");
            }
        }

        fileScanner.close();
        return preprocessed.toString();
    }

    /**
     * Checks if there are more commands to read.
     * 
     * @return True if more commands are available, false otherwise
     */
    public boolean hasMoreCommands() {
        return cmds.hasNextLine();
    }

    /**
     * Reads the next command from the input and makes it the current command.
     * Should be called only when hasMoreCommands() returns true.
     */
    public void advance() {
        currentCmd = cmds.nextLine();
        argument1 = "";
        argument2 = -1;

        String[] segments = currentCmd.split(" ");
        if (segments.length > 3) {
            throw new IllegalArgumentException("Too many arguments in command: " + currentCmd);
        }

        parseCommand(segments);
    }

    /**
     * Parses the current command and sets its type and arguments.
     * 
     * @param segments Segments of the current command
     */
    private void parseCommand(String[] segments) {
        String command = segments[0];

        if (arithmeticCmds.contains(command)) {
            argType = ARITHMETIC;
            argument1 = command;
        } else if ("return".equals(command)) {
            argType = RETURN;
            argument1 = command;
        } else {
            parseNonArithmeticCommand(segments);
        }
    }

    /**
     * Parses non-arithmetic commands and sets their type and arguments.
     * 
     * @param segments Segments of the current command
     */
    private void parseNonArithmeticCommand(String[] segments) {
        String command = segments[0];
        argument1 = segments[1];

        switch (command) {
            case "push":
                argType = PUSH;
                break;
            case "pop":
                argType = POP;
                break;
            case "label":
                argType = LABEL;
                break;
            case "if-goto":
                argType = IF;
                break;
            case "goto":
                argType = GOTO;
                break;
            case "function":
                argType = FUNCTION;
                break;
            case "call":
                argType = CALL;
                break;
            default:
                throw new IllegalArgumentException("Unknown command type: " + command);
        }

        if (requiresSecondArgument(argType)) {
            parseSecondArgument(segments);
        }
    }

    /**
     * Parses the second argument of the current command.
     * 
     * @param segments Segments of the current command
     */
    private void parseSecondArgument(String[] segments) {
        try {
            argument2 = Integer.parseInt(segments[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Second argument is not an integer: " + segments[2]);
        }
    }

    /**
     * Checks if the command type requires a second argument.
     * 
     * @param commandType Command type
     * @return True if a second argument is required, false otherwise
     */
    private boolean requiresSecondArgument(int commandType) {
        return commandType == PUSH || commandType == POP || commandType == FUNCTION || commandType == CALL;
    }

    /**
     * Returns the type of the current command.
     * 
     * @return Command type
     */
    public int commandType() {
        if (argType != -1) {
            return argType;
        } else {
            throw new IllegalStateException("No command available.");
        }
    }

    /**
     * Returns the first argument of the current command.
     * Should not be called if the command type is RETURN.
     * 
     * @return First argument
     */
    public String arg1() {
        if (commandType() != RETURN) {
            return argument1;
        } else {
            throw new IllegalStateException("Cannot get arg1 from a RETURN command.");
        }
    }

    /**
     * Returns the second argument of the current command.
     * Should be called only if the command type is PUSH, POP, FUNCTION, or CALL.
     * 
     * @return Second argument
     */
    public int arg2() {
        if (requiresSecondArgument(commandType())) {
            return argument2;
        } else {
            throw new IllegalStateException("Cannot get arg2 for this command type.");
        }
    }

    /**
     * Removes comments from a line of code.
     * 
     * @param line Input line
     * @return Line without comments
     */
    public static String noComments(String line) {
        int commentIndex = line.indexOf("//");
        return (commentIndex != -1) ? line.substring(0, commentIndex) : line;
    }

    /**
     * Removes spaces from a string.
     * 
     * @param str Input string
     * @return String without spaces
     */
    public static String noSpaces(String str) {
        return str.replaceAll("\\s+", "");
    }

    /**
     * Extracts the file extension from a filename.
     * 
     * @param fileName Input filename
     * @return File extension
     */
    public static String getExt(String fileName) {
        int index = fileName.lastIndexOf('.');
        return (index != -1) ? fileName.substring(index) : "";
    }
}