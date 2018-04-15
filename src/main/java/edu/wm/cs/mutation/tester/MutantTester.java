package edu.wm.cs.mutation.tester;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import edu.wm.cs.mutation.Consts;
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

    private static final int OK_STATUS = 0;
    private static final int ERROR_STATUS = 1;

    private static String[] compileCmd = null;
    private static String[] testCmd = null;

    private static boolean usingBaseline = true;
    private static String compileBaseline;
    private static String testBaseline;
    private static String[] compileFailStrings;
    private static String[] testFailStrings;

    // model -> (mutantID -> logs)
    private static Map<String, Map<String,List<String>>> compileLogs;
    private static Map<String, Map<String,List<String>>> testLogs;

    // model -> (mutantIDs)
    private static Map<String, List<String>> failedMutants;

    // model -> (mutantID -> pass/fail)
    private static Map<String, Map<String,List<Boolean>>> compilable;
    private static Map<String, Map<String,List<Boolean>>> successful;

    private static boolean parallel = true;
    private static boolean cleanUp = true;

    public static void testMutants(String projPath, Map<String, LinkedHashMap<String, List<String>>> modelsMap,
                                   List<CtMethod> methods, List<String> modelPaths) {

        // get real and absolute path to projPath
        final File projFile;
        try {
            projFile = new File(Paths.get(projPath).toRealPath().toString());
        } catch (IOException e) {
            System.err.println("  ERROR: toRealPath()");
            e.printStackTrace();
            return;
        }
        if (!projFile.exists() || !projFile.isDirectory()) {
            System.err.println("  ERROR: " + projFile.getAbsolutePath() + " is not a directory");
        }

        System.out.println("Testing " + projFile.getAbsolutePath() + "... ");

        // Check for null maps
        if (modelsMap == null) {
            System.err.println("  ERROR: null input map");
            return;
        }

        boolean all_null = true;
        for (String modelPath : modelPaths) {
            LinkedHashMap<String, List<String>> mutantsMap = modelsMap.get(new File(modelPath).getName());
            if (mutantsMap != null) {
                all_null = false;
                break;
            }
        }

        if (all_null) {
            System.err.println("  ERROR: maps are null for all models");
            return;
        }

        // Check for null commands
        if (compileCmd == null) {
            System.err.println("  ERROR: compile command not set");
            return;
        }
        if (testCmd == null) {
            System.err.println("  ERROR: test command not set");
            return;
        }

        // Check for existing binaries
        if (!findCommand(compileCmd[0])) {
            System.err.println("  ERROR: could not find " + compileCmd[0]);
            return;
        }
        if (!findCommand(testCmd[0])) {
            System.err.println("  ERROR: could not find " + testCmd[0]);
            return;
        }

        // Check for null fail string
        if (!usingBaseline) {
            if (compileFailStrings == null) {
                System.err.println("  ERROR: compile regex not set");
                return;
            }
            if (testFailStrings == null) {
                System.err.println("  ERROR: test regex not set");
                return;
            }
        }

        // Get number of threads
        int numThreads;
        if (parallel) {
            numThreads = Runtime.getRuntime().availableProcessors();
        } else {
            numThreads = 1;
        }
        System.out.println("  Using " + numThreads + " thread(s)...");

        // Create format for padded threadIDs
        int numDigits = Integer.toString(numThreads).length();
        StringBuilder threadFormat = new StringBuilder();
        threadFormat.append("%0").append(numDigits).append("d");

        // Create a copy of the project for each thread
        System.out.println("  Copying project(s)... ");

        File[] mutantProj = new File[numThreads];
        String[] mutantProjPaths = new String[numThreads];
        for (int i = 0; i < numThreads; i++) {
            try {
                mutantProj[i] = new File(projFile.getParent() + File.separator + projFile.getName() + i);
                if (!mutantProj[i].exists()) {
                    FileUtils.copyDirectory(projFile, mutantProj[i]);
                    System.out.println("    Created " + mutantProj[i].getPath() + ".");
                } else {
                    System.out.println("    " + mutantProj[i].getPath() + " already exists.");
                }

                mutantProjPaths[i] = mutantProj[i].getAbsolutePath();
            } catch (IOException e) {
                System.err.println("    ERROR: could not copy project(s)");
                e.printStackTrace();
                return;
            }
        }
        System.out.println("  done.");

        // Establish baseline
        if (usingBaseline) {
            System.out.println("  Establishing baseline... ");

            File baselineProj = new File(projFile.getParent() + File.separator + projFile.getName() + ".base");
            try {
                FileUtils.copyDirectory(projFile, baselineProj);
            } catch (IOException e) {
                System.err.println("  ERROR: could not establish baseline");
                e.printStackTrace();
                return;
            }

            compileBaseline = compile(baselineProj.getPath());
            testBaseline = test(baselineProj.getPath());
            if (compileBaseline == null || testBaseline == null) {
                System.err.println("  ERROR: could not establish baseline");
                return;
            }

            try {
                FileUtils.deleteDirectory(baselineProj);
            } catch (IOException e) {
                System.err.println("  WARNING: could not clean up baseline project");
                e.printStackTrace();
            }
            System.out.println("  done. ");
        }

        // Begin testing
        compileLogs = new HashMap<>();
        testLogs = new HashMap<>();
        compilable = new HashMap<>();
        successful = new HashMap<>();
        failedMutants = new HashMap<>();
        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            LinkedHashMap<String, List<String>> mutantsMap = modelsMap.get(modelName);

            if (mutantsMap == null || mutantsMap.size() == 0) {
                System.err.println("    WARNING: skipping null/empty map for model " + modelName);
                continue;
            }

            int numMethods = mutantsMap.keySet().size();
            int numBeams = 0;
            int numMutants = 0;
            for (String method : mutantsMap.keySet()) {
                int size = mutantsMap.get(method).size();
                numMutants += size;
                numBeams = (numBeams < size) ? size : numBeams;
            }
            int maxIter = (numMutants < numThreads) ? numMutants : numThreads;
            System.out.println("    Number of mutants: " + numMutants);
            System.out.println("    Number of methods: " + numMethods);
            System.out.println("    Number of beams:   " + numBeams);

            // create format for padded methodIDs
            numDigits = Integer.toString(numMethods).length();
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

            // Test each mutant in parallel
            ExecutorService executorService = Executors.newFixedThreadPool(maxIter);
            List<Callable<LogContainer>> tasks = new ArrayList<>(maxIter);

            // Create task list
            for (int i=0; i<maxIter; i++) {
                int threadID = i;
                tasks.add(new Callable<LogContainer>() {
                    @Override
                    public LogContainer call() throws Exception {
                        Map<String, List<String>> compileMap = new HashMap<>();
                        Map<String, List<String>> testMap = new HashMap<>();

                        for (int j = threadID; j < mutated.size(); j += numThreads) {
                            String methodID = String.format(mutantFormat.toString(), j+1);

                            CtMethod method = mutated.get(j);
                            String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();

                            System.out.println("    " + String.format(threadFormat.toString(), threadID) +
                                    ": Testing method " + methodID + ": " + signature + "... ");

                            // Get original source code and path to file
                            SourcePosition sp = method.getPosition();
                            String original = sp.getCompilationUnit().getOriginalSourceCode();
                            String origPath = sp.getCompilationUnit().getFile().getAbsolutePath();
                            String mutantPath = origPath.replaceFirst(projFile.getAbsolutePath(), mutantProjPaths[threadID]);
                            if (mutantPath.equals(origPath)) {
                                System.err.println("      ERROR: could not locate path to mutant project");
                            }

                            // Construct and format mutated class
                            int srcStart = sp.getNameSourceStart();
                            while (original.charAt(srcStart) != '\n')
                                srcStart--;
                            srcStart++;

                            List<String> mutants = mutantsMap.get(signature);
                            List<String> compileLogs = new ArrayList<>();
                            List<String> testLogs = new ArrayList<>();
                            for (int k=0; k<mutants.size(); k++) {
                                String mutant = mutants.get(k);
                                String mutantID = Integer.toString(k+1);

                                StringBuilder sb = new StringBuilder();
                                sb.append(original.substring(0, srcStart));
                                sb.append(mutant);
                                sb.append(original.substring(sp.getSourceEnd() + 1));

                                // Replace original file with mutant file
                                try {
                                    String formattedSrc = new Formatter().formatSource(sb.toString());
                                    Files.write(Paths.get(mutantPath), formattedSrc.getBytes());
                                } catch (IOException e) {
                                    System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                            ": Error in writing mutant " + methodID + "." + mutantID +": " + e.getMessage());
                                    try {
                                        Files.write(Paths.get(mutantPath), original.getBytes());
                                    } catch (IOException ee) {
                                        System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                                ": Failed to recover from mutant " + methodID + "." + mutantID + ": " + ee.getMessage());
                                        return null;
                                    }
                                    System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                            ": Successfully recovered from mutant " + methodID + "." + mutantID);
                                    e.printStackTrace();
                                    continue;
                                } catch (FormatterException e) {
                                    System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                            ": Error in formatting mutant " + methodID + "." + mutantID + ": " + e.getMessage());
                                    continue;
                                }

                                // Run tests and save output
                                compileLogs.add(compile(mutantProj[threadID].getPath()));
                                testLogs.add(test(mutantProj[threadID].getPath()));

                                // Replace mutant file with original file
                                try {
                                    Files.write(Paths.get(mutantPath), original.getBytes());
                                } catch (IOException e) {
                                    System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                            ": ERROR: " + methodID + "." + mutantID +
                                            ": could not restore original file: " + mutantPath + "'");

//                                    List<String> failedIDs = new ArrayList<>();
//                                    for (int l = j; l < mutated.size(); l += numThreads) {
//                                        String failedID = String.format(mutantFormat.toString(), l + 1);
//                                        failedIDs.add(failedID);
//                                    }
//                                    threadFailedMutants.add(threadID, failedIDs);
                                    e.printStackTrace();
                                    return null;
                                }
                            }
                            compileMap.put(methodID, compileLogs);
                            testMap.put(methodID, testLogs);
                        }
                        return new LogContainer(compileMap, testMap);
                    }
                });
            }

            // Run tasks
            Map<String,List<String>> modelCompileLogs = new HashMap<>();
            Map<String,List<String>> modelTestLogs = new HashMap<>();
