package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.compiler.ModelBuildingException;

public class SpoonConfig {
    public static SpoonAPI buildModel(String sourcePath, int complianceLvl, String libDir, boolean compiled) {
        SpoonAPI spoon = new Launcher();

        //Configure classpath
        if (compiled) {
            configureCompiledClasspath(libDir, spoon);
        } else {
            configureClasspath(libDir, spoon);
        }


        try {
            buildSpoonModel(spoon, sourcePath, complianceLvl);
        } catch (ModelBuildingException e) {
            e.printStackTrace();
        }

        return spoon;
    }

    private static void configureCompiledClasspath(String srcRoot, SpoonAPI spoon) {

        String libDir = srcRoot + "lib";
        String buildClasses = srcRoot + "build/classes";
        String buildTest = srcRoot + "build/test";
        String buildLib = srcRoot + "build/lib";

        ArrayList<String> paths = new ArrayList<>();
        addIfExists(paths, buildClasses);
        addIfExists(paths, buildTest);

        addFilesInDir(paths, buildLib);
        addFilesInDir(paths, libDir);
        addJarFiles(paths, libDir);

        String[] classpath = paths.toArray(new String[paths.size()]);

        spoon.getEnvironment().setSourceClasspath(classpath);
    }

    private static void addJarFiles(List<String> paths, String path) {
        List<String> jars = FileUtility.listJARFiles(path)
                .stream()
                .map(j -> j.getAbsolutePath())
                .collect(Collectors.toList());

        paths.addAll(jars);
    }

    private static void addFilesInDir(List<String> paths, String path) {
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

    private static void configureClasspath(String libDir, SpoonAPI spoon) {
        File[] libs = new File(libDir).listFiles();
        ArrayList<String> libPaths = new ArrayList<>();

        for (File l : libs) {
            libPaths.add(l.getAbsolutePath());
        }

        String[] classpath = libPaths.toArray(new String[libPaths.size()]);

        spoon.getEnvironment().setSourceClasspath(classpath);
    }

    private static void buildSpoonModel(SpoonAPI spoon, String sourcePath, int complianceLvl) {
        spoon.getEnvironment().setComplianceLevel(complianceLvl);
        spoon.getEnvironment().setAutoImports(false);
        spoon.addInputResource(sourcePath);
        spoon.buildModel();
    }
}
