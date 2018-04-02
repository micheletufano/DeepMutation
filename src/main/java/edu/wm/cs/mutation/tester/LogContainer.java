package edu.wm.cs.mutation.tester;

import java.util.List;
import java.util.Map;

public class LogContainer {

    private Map<String, List<String>> compileLogs;
    private Map<String, List<String>> testLogs;

    LogContainer(Map<String, List<String>> compileLogs,
                 Map<String, List<String>> testLogs) {
        this.compileLogs = compileLogs;
        this.testLogs = testLogs;
    }

    public Map<String, List<String>> getCompileLogs() {
        return compileLogs;
    }

    public Map<String, List<String>> getTestLogs() {
        return testLogs;
    }

}
