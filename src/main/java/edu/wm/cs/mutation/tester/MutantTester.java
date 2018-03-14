package edu.wm.cs.mutation.tester;

import edu.wm.cs.mutation.io.IOHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MutantTester {

    private static final String MUTANT_DIR_SUFFIX = "Mutant";
    private static final String COPY_SUFFIX = ".bak";

    private static final String COMPILE_LOG_SUFFIX = "_compile.log";
    private static final String TEST_LOG_SUFFIX = "_test.log";

    private static String[] compileCmd = null;
    private static String[] testCmd = null;

    public static void testMutants(String outPath, String projPath,
                                   Map<String, LinkedHashMap<String,String>> mutantsMap, List<String> modelPaths) {

        System.out.println("Testing " + projPath + "... ");

        if (compileCmd == null) {
            System.err.println("  ERROR: compile command not set");
            return;
        }
        if (testCmd == null) {
            System.err.println("  ERROR: test command not set");
            return;
        }

        System.out.println("  Copying projects... ");
        String projDir = null, mutantProjDir = null;
        File origProj = null, mutantProj = null;
        try {
            origProj = new File(projPath);
            mutantProj = new File(new File(projPath).getParent() + "/" + origProj.getName() + MUTANT_DIR_SUFFIX);
            FileUtils.copyDirectory(origProj, mutantProj);

            projDir = origProj.getName();
            mutantProjDir = mutantProj.getName();
        } catch (IOException e) {
            System.err.println("    ERROR: could not copy project");
            return;
        }
        System.out.println("  done.");

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            // Get mutant signatures
            List<String> signatures = new ArrayList<>(mutantsMap.get(modelName).keySet());

            // Get mutant files
            String mutantsPath = outPath + modelName + "/" + IOHandler.MUTANT_DIR;
            File[] mutantFiles = new File(mutantsPath).listFiles();
            if (mutantFiles == null) {
                System.err.println("  ERROR: could not find any mutants");
                continue;
            }
            Arrays.sort(mutantFiles);

            // Create log directories
            String logPath = outPath + modelName + "/" + IOHandler.LOG_DIR;
            try {
                Files.createDirectories(Paths.get(logPath));
            } catch (IOException e) {
                System.err.println("  ERROR: could not create log directory");
                continue;
            }

            // Test each mutant
            for (File mutantFile : mutantFiles) {
                String mutantID = mutantFile.getName().split("_")[0];
                File origFile = new File(mutantFile.getName()
                        .split("_")[1]
                        .replace("-","/")
                        .replaceFirst(projDir, mutantProjDir));
                File copyFile = new File(origFile.getPath() + COPY_SUFFIX);

                System.out.println("    Testing mutant " + mutantID + ": " +
                        signatures.get(Integer.parseInt(mutantID)) + "... ");

                // Copy original file
                try {
                    FileUtils.copyFile(origFile, copyFile);
                } catch (IOException e) {
                    System.err.println("    WARNING: could not copy original file");
                    e.printStackTrace();
                    continue;
                }

                // Replace original file with mutant file
                try {
                    FileUtils.copyFile(mutantFile, origFile);
                } catch (IOException e) {
                    System.err.println("    WARNING: could not copy mutant file");
                    FileUtils.deleteQuietly(copyFile);
                    continue;
                }

                // Run test
                compile(mutantID, mutantProj.getPath(), logPath);
                test(mutantID, mutantProj.getPath(), logPath);

                // Replace mutant file with original file
                try {
                    FileUtils.copyFile(copyFile, origFile);
                } catch (IOException e) {
                    System.err.println("    ERROR: could not restore original file");
                    break;
                } finally {
                    FileUtils.deleteQuietly(copyFile);
                }
            }

            System.out.println("  done.");
        }

        try {
            FileUtils.deleteDirectory(mutantProj);
        } catch (IOException e) {
            System.err.println("  WARNING: could not clean up directory");
        }

        System.out.println("done.");
    }

    /**
     * Compile project. Adapted from {@link ProcessBuilder} javadoc.
     * @param mutantID
     * @param mutantProjPath
     * @param logPath
     */
    private static void compile(String mutantID, String mutantProjPath, String logPath) {
        File log = new File(logPath + mutantID + COMPILE_LOG_SUFFIX);
        FileUtils.deleteQuietly(log);

        ProcessBuilder pb = new ProcessBuilder(compileCmd);
        pb.directory(new File(mutantProjPath));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        try {
            Process p = pb.start();
            assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
            assert pb.redirectOutput().file() == log;
            assert p.getInputStream().read() == -1;
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("    ERROR: could not run compile command");
            FileUtils.deleteQuietly(log);
        }
    }

    /**
     * Run project tests. Adapted from {@link ProcessBuilder} javadoc.
     * @param mutantID
     * @param mutantProjPath
     * @param logPath
     */
    private static void test(String mutantID, String mutantProjPath, String logPath) {
        File log = new File(logPath + mutantID + TEST_LOG_SUFFIX);
        FileUtils.deleteQuietly(log);

        ProcessBuilder pb = new ProcessBuilder(testCmd);
        pb.directory(new File(mutantProjPath));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        try {
            Process p = pb.start();
            assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
            assert pb.redirectOutput().file() == log;
            assert p.getInputStream().read() == -1;
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("    ERROR: could not run test command");
            FileUtils.deleteQuietly(log);
        }
    }

    public static void setCompileCmd(String... compileCmd) {
        MutantTester.compileCmd = compileCmd;
    }

    public static void setTestCmd(String... testCmd) {
        MutantTester.testCmd = testCmd;
    }
}
