package edu.wm.cs.mutation.io;

public class ExportMutantsTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String projPath = dataPath + "WebServer/";
        String outPath = dataPath + "out/WebServer/";
        String mutantsPath = outPath + "50len_ident_lit/mutants/";

        IOHandler.exportMutants(projPath, outPath, mutantsPath);
    }

}
