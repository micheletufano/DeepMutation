package edu.wm.cs.mutation.extractor;

/**
 * Container for a Defects4J project.
 * Useful for testing multiple revisions.
 */
public class Defects4JInput {

    private String projPath;
    private String srcPath;
    private String outPath;
    private int complianceLvl;

    Defects4JInput(String projPath, String srcPath, String outPath, int complianceLvl) {
        this.projPath = projPath;
        this.srcPath = srcPath;
        this.outPath = outPath;
        this.complianceLvl = complianceLvl;
    }

    public String getProjPath() {
        return projPath;
    }

    public String getSrcPath() {
        return srcPath;
    }

    public String getOutPath() {
        return outPath;
    }

    public int getComplianceLvl() {
        return complianceLvl;
    }
}
