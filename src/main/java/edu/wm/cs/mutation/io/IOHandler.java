package edu.wm.cs.mutation.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOHandler {

    private static final String KEY_OUTPUT = "methods.key";
    private static final String SRC_OUTPUT = "methods.src";
    private static final String MAP_OUTPUT = "methods.map";

    public static final String ABS_OUTPUT = "methods.abs";
    public static final String ABS_SUFFIX = ".abs";

    public static void writeMethods(String outDir, LinkedHashMap<String, String> map, boolean abstracted) {
        List<String> signatures = new ArrayList<>(map.keySet());
        List<String> bodies = new ArrayList<>(map.values());

        try {
            Files.createDirectories(Paths.get(outDir));
            Files.write(Paths.get(outDir + KEY_OUTPUT), signatures);
            if (abstracted) {
                Files.write(Paths.get(outDir + ABS_OUTPUT), bodies);
            } else {
                Files.write(Paths.get(outDir + SRC_OUTPUT), bodies);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeMethodsFromDefects4J(Map<String, LinkedHashMap<String, String>> map, boolean abstracted) {
        for (String outDir : map.keySet()) {
            LinkedHashMap<String, String> submap = map.get(outDir);

            List<String> signatures = new ArrayList<>(submap.keySet());
            List<String> bodies = new ArrayList<>(submap.values());

            try {
                Files.createDirectories(Paths.get(outDir));
                Files.write(Paths.get(outDir + KEY_OUTPUT), signatures);
                if (abstracted) {
                    Files.write(Paths.get(outDir + ABS_OUTPUT), bodies);
                } else {
                    Files.write(Paths.get(outDir + SRC_OUTPUT), bodies);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeMappings(Map<String, List<String>> map) {
        for (String outDir : map.keySet()) {
            try {
                Files.write(Paths.get(outDir + MAP_OUTPUT), map.get(outDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Set<String> readIdioms(String filePath) {
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            return stream.collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
