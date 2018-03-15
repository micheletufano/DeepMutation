package edu.wm.cs.mutation.tester;

import edu.wm.cs.mutation.io.IOHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class MutantTester {

    private static final String COPY_SUFFIX = ".bak";

    private static final String COMPILE_LOG_SUFFIX = "_compile.log";
    private static final String TEST_LOG_SUFFIX = "_test.log";

    private static String[] compileCmd = null;
    private static String[] testCmd = null;

    private static final int OK_STATUS = 0;
    private static final int ERROR_STATUS = 1;

    public static void testMutants(String outPath, String projPath,
                                   Map<String, LinkedHashMap<String, String>> mutantsMap, List<String> modelPaths) {

        System.out.println("Testing " + projPath + "... ");

        if (compileCmd == null) {
            System.err.println("  ERROR: compile command not set");
            return;
        }
        if (testCmd == null) {
            System.err.println("  ERROR: test command not set");
            return;
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("  Using " + numThreads + " threads...");

        System.out.println("  Copying project(s)... ");
        File origProj = new File(projPath);
        final String projDir = origProj.getName();

        File[] mutantProj = new File[numThreads];
        String[] mutantProjDir = new String[numThreads];
        for (int i = 0; i < numThreads; i++) {
            try {
                mutantProj[i] = new File(new File(projPath).getParent() + "/" + origProj.getName() + i);
                FileUtils.copyDirectory(origProj, mutantProj[i]);
                System.out.println("    Created " + mutantProj[i].getPath() + ".");

                mutantProjDir[i] = mutantProj[i].getName();
            } catch (IOException e) {
                System.err.println("    ERROR: could not copy project(s)");
                return;
            }
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

            // Test each mutant in parallel
            ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
            List<Callable<Object>> tasks = new ArrayList<>(numThreads);

            for (int i=0; i<numThreads; i++) {
                int threadID = i;
                tasks.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        for (int j = threadID; j < mutantFiles.length; j += numThreads) {
                            File mutantFile = mutantFiles[j];
                            String mutantID = mutantFile.getName().split("_")[0];
                            File origFile = new File(mutantFile.getName()
                                    .split("_")[1]
                                    .replace("-", "/")
                                    .replaceFirst(projDir, mutantProjDir[threadID]));
                            File copyFile = new File(origFile.getPath() + COPY_SUFFIX);

                            System.out.println("    Testing mutant " + mutantID + ": " +
                                    signatures.get(Integer.parseInt(mutantID) - 1) + "... ");

                            // Copy original file
                            try {
                                FileUtils.copyFile(origFile, copyFile);
                            } catch (IOException e) {
                                System.err.println("    ERROR: " + mutantID + ": could not copy original file");
                                return ERROR_STATUS;
                            }

                            // Replace original file with mutant file
                            try {
                                FileUtils.copyFile(mutantFile, origFile);
                            } catch (IOException e) {
                                System.err.println("    ERROR: " + mutantID + ": could not copy mutant file");
                                FileUtils.deleteQuietly(copyFile);
                                return ERROR_STATUS;
                            }

                            // Run test
                            if (!compile(mutantID, mutantProj[threadID].getPath(), logPath)) {
                                return ERROR_STATUS;
                            }
                            if (!test(mutantID, mutantProj[threadID].getPath(), logPath)) {
                                return ERROR_STATUS;
                            }

                            // Replace mutant file with original file
                            try {
                                FileUtils.copyFile(copyFile, origFile);
                            } catch (IOException e) {
                                System.err.println("    ERROR: " + mutantID + ": could not restore original file");
                                return ERROR_STATUS;
                            } finally {
                                FileUtils.deleteQuietly(copyFile);
                            }
                        }
                        return OK_STATUS;
                    }
                });
            }

            try {
                List<Future<Object>> futures = executorService.invokeAll(tasks);
                for (Future future : futures) {
                    future.get();
                }
            } catch (InterruptedException e) {
                System.err.println("    ERROR: main thread was interrupted");
            } catch (ExecutionException e) {
                System.err.println("    ERROR: worker thread threw an exception");
            } finally {
                System.out.println("    Stopping all threads...");
                executorService.shutdown();
                try {
                    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    System.err.println("      ERROR: interrupted while stopping threads");
                }
            }
            System.out.println("  done.");
        }

        System.out.println("  Deleting mutant project(s)...");
        for (int i = 0; i < numThreads; i++) {
            try {
                FileUtils.deleteDirectory(mutantProj[i]);
            } catch (IOException e) {
                System.err.println("  WARNING: could not clean up mutant project(s)");
            }
        }
        System.out.println("  done.");

        System.out.println("done.");
    }

    /**
     * Compile project. Adapted from {@link ProcessBuilder} javadoc.
     *
     * @param mutantID
     * @param mutantProjPath
     * @param logPath
     */
    private static boolean compile(String mutantID, String mutantProjPath, String logPath) {
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
            return false;
        }
        return true;
    }

    /**
     * Run project tests. Adapted from {@link ProcessBuilder} javadoc.
     *
     * @param mutantID
     * @param mutantProjPath
     * @param logPath
     */
    private static boolean test(String mutantID, String mutantProjPath, String logPath) {
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
            return false;
        }
        return true;
    }

    public static void setCompileCmd(String... compileCmd) {
        MutantTester.compileCmd = compileCmd;
    }

    public static void setTestCmd(String... testCmd) {
        MutantTester.testCmd = testCmd;
    }
}