//            List<String> modelFailedMutants = new ArrayList<>();
            try {
                long start = System.nanoTime();
                List<Future<LogContainer>> futures = executorService.invokeAll(tasks);
                System.out.println("    Took " + (System.nanoTime() - start) / 1000000000.0 + " seconds.");

                for (Future<LogContainer> future : futures) {
                    LogContainer lc = future.get();
                    modelCompileLogs.putAll(lc.getCompileLogs());
                    modelTestLogs.putAll(lc.getTestLogs());
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
            compileLogs.put(modelName, modelCompileLogs);
            testLogs.put(modelName, modelTestLogs);
//            failedMutants.put(modelName, modelFailedMutants);

            // Generate results
            Map<String,List<Boolean>> modelCanCompile = new HashMap<>();
            Map<String,List<Boolean>> modelPassesTest = new HashMap<>();

            for (String methodID : modelCompileLogs.keySet()) {
                List<String> compileLogs = modelCompileLogs.get(methodID);
                List<Boolean> canCompile = new ArrayList<>();
                for (String log : compileLogs) {
                    if (usingBaseline) {
                        canCompile.add(log.equals(compileBaseline));
                    } else {
                        boolean res = true;
                        for (String failStr : compileFailStrings) {
                            if (log.contains(failStr)) {
                                res = false;
                                break;
                            }
                        }
                        canCompile.add(res);
                    }
                }
                modelCanCompile.put(methodID, canCompile);
            }

            for (String methodID : modelTestLogs.keySet()) {
                List<String> testLogs = modelTestLogs.get(methodID);
                List<Boolean> passesTest = new ArrayList<>();
                for (String log : testLogs) {
                    if (usingBaseline) {
                        passesTest.add(log.equals(testBaseline));
                    } else {
                        boolean res = true;
                        for (String failStr : testFailStrings) {
                            if (log.contains(failStr)) {
                                res = false;
                                break;
                            }
                        }
                        passesTest.add(res);
                    }
                }
                modelPassesTest.put(methodID, passesTest);
            }

            compilable.put(modelName, modelCanCompile);
            successful.put(modelName, modelPassesTest);

            System.out.println("  done.");
        }

        // Clean up extra projects
        if (cleanUp) {
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
        }

        System.out.println("done.");
    }

    private static boolean findCommand(String cmd) {
        if (!new File(cmd).exists()) {
            String PATH = System.getenv("PATH");
            String[] paths = PATH.split(File.pathSeparator);
            for (String path : paths) {
                if (new File(path + File.separator + cmd).exists()) {
                    return true;
                }
            }
            return false;
        }
        return true;
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

    public static void writeBaseline(String outPath) {
        if (compileBaseline == null || testBaseline == null) {
            System.err.println("ERROR: cannot write null baselines");
            return;
        }

        try {
            Files.write(Paths.get(outPath + File.separator + "baseline" + Consts.COMPILE_LOG_SUFFIX), compileBaseline.getBytes());
            Files.write(Paths.get(outPath + File.separator + "baseline" + Consts.TEST_LOG_SUFFIX), testBaseline.getBytes());
        } catch (IOException e) {
            System.err.println("    Error in writing baseline: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void writeLogs(String outPath, List<String> modelPaths) {
        System.out.println("Writing logs... ");

        if (compileLogs == null || testLogs == null) {
            System.err.println("  ERROR: cannot write null maps");
            return;
        }

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            Map<String, List<String>> compileMap = compileLogs.get(modelName);
            Map<String, List<String>> testMap = testLogs.get(modelName);

            if (compileMap == null || testMap == null) {
                System.err.println("    WARNING: cannot write null map for model " + modelName);
                continue;
            }

            // Create log directory
            String logPath = outPath + File.separator + modelName + File.separator + Consts.LOG_DIR + File.separator;
            try {
                Files.createDirectories(Paths.get(logPath));
            } catch (IOException e) {
                System.err.println("    ERROR: could not create log directory");
                e.printStackTrace();
                continue;
            }

            for (String methodID : compileMap.keySet()) {
                List<String> logs = compileMap.get(methodID);
                for (int i=0; i<logs.size(); i++) {
                    try {
                        if (logs.size() == 1) {
                            Files.write(Paths.get(logPath + methodID + Consts.COMPILE_LOG_SUFFIX), logs.get(i).getBytes());
                        } else {
                            Files.write(Paths.get(logPath + methodID + "-" + (i + 1) + Consts.COMPILE_LOG_SUFFIX), logs.get(i).getBytes());
                        }
                    } catch (IOException e) {
                        System.err.println("    Error in writing mutant " + methodID + "." + i + ": " + e.getMessage());
                    }
                }
            }
            for (String methodID : testMap.keySet()) {
                List<String> logs = testMap.get(methodID);
                for (int i=0; i<logs.size(); i++) {
                    try {
                        if (logs.size() == 1) {
                            Files.write(Paths.get(logPath + methodID + Consts.TEST_LOG_SUFFIX), logs.get(i).getBytes());
                        } else {
                            Files.write(Paths.get(logPath + methodID + "-" + (i + 1) + Consts.TEST_LOG_SUFFIX), logs.get(i).getBytes());
                        }
                    } catch (IOException e) {
                        System.err.println("    Error in writing mutant " + methodID + "." + i + ": " + e.getMessage());
                    }
                }
            }
            System.out.println("  done.");
        }
        System.out.println("done.");
    }

    public static void writeResults(String outPath, List<String> modelPaths) {
        System.out.println("Writing results... ");
        if (compilable == null || successful == null) {
            System.err.println("ERROR: cannot write null result maps");
            return;
        }

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            Map<String, List<Boolean>> compMap = compilable.get(modelName);
            Map<String, List<Boolean>> succMap = successful.get(modelName);
            if (compMap == null || succMap == null) {
                System.err.println("    WARNING: cannot write null map for model " + modelName);
                continue;
            }

            SortedMap<String, List<Boolean>> sortedCompMap = new TreeMap<>(compMap);
            SortedMap<String, List<Boolean>> sortedSuccMap = new TreeMap<>(succMap);

            String path = outPath + File.separator + modelName + File.separator;
            StringBuilder sb = new StringBuilder();

            for (String methodID : sortedCompMap.keySet()) {
                List<Boolean> results = sortedCompMap.get(methodID);
                sb.append(methodID);
                for (Boolean b : results) {
                    sb.append(" ");
                    if (b) {
                        sb.append(Consts.PASSED);
                    } else {
                        sb.append(Consts.FAILED);
                    }
                }
                sb.append(System.lineSeparator());
            }
            try {
                Files.write(Paths.get(path + Consts.RESULTS_COMP), sb.toString().getBytes());
            } catch (IOException e) {
                System.err.println("    Error in writing results");
            }

            sb.setLength(0);
            for (String methodID : sortedSuccMap.keySet()) {
                List<Boolean> results = sortedSuccMap.get(methodID);
                sb.append(methodID);
                for (Boolean b : results) {
                    sb.append(" ");
                    if (b) {
                        sb.append(Consts.PASSED);
                    } else {
                        sb.append(Consts.FAILED);
                    }
                }
                sb.append(System.lineSeparator());
            }
            try {
                Files.write(Paths.get(path + Consts.RESULTS_TEST), sb.toString().getBytes());
            } catch (IOException e) {
                System.err.println("    Error in writing results");
            }
            System.out.println("  done.");
        }
        System.out.println("done.");
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

    public static String getCompileBaseline() {
        return compileBaseline;
    }

    public static String getTestBaseline() {
        return testBaseline;
    }

    public static Map<String, Map<String, List<String>>> getCompileLogs() {
        return compileLogs;
    }

    public static Map<String, Map<String, List<String>>> getTestLogs() {
        return testLogs;
    }

    public static Map<String, Map<String, List<Boolean>>> getCompilable() {
        return compilable;
    }

    public static Map<String, Map<String, List<Boolean>>> getSuccessful() {
        return successful;
    }

    public static Map<String, List<String>> getFailedMutants() {
        return failedMutants;
    }

    public static void useBaseline(boolean usingBaseline) {
        MutantTester.usingBaseline = usingBaseline;
    }

    public static void setCompileFailStrings(String... compileFailStrings) {
        MutantTester.compileFailStrings = compileFailStrings;
    }

    public static void setTestFailStrings(String... testFailStrings) {
        MutantTester.testFailStrings = testFailStrings;
    }

    public static boolean usingBaseline() {
        return usingBaseline;
    }

    public static void setCleanUp(boolean cleanUp) {
        MutantTester.cleanUp = cleanUp;
    }
}
