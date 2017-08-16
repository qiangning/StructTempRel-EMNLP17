package edu.illinois.cs.cogcomp.nlp.corpusreaders;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.CompareCAVEO.TBDense_split;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Created by qning2 on 12/8/16.
 */
public class ChambersDenseReader {
    private final String input = "data/TempEval3/TimebankDense.full.txt";
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
            String docId = parts[0];
            String sourceType = parts[1].charAt(0) == 'e' ? TempEval3Reader.Type_Event : TempEval3Reader.Type_Timex;
            String targetType = parts[2].charAt(0) == 'e' ? TempEval3Reader.Type_Event : TempEval3Reader.Type_Timex;
            String sourceId = parts[1].replaceAll("[^0-9]", "");
            String targetId = parts[2].replaceAll("[^0-9]", "");
            String relType = "";
            /*if(parts[3].equals("v"))
                continue;//don't consider vague rel*/
            switch(parts[3]){
                case "b":
                    relType = "BEFORE";
                    break;
                case "a":
                    relType = "AFTER";
                    break;
                case "ii":
                    relType = "IS_INCLUDED";
                    break;
                case "i":
                    relType = "INCLUDES";
                    break;
                case "s":
                    relType = "SIMULTANEOUS";
                    break;
                case "v":
                    relType = "UNDEF";
                    break;
                default:
                    System.out.println("Unexpected relType in chambers file.");
                    continue;
            }
            /*NOTE this id is eid instead of eiid. We have to fix this when adding it to documents.*/
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

    /*Serialize chambers_only to ser_dir3*/
    public static void main(String[] args) throws Exception{
        ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
        List<TemporalDocument> timebank = TempEval3Reader.deserialize(TempEval3Reader.label2dir("timebank"));
        ChambersDenseReader reader = new ChambersDenseReader();
        reader.readData();
        HashMap<String, List<TLINK>> extraRels = reader.getExtraRels();
        int cnt = 0;
        for(TemporalDocument doc:timebank){
            if(!extraRels.containsKey(doc.getDocID()))
                continue;
            int lid = 1;
            List<TLINK> validTlinks = new ArrayList<>();
            for (TLINK tlink : extraRels.get(doc.getDocID())) {
                if (tlink.getSourceType().equals(TempEval3Reader.Type_Event)) {
                    EventChunk ec = doc.getEventMentionFromEID(tlink.getSourceId(), false);
                    if (ec == null)
                        continue;
                    tlink.setSourceId(ec.getEiid());
                }
                if (tlink.getTargetType().equals(TempEval3Reader.Type_Event)) {
                    EventChunk ec = doc.getEventMentionFromEID(tlink.getTargetId(), false);
                    if (ec == null)
                        continue;
                    tlink.setTargetId(ec.getEiid());
                }
                if (!doc.validateTlink(tlink))//check if this tlink is valid
                    continue;
                tlink.setLid(lid++);
                validTlinks.add(tlink);
            }
            doc.setBodyTlinks(validTlinks);
            cnt+=validTlinks.size();
            doc.serialize(rm.getString("ser_dir3"),doc.getDocID(),true);
            doc.temporalDocumentToText("./output/Chambers/gold/"+doc.getDocID()+".tml");
        }
        System.out.println(cnt);
    }
}
