package edu.illinois.cs.cogcomp.nlp.corpusreaders;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Created by qning2 on 12/8/16.
 */
public class BethardVerbClauseReader {
    private final String input = "data/TempEval3/timebank-verb-clause.txt";
    private HashMap<String, List<TLINK>> extraRels = new HashMap<>();

    public String getInput() {
        return input;
    }

    public void readData() throws Exception {
        Scanner in = new Scanner(new FileReader(input));
        while (in.hasNextLine()) {
            String line = in.nextLine();
            line = line.trim().replaceAll(" +", " ");
            String[] parts = line.split("\\s+");
            if(line.startsWith("#"))
                continue;
            String docId = parts[0];
            String sourceType = TempEval3Reader.Type_Event;
            String targetType = TempEval3Reader.Type_Event;
            String sourceId = parts[1].replaceAll("[^0-9]", "");
            String targetId = parts[2].replaceAll("[^0-9]", "");
            String relType = parts[3];
            if(relType.equals("OVERLAP"))//don't consider overlap rel
                continue;
            TLINK extra = new TLINK(-1, relType, sourceType, targetType, Integer.parseInt(sourceId), Integer.parseInt(targetId));
            if (extraRels.containsKey(docId)) {
                extraRels.get(docId).add(extra);
            } else {
                List<TLINK> newList = new ArrayList<>();
                newList.add(extra);
                extraRels.put(docId, newList);
            }
        }
    }

    public HashMap<String, List<TLINK>> getExtraRels() {
        return extraRels;
    }
}
