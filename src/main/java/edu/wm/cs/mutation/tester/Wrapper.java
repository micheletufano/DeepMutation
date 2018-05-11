package edu.wm.cs.mutation.tester;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Wrapper {

    public static native String[] wrapper(String[] args, String dir, long timeout);

    public static void load(String wrapperLib) {
        File f = new File(wrapperLib);
        if (!f.exists()) {
            System.err.println("  ERROR: could not find " + wrapperLib);
            return;
        }
        System.load(f.getAbsolutePath());
    }

    public static String[] run(String[] cmd, String dir, long timeout) {
        // Build exec arguments
        File prog = new File(cmd[0]);
        List<String> args = new ArrayList<>();

        args.add(prog.getAbsolutePath());
        args.add(prog.getName());
        for (int i = 1; i < cmd.length; i++) {
            args.add(cmd[i]);
        }

        // Run C code
        return wrapper(args.toArray(new String[args.size()]), dir, timeout);
    }
}
