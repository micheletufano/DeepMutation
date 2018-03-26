package edu.wm.cs.mutation.tester;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import edu.wm.cs.mutation.io.IOHandler;
import org.apache.commons.io.FileUtils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class MutantTester {

    private static final String COPY_SUFFIX = ".bak";

    private static final int OK_STATUS = 0;
    private static final int ERROR_STATUS = 1;

    private static String[] compileCmd = null;
    private static String[] testCmd = null;

    // model -> (mutantID -> log)
    private static Map<String, Map<String,String>> compileLogs;
    private static Map<String, Map<String,String>> testLogs;

    private static boolean parallel = true;


    public static void testMutants(String outPath, String projPath,
                                   Map<String, LinkedHashMap<String, String>> modelsMap,
                                   List<CtMethod> methods, List<String> modelPaths) {

        System.out.println("Testing " + projPath + "... ");

        if (compileCmd == null) {
            System.err.println("  ERROR: compile command not set");
            return;
        }
        if (testCmd == null) {
            System.err.println("  ERROR: test command not set");
            return;
        }

        int numThreads;
        if (parallel) {
            numThreads = Runtime.getRuntime().availableProcessors();
        } else {
            numThreads = 1;
        }
        System.out.println("  Using " + numThreads + " thread(s)...");

        // create format for padded threadIDs
        int numDigits = Integer.toString(numThreads).length();
        StringBuilder threadFormat = new StringBuilder();
        threadFormat.append("%0").append(numDigits).append("d");

        // Create a copy of the project for each thread
        System.out.println("  Copying project(s)... ");
        File origProj = new File(projPath);

        File[] mutantProj = new File[numThreads];
        String[] mutantProjPaths = new String[numThreads];
        for (int i = 0; i < numThreads; i++) {
            try {
                mutantProj[i] = new File(origProj.getParent() + "/" + origProj.getName() + i);
                FileUtils.copyDirectory(origProj, mutantProj[i]);
                System.out.println("    Created " + mutantProj[i].getPath() + ".");

                mutantProjPaths[i] = mutantProj[i].getPath() + "/";
            } catch (IOException e) {
                System.err.println("    ERROR: could not copy project(s)");
                e.printStackTrace();
                return;
            }
        }
        System.out.println("  done.");

        // Begin testing
        compileLogs = new HashMap<>();
        testLogs = new HashMap<>();
        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            LinkedHashMap<String, String> mutantsMap = modelsMap.get(modelName);
            int numMutants = mutantsMap.keySet().size();
            int maxIter = (numMutants > numThreads) ? numMutants : numThreads;

            // create format for padded mutantIDs
            numDigits = Integer.toString(numMutants).length();
            StringBuilder mutantFormat = new StringBuilder();
            mutantFormat.append("%0").append(numDigits).append("d");

            // Get list of methods that were mutated
            List<CtMethod> mutated = new ArrayList<>();
            for (CtMethod method : methods) {
                String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
                if (!mutantsMap.containsKey(signature)) {
                    continue;
                }
                mutated.add(method);
            }

            // Create log directories
            String logPath = outPath + modelName + "/" + IOHandler.LOG_DIR;
            try {
                Files.createDirectories(Paths.get(logPath));
            } catch (IOException e) {
                System.err.println("    ERROR: could not create log directory");
                e.printStackTrace();
                continue;
            }

            // Test each mutant in parallel
            ExecutorService executorService = Executors.newFixedThreadPool(maxIter);
            List<Callable<Object>> tasks = new ArrayList<>(maxIter);

            // One map per thread: String mutantID -> String log
            List<Map<String, String>> threadCompileLogs = new ArrayList<>(maxIter);
            List<Map<String, String>> threadTestLogs = new ArrayList<>(maxIter);
            for (int i=0; i<maxIter; i++) {
                threadCompileLogs.add(new HashMap<>());
                threadTestLogs.add(new HashMap<>());
            }

            // Create task list
            for (int i=0; i<maxIter; i++) {
                int threadID = i;
                tasks.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        Map<String, String> compileMap = threadCompileLogs.get(threadID);
                        Map<String, String> testMap = threadTestLogs.get(threadID);

                        for (int j = threadID; j < mutated.size(); j += numThreads) {
                            String mutantID = String.format(mutantFormat.toString(), j+1);

                            CtMethod method = mutated.get(j);
                            String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();

                            System.out.println("    " + String.format(threadFormat.toString(), threadID) +
                                    ": Testing mutant " + mutantID + ": " + signature + "... ");

                            // Get original source code and path to file
                            SourcePosition sp = method.getPosition();
                            String original = sp.getCompilationUnit().getOriginalSourceCode();
                            String mutantPath = sp.getCompilationUnit().getFile().getPath()
                                    .replaceFirst(projPath, mutantProjPaths[threadID]);

                            // Construct and format mutated class
                            StringBuilder sb = new StringBuilder();
                            sb.append(original.substring(0, sp.getSourceStart()));
                            sb.append(mutantsMap.get(signature));
                            sb.append(original.substring(sp.getSourceEnd() + 1));

                            // Replace original file with mutant file
                            try {
                                String formattedSrc = new Formatter().formatSource(sb.toString());
                                Files.write(Paths.get(mutantPath), formattedSrc.getBytes());
                            } catch (IOException e) {
                                System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                        ": Error in writing mutant " + mutantID + ": " + e.getMessage());
                                try {
                                    Files.write(Paths.get(mutantPath), original.getBytes());
                                } catch (IOException ee) {
                                    System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                            ": Failed to recover from mutant " + mutantID + ": " + ee.getMessage());
                                    return ERROR_STATUS;
                                }
                                System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                        ": Successfully recovered from mutant " + mutantID);
                                e.printStackTrace();
                                continue;
                            } catch (FormatterException e) {
                                System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                        ": Error in formatting mutant " + mutantID + ": " + e.getMessage());
                                continue;
                            }

                            // Run tests and save output
                            compileMap.put(mutantID, compile(mutantProj[threadID].getPath()));
                            testMap.put(mutantID, test(mutantProj[threadID].getPath()));

                            // Replace mutant file with original file
                            try {
                                Files.write(Paths.get(mutantPath), original.getBytes());
                            } catch (IOException e) {
                                System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                        ": ERROR: " + mutantID +
                                        ": could not restore original file: " + mutantPath + "'");
                                e.printStackTrace();
                                return ERROR_STATUS;
                            }
                        }

                        threadCompileLogs.add(threadID, compileMap);
                        threadTestLogs.add(threadID, testMap);
                        return OK_STATUS;
                    }
                });
            }

            // Run tasks
            try {
                long start = System.nanoTime();
                List<Future<Object>> futures = executorService.invokeAll(tasks);
                System.out.println("    Took " + (System.nanoTime() - start) / 1000000000.0 + " seconds.");
                for (Future future : futures) {
                    future.get();
                }
            } catch (InterruptedException e) {
                System.err.println("    ERROR: main thread was interrupted");
                e.printStackTrace();
            } catch (ExecutionException e) {
                System.err.println("    ERROR: worker thread threw an exception");
                e.printStackTrace();
            } finally {
                System.out.println("    Stopping all threads...");
                executorService.shutdown();
                try {
                    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    System.err.println("      ERROR: interrupted while stopping threads");
                    e.printStackTrace();
                }
            }

            // Reduce and save
            Map<String,String> modelCompileLogs = new HashMap<>();
            Map<String,String> modelTestLogs = new HashMap<>();
            for (int i=0; i<maxIter; i++) {
                modelCompileLogs.putAll(threadCompileLogs.get(i));
                modelTestLogs.putAll(threadTestLogs.get(i));
            }
            compileLogs.put(modelName, modelCompileLogs);
            testLogs.put(modelName, modelTestLogs);

            System.out.println("  done.");
        }

        // Clean up extra projects
        System.out.println("  Deleting mutant project(s)...");
        for (int i = 0; i < numThreads; i++) {
            try {
                FileUtils.deleteDirectory(mutantProj[i]);
            } catch (IOException e) {
                System.err.println("  WARNING: could not clean up mutant project(s)");
                e.printStackTrace();
            }
        }
        System.out.println("  done.");

        System.out.println("done.");
    }

    /**
     * Compile project. Adapted from {@link ProcessBuilder} javadoc.
     *
     * @param mutantProjPath
     */
    private static String compile(String mutantProjPath) {
        StringBuilder sb = new StringBuilder();

        ProcessBuilder pb = new ProcessBuilder(compileCmd);
        pb.directory(new File(mutantProjPath));
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            for (String line; (line = br.readLine()) != null; ) {
                sb.append(line).append(System.lineSeparator());
            }
            p.waitFor();
            return sb.toString();
        } catch (IOException e) {
            System.err.println("    ERROR: could not run compile command");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("    ERROR: interrupted compile command");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Run project tests. Adapted from {@link ProcessBuilder} javadoc.
     *
     * @param mutantProjPath
     */
    private static String test(String mutantProjPath) {
        StringBuilder sb = new StringBuilder();

        ProcessBuilder pb = new ProcessBuilder(testCmd);
        pb.directory(new File(mutantProjPath));
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            for (String line; (line = br.readLine()) != null; ) {
                sb.append(line).append(System.lineSeparator());
            }
            p.waitFor();
            return sb.toString();
        } catch (IOException e) {
            System.err.println("    ERROR: could not run test command");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setCompileCmd(String... compileCmd) {
        MutantTester.compileCmd = compileCmd;
    }

    public static void setTestCmd(String... testCmd) {
        MutantTester.testCmd = testCmd;
    }

    public static void setParallel(boolean parallel) {
        MutantTester.parallel = parallel;
    }

    public static Map<String, Map<String, String>> getCompileLogs() {
        return compileLogs;
    }

    public static Map<String, Map<String, String>> getTestLogs() {
        return testLogs;
    }
}
