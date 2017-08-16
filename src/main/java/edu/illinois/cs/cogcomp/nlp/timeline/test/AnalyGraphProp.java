package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 * Created by qning2 on 11/17/16.
 */
public class AnalyGraphProp {
    private TemporalDataset dataset;
    private final String type = "TIMEML";
    private final String corpus = "TimeBank";
    //private final String dir = "data/TempEval3/Evaluation/";
    private final String dir = "data/TempEval3/Training/TBAQ-cleaned/";

    public AnalyGraphProp(String type, String corpus, String dir) throws Exception{
        TempEval3Reader myReader;
        myReader = new TempEval3Reader(type,corpus,dir);
        try{
            myReader.ReadData();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        dataset = myReader.getDataset();
    }
    public AnalyGraphProp() throws Exception{
        TempEval3Reader myReader;
        myReader = new TempEval3Reader(type,corpus,dir);
        try{
            myReader.ReadData();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        dataset = myReader.getDataset();
    }
    public AnalyGraphProp(boolean bethard,boolean chambers) throws Exception{
        TempEval3Reader myReader;
        myReader = new TempEval3Reader(type,corpus,dir);
        try{
            myReader.ReadData();
            if(bethard)
                myReader.readBethard();
            if(chambers)
                myReader.readChambers();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        dataset = myReader.getDataset();
    }
    public void printAll(){
        List<TemporalDocument> documents = dataset.getDocuments();
        for(TemporalDocument doc:documents){
            doc.printAll();
        }
    }
    public void createJavaScriptAll(String dir, int mode){
        List<TemporalDocument> documents = dataset.getDocuments();
        for(TemporalDocument doc:documents){
            doc.createJavaScript(dir,mode);
        }
    }

    public TemporalDataset getDataset() {
        return dataset;
    }

    public static void main(String[] args) throws Exception{
        String dir = "output/original";
        AnalyGraphProp graphProp = new AnalyGraphProp(false,false);
        //graphProp.getDataset().getDocuments().get(7).saturateTlinks();
        //graphProp.getDataset().getDocuments().get(7).createJavaScript(dir,1);
        /*for(TemporalDocument doc:graphProp.getDataset().getDocuments()) {
            doc.orderTimexes();
            doc.saturateTlinks();
        }*/
        graphProp.createJavaScriptAll(dir,0);
        //graphProp.printAll();
    }
}
