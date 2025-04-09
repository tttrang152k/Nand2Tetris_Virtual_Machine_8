import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates VM commands into HACK assembly code
 */
public class ASMWriter {

    private int arthJumpFlag;
    private PrintWriter outPrinter;
    private static final Pattern labelReg = Pattern.compile("^[^0-9][0-9A-Za-z\\_\\:\\.\\$]+");
    private static int labelCnt = 0;
    private static String fileName = "";

    /**
     * Constructor: Opens an output file and prepares to write content
     * 
     * @param fileOut Output file
     */
    public ASMWriter(File fileOut) {
        try {
            fileName = fileOut.getName();
            outPrinter = new PrintWriter(fileOut);
            arthJumpFlag = 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the current file name for static variables
     * 
     * @param fileOut Input file
     */
    public void setFileName(File fileOut) {
        fileName = fileOut.getName();
    }

    /**
     * Writes the assembly code for an arithmetic command
     * 
     * @param command Arithmetic command
     */
    public void writeArithmetic(String command) {
        switch (command) {
            case "add":
                outPrinter.print(arithmeticTemplate1() + "M=D+M\n");
                break;
            case "sub":
                outPrinter.print(arithmeticTemplate1() + "M=M-D\n");
                break;
            case "and":
                outPrinter.print(arithmeticTemplate1() + "M=D&M\n");
                break;
            case "or":
                outPrinter.print(arithmeticTemplate1() + "M=D|M\n");
                break;
            case "gt":
                outPrinter.print(arithmeticTemplate2("JLE"));
                arthJumpFlag++;
                break;
            case "lt":
                outPrinter.print(arithmeticTemplate2("JGE"));
                arthJumpFlag++;
                break;
            case "eq":
                outPrinter.print(arithmeticTemplate2("JNE"));
                arthJumpFlag++;
                break;
            case "not":
                outPrinter.print("@SP\nA=M-1\nM=!M\n");
                break;
            case "neg":
                outPrinter.print("D=0\n@SP\nA=M-1\nM=M-D\n");
                break;
            default:
                throw new IllegalArgumentException("Invalid arithmetic command: " + command);
        }
    }

    /**
     * Writes the assembly code for a push or pop command
     * 
     * @param command PUSH or POP
     * @param segment Memory segment
     * @param index   Index within the segment
     */
    public void writePushPop(int command, String segment, int index) {
        if (command == VMParser.PUSH) {
            writePush(segment, index);
        } else if (command == VMParser.POP) {
            writePop(segment, index);
        } else {
            throw new IllegalArgumentException("Invalid command for writePushPop");
        }
    }

    private void writePush(String segment, int index) {
        switch (segment) {
            case "constant":
                outPrinter.print("@" + index + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
                break;
            case "local":
                outPrinter.print(pushTemplate1("LCL", index, false));
                break;
            case "argument":
                outPrinter.print(pushTemplate1("ARG", index, false));
                break;
            case "this":
                outPrinter.print(pushTemplate1("THIS", index, false));
                break;
            case "that":
                outPrinter.print(pushTemplate1("THAT", index, false));
                break;
            case "temp":
                outPrinter.print("@" + (index + 5) + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
                break;
            case "pointer":
                outPrinter.print(pushPointer(index));
                break;
            case "static":
                outPrinter.print("@" + fileName + index + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
                break;
            default:
                throw new IllegalArgumentException("Invalid segment for push: " + segment);
        }
    }

    private void writePop(String segment, int index) {
        switch (segment) {
            case "local":
                outPrinter.print(popTemplate1("LCL", index, false));
                break;
            case "argument":
                outPrinter.print(popTemplate1("ARG", index, false));
                break;
            case "this":
                outPrinter.print(popTemplate1("THIS", index, false));
                break;
            case "that":
                outPrinter.print(popTemplate1("THAT", index, false));
                break;
            case "temp":
                outPrinter.print("@" + (index + 5) + "\nD=A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
                break;
            case "pointer":
                outPrinter.print(popPointer(index));
                break;
            case "static":
                outPrinter.print("@" + fileName + index + "\nD=A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
                break;
            default:
                throw new IllegalArgumentException("Invalid segment for pop: " + segment);
        }
    }

    private String pushPointer(int index) {
        if (index == 0) {
            return "@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
        } else if (index == 1) {
            return "@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
        } else {
            throw new IllegalArgumentException("Invalid index for pointer: " + index);
        }
    }

    private String popPointer(int index) {
        if (index == 0) {
            return "@SP\nAM=M-1\nD=M\n@THIS\nM=D\n";
        } else if (index == 1) {
            return "@SP\nAM=M-1\nD=M\n@THAT\nM=D\n";
        } else {
            throw new IllegalArgumentException("Invalid index for pointer: " + index);
        }
    }

    /**
     * Writes the assembly code for a label command
     * 
     * @param label Label name
     */
    public void writeLabel(String label) {
        validateLabel(label);
        outPrinter.print("(" + label + ")\n");
    }

    /**
     * Writes the assembly code for a goto command
     * 
     * @param label Label name
     */
    public void writeGoto(String label) {
        validateLabel(label);
        outPrinter.print("@" + label + "\n0;JMP\n");
    }

    /**
     * Writes the assembly code for an if-goto command
     * 
     * @param label Label name
     */
    public void writeIf(String label) {
        validateLabel(label);
        outPrinter.print(arithmeticTemplate1() + "@" + label + "\nD;JNE\n");
    }

    private void validateLabel(String label) {
        Matcher m = labelReg.matcher(label);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid label format: " + label);
        }
    }

    /**
     * Writes the assembly code for VM initialization (bootstrap code)
     */
    public void writeBootstrapInit() {
        outPrinter.print("@256\nD=A\n@SP\nM=D\n");
        writeCall("Sys.init", 0);
    }

    /**
     * Writes the assembly code for a call command
     * 
     * @param functionName Function name
     * @param numArgs      Number of arguments
     */
    public void writeCall(String functionName, int numArgs) {
        String returnLabel = "RETURN_LABEL" + (labelCnt++);
        outPrinter.print("@" + returnLabel + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
        outPrinter.print(pushTemplate1("LCL", 0, true));
        outPrinter.print(pushTemplate1("ARG", 0, true));
        outPrinter.print(pushTemplate1("THIS", 0, true));
        outPrinter.print(pushTemplate1("THAT", 0, true));
        outPrinter.print("@SP\nD=M\n@5\nD=D-A\n@" + numArgs + "\nD=D-A\n@ARG\nM=D\n");
        outPrinter.print("@SP\nD=M\n@LCL\nM=D\n");
        outPrinter.print("@" + functionName + "\n0;JMP\n");
        outPrinter.print("(" + returnLabel + ")\n");
    }

    /**
     * Writes the assembly code for a return command
     */
    public void writeReturn() {
        outPrinter.print(returnTemplate());
    }

    /**
     * Writes the assembly code for a function command
     * 
     * @param functionName Function name
     * @param numLocals    Number of local variables
     */
    public void writeFunction(String functionName, int numLocals) {
        outPrinter.print("(" + functionName + ")\n");
        for (int i = 0; i < numLocals; i++) {
            writePushPop(VMParser.PUSH, "constant", 0);
        }
    }

    /**
     * Closes the output file
     */
    public void close() {
        outPrinter.close();
    }

    // Templates for common assembly code patterns
    private String arithmeticTemplate1() {
        return "@SP\nAM=M-1\nD=M\nA=A-1\n";
    }

    private String arithmeticTemplate2(String type) {
        return "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@FALSE" + arthJumpFlag + "\nD;" + type + "\n" +
                "@SP\nA=M-1\nM=-1\n@CONTINUE" + arthJumpFlag + "\n0;JMP\n" +
                "(FALSE" + arthJumpFlag + ")\n@SP\nA=M-1\nM=0\n(CONTINUE" + arthJumpFlag + ")\n";
    }

    private String pushTemplate1(String segment, int index, boolean isDirect) {
        String noPointerCode = isDirect ? "" : "@" + index + "\nA=D+A\nD=M\n";
        return "@" + segment + "\nD=M\n" + noPointerCode + "@SP\nA=M\nM=D\n@SP\nM=M+1\n";
    }

    private String popTemplate1(String segment, int index, boolean isDirect) {
        String noPointerCode = isDirect ? "D=A\n" : "D=M\n@" + index + "\nD=D+A\n";
        return "@" + segment + "\n" + noPointerCode + "@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n";
    }

    private String returnTemplate() {
        return "@LCL\nD=M\n@R11\nM=D\n@5\nA=D-A\nD=M\n@R12\nM=D\n" +
                popTemplate1("ARG", 0, false) +
                "@ARG\nD=M\n@SP\nM=D+1\n" +
                preFrameTemplate("THAT") +
                preFrameTemplate("THIS") +
                preFrameTemplate("ARG") +
                preFrameTemplate("LCL") +
                "@R12\nA=M\n0;JMP\n";
    }

    private String preFrameTemplate(String position) {
        return "@R11\nD=M-1\nAM=D\nD=M\n@" + position + "\nM=D\n";
    }
}