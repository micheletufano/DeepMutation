package edu.wm.cs.mutation.tester;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import edu.wm.cs.mutation.io.IOHandler;
import org.apache.commons.io.FileUtils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

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

    private static final int OK_STATUS = 0;
    private static final int ERROR_STATUS = 1;

    private static String[] compileCmd = null;
    private static String[] testCmd = null;

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
            numThreads = Runtime.getRuntime().availableProcessors() - 1;
        } else {
            numThreads = 1;
        }
        System.out.println("  Using " + numThreads + " thread(s)...");

        // create format for padded threadIDs
        int num_digits = Integer.toString(numThreads).length();
        StringBuilder threadFormat = new StringBuilder();
        threadFormat.append("%0").append(num_digits).append("d");

        // Create a copy of the project for each thread
        System.out.println("  Copying project(s)... ");
        File origProj = new File(projPath);

        File[] mutantProj = new File[numThreads];
        String[] mutantProjPaths = new String[numThreads];
        for (int i = 0; i < numThreads; i++) {
            try {
                mutantProj[i] = new File(new File(projPath).getParent() + "/" + origProj.getName() + i);
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
        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            LinkedHashMap<String, String> mutantsMap = modelsMap.get(modelName);

            // create format for padded mutantIDs
            num_digits = Integer.toString(mutantsMap.keySet().size()).length();
            StringBuilder mutantFormat = new StringBuilder();
            mutantFormat.append("%0").append(num_digits).append("d");

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
            ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
            List<Callable<Object>> tasks = new ArrayList<>(numThreads);

            // Create task list
            for (int i=0; i<numThreads; i++) {
                int threadID = i;
                tasks.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
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
                            int srcStart = sp.getNameSourceStart();
            				    while (original.charAt(srcStart) != '\n')
            					    srcStart--;
            				    srcStart++;
            				    
                            StringBuilder sb = new StringBuilder();
                            sb.append(original.substring(0, srcStart));
                            sb.append(mutantsMap.get(signature));
                            sb.append(original.substring(sp.getSourceEnd() + 1));

                            // Replace original file with mutant file
                            try {
                                String formattedSrc = new Formatter().formatSource(sb.toString());
                                Files.write(Paths.get(mutantPath), formattedSrc.getBytes());
                            } catch (IOException e) {
                                System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                        ": Error in writing mutant " + mutantID + ": " + e.getMessage());
                                e.printStackTrace();
                                return ERROR_STATUS;
                            } catch (FormatterException e) {
                                System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                        ": Error in formatting mutant " + mutantID + ": " + e.getMessage());
                                continue;
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
                                Files.write(Paths.get(mutantPath), original.getBytes());
                            } catch (IOException e) {
                                System.err.println("    " + String.format(threadFormat.toString(), threadID) +
                                        ": ERROR: " + mutantID +
                                        ": could not restore original file: " + mutantPath + "'");
                                e.printStackTrace();
                                return ERROR_STATUS;
                            }
                        }
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
            e.printStackTrace();
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
            e.printStackTrace();
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

    public static void setParallel(boolean parallel) {
        MutantTester.parallel = parallel;
    }
}
