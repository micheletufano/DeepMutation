package edu.wm.cs.mutation.tester;

import java.util.List;
import java.util.Map;

public class Output {

    private Map<String, List<String>> compileLogs;
    private Map<String, List<String>> testLogs;
    private Map<String, List<Boolean>> timeouts;

    Output(Map<String, List<String>> compileLogs,
           Map<String, List<String>> testLogs,
           Map<String, List<Boolean>> timeouts) {
        this.compileLogs = compileLogs;
        this.testLogs = testLogs;
        this.timeouts = timeouts;
    }

    public Map<String, List<String>> getCompileLogs() {
        return compileLogs;
    }

    public Map<String, List<String>> getTestLogs() {
        return testLogs;
    }

    public Map<String, List<Boolean>> getTimeouts() {
        return timeouts;
    }
}
