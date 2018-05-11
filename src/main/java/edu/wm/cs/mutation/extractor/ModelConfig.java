package edu.wm.cs.mutation.extractor;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Container for Spoon model configurations.
 */
public class ModelConfig {
    private JSONObject data;
    private JSONObject compliance;
    private Map<Integer, String> srcMap;
    private List<Integer> srcKeys;

    /**
     * Parse a model config JSON file.
     *
     * @param file Model config JSON file
     */
    public void init(String file) {
        //Parse json file
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(file));
            data = (JSONObject) obj;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Init compliance
        compliance = (JSONObject) data.get("complianceLevel");

        //Init src dirs
        JSONObject srcConfs = (JSONObject) data.get("src");
        Set<String> confIDs = srcConfs.keySet();
        srcMap = new HashMap<>();
        srcKeys = new ArrayList<>();

        for (String key : confIDs) {
            JSONObject conf = (JSONObject) srcConfs.get(key);
            String src = (String) conf.get("srcjava");
            srcMap.put(Integer.parseInt(key), src);
            srcKeys.add(Integer.parseInt(key));
        }

        Collections.sort(srcKeys);
    }

    public String getSrcPath(int confID) {
        String dir = "";

        for (int conf : srcKeys) {
            if (confID <= conf) {
                return srcMap.get(conf);
            }
        }

        return dir;
    }

    public int getComplianceLevel(int confID) {
        JSONObject conf = (JSONObject) compliance.get("" + confID);
        String complianceLvl = conf.get("source").toString();
        return Integer.parseInt(complianceLvl);
    }
}
