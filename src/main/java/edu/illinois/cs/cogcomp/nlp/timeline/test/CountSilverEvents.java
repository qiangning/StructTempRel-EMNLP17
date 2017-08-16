package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.List;

/**
 * Created by qning2 on 4/9/17.
 */
public class CountSilverEvents {
    public static void main(String[] args) throws Exception {
        int dataset = args.length==0?8:Integer.parseInt(args[0]);

        boolean bethard_chambers = false;

        ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        TempEval3Reader myReader;
        String data_dir, data_label;
        switch (dataset) {
            case 0:
                data_dir = rm.getString("platinum_dir");
                data_label = rm.getString("platinum_label");
                break;
            case 1:
                data_dir = rm.getString("timebank_dir");
                data_label = rm.getString("timebank_label");
                break;
            case 2:
                data_dir = rm.getString("aquaint_dir");
                data_label = rm.getString("aquaint_label");
                break;
            case 3:
                data_dir = rm.getString("silver0_dir");
                data_label = rm.getString("silver0_label");
                break;
            case 4:
                data_dir = rm.getString("silver1_dir");
                data_label = rm.getString("silver1_label");
                break;
            case 5:
                data_dir = rm.getString("silver2_dir");
                data_label = rm.getString("silver2_label");
                break;
            case 6:
                data_dir = rm.getString("silver3_dir");
                data_label = rm.getString("silver3_label");
                break;
            case 7:
                data_dir = rm.getString("silver4_dir");
                data_label = rm.getString("silver4_label");
                break;
            case 8:
                data_dir = rm.getString("silver5_dir");
                data_label = rm.getString("silver5_label");
                break;
            default:
                data_dir = rm.getString("platinum_dir");
                data_label = rm.getString("platinum_label");
        }
        System.out.println(data_dir+":"+data_label);
        myReader = new TempEval3Reader("TIMEML", data_label, data_dir);
        myReader.ReadData();
        if(bethard_chambers) {
            myReader.readChambers();
            myReader.readBethard();
        }
        List<TemporalDocument> allDocs = myReader.getDataset().getDocuments();
        int cnt = 0;
        for(TemporalDocument doc:allDocs){
            cnt += doc.getBodyEventMentions().size();
        }
        System.out.println(cnt);
        timer.end();
        timer.print();
    }
}
