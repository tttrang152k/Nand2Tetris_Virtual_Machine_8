import java.io.File;
import java.util.ArrayList;

public class VMTranslator {

    /**
     * Return all the .vm files in a directory
     * 
     * @param dir Directory to search for .vm files
     * @return List of .vm files
     */
    public static ArrayList<File> getVMFiles(File dir) {
        File[] files = dir.listFiles();
        ArrayList<File> result = new ArrayList<>();

        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".vm")) {
                    result.add(f);
                }
            }
        }

        return result;
    }

    /**
     * Process a single file or directory and return the list of .vm files
     * 
     * @param fileIn Input file or directory
     * @return List of .vm files
     */
    private static ArrayList<File> processInput(File fileIn) {
        ArrayList<File> vmFiles = new ArrayList<>();

        if (fileIn.isFile()) {
            // Single file: check if it's a .vm file
            if (!VMParser.getExt(fileIn.getAbsolutePath()).equals(".vm")) {
                throw new IllegalArgumentException(".vm file is required!");
            }
            vmFiles.add(fileIn);
        } else if (fileIn.isDirectory()) {
            // Directory: get all .vm files
            vmFiles = getVMFiles(fileIn);
            if (vmFiles.isEmpty()) {
                throw new IllegalArgumentException("No .vm file in this directory");
            }
        }

        return vmFiles;
    }

    /**
     * Generate the output file path based on the input file or directory
     * 
     * @param fileIn  Input file or directory
     * @param vmFiles List of .vm files
     * @return Output file path
     */
    private static String generateOutputFilePath(File fileIn, ArrayList<File> vmFiles) {
        if (fileIn.isFile()) {
            return fileIn.getAbsolutePath().substring(0, fileIn.getAbsolutePath().lastIndexOf(".")) + ".asm";
        } else {
            return fileIn.getAbsolutePath() + "/" + fileIn.getName() + ".asm";
        }
    }

    /**
     * Check if the Sys.vm file exists in the list of .vm files
     * 
     * @param vmFiles List of .vm files
     * @return True if Sys.vm exists, false otherwise
     */
    private static boolean containsSysInit(ArrayList<File> vmFiles) {
        for (File f : vmFiles) {
            if (f.getName().equals("Sys.vm")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Translate the .vm files into assembly code
     * 
     * @param vmFiles List of .vm files
     * @param writer  ASMWriter instance
     */
    private static void translateVMFiles(ArrayList<File> vmFiles, ASMWriter writer) {
        for (File f : vmFiles) {
            writer.setFileName(f);
            VMParser parser = new VMParser(f);

            while (parser.hasMoreCommands()) {
                parser.advance();
                int type = parser.commandType();

                switch (type) {
                    case VMParser.ARITHMETIC:
                        writer.writeArithmetic(parser.arg1());
                        break;
                    case VMParser.POP:
                    case VMParser.PUSH:
                        writer.writePushPop(type, parser.arg1(), parser.arg2());
                        break;
                    case VMParser.LABEL:
                        writer.writeLabel(parser.arg1());
                        break;
                    case VMParser.GOTO:
                        writer.writeGoto(parser.arg1());
                        break;
                    case VMParser.IF:
                        writer.writeIf(parser.arg1());
                        break;
                    case VMParser.RETURN:
                        writer.writeReturn();
                        break;
                    case VMParser.FUNCTION:
                        writer.writeFunction(parser.arg1(), parser.arg2());
                        break;
                    case VMParser.CALL:
                        writer.writeCall(parser.arg1(), parser.arg2());
                        break;
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator [filename|directory]");
            return;
        }

        String fileInName = args[0];
        File fileIn = new File(fileInName);

        try {
            ArrayList<File> vmFiles = processInput(fileIn);
            String fileOutPath = generateOutputFilePath(fileIn, vmFiles);

            File fileOut = new File(fileOutPath);
            ASMWriter writer = new ASMWriter(fileOut);

            if (containsSysInit(vmFiles)) {
                writer.writeBootstrapInit(); // Include bootstrap code if Sys.vm exists
            }

            translateVMFiles(vmFiles, writer);

            writer.close();
            System.out.println("File created: " + fileOutPath);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}