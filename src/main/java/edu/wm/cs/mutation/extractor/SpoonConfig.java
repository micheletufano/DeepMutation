package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.compiler.ModelBuildingException;

public class SpoonConfig {
    public static SpoonAPI buildModel(String sourcePath, int complianceLvl, String libPath, boolean compiled) {
        SpoonAPI spoon = new Launcher();

        //Configure classpath
        if (compiled) {
            configureCompiledClasspath(libPath, spoon);
        } else {
            configureClasspath(libPath, spoon);
        }


        try {
            buildSpoonModel(spoon, sourcePath, complianceLvl);
        } catch (ModelBuildingException e) {
            e.printStackTrace();
        }

        return spoon;
    }

    private static void configureCompiledClasspath(String srcRoot, SpoonAPI spoon) {

        String libPath = srcRoot + "lib";
        String buildClasses = srcRoot + "build/classes";
        String buildTest = srcRoot + "build/test";
        String buildLib = srcRoot + "build/lib";

        ArrayList<String> paths = new ArrayList<>();
        addIfExists(paths, buildClasses);
        addIfExists(paths, buildTest);

        addFilesInPath(paths, buildLib);
        addFilesInPath(paths, libPath);
        addJarFiles(paths, libPath);

        String[] classpath = paths.toArray(new String[paths.size()]);

        spoon.getEnvironment().setSourceClasspath(classpath);
    }

    private static void addJarFiles(List<String> paths, String path) {
        File file = new File(path);
        if (file.exists()) {
            List<String> jars = listJARFiles(path)
                    .stream()
                    .map(j -> j.getAbsolutePath())
                    .collect(Collectors.toList());

            paths.addAll(jars);
        }
    }

    private static List<File> listJARFiles(String dirPath) {
        //Path Matcher for java files (recursive)
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.jar");

        List<File> files = null;
        try {
            files = Files.walk(Paths.get(dirPath))
                    .filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(p))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    private static void addFilesInPath(List<String> paths, String path) {
        File dir = new File(path);

        if (dir.exists()) {
            File[] libs = dir.listFiles();

            for (File l : libs) {
                paths.add(l.getAbsolutePath());
            }
        }
    }

    private static void addIfExists(List<String> paths, String path) {
        File file = new File(path);

        if (file.exists()) {
            paths.add(path);
        }
    }

    private static void configureClasspath(String libPath, SpoonAPI spoon) {
        if (libPath != null) {
            File[] libs = new File(libPath).listFiles();
            ArrayList<String> libPaths = new ArrayList<>();

            for (File l : libs) {
                libPaths.add(l.getAbsolutePath());
            }

            String[] classpath = libPaths.toArray(new String[libPaths.size()]);

            spoon.getEnvironment().setSourceClasspath(classpath);
        } else {
            spoon.getEnvironment().setNoClasspath(true);
        }
    }

    private static void buildSpoonModel(SpoonAPI spoon, String sourcePath, int complianceLvl) {
        spoon.getEnvironment().setComplianceLevel(complianceLvl);
        spoon.getEnvironment().setAutoImports(false);
        spoon.addInputResource(sourcePath);
        spoon.buildModel();
    }
}
