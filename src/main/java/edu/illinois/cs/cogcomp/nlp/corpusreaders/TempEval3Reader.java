package edu.illinois.cs.cogcomp.nlp.corpusreaders;

import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.MemoryDog;
import edu.illinois.cs.cogcomp.pipeline.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.uwtime.chunking.ChunkSequence;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.IChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.corrections.AnnotationCorrections;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.data.readers.AbstractTemporalReader;
import edu.uw.cs.lil.uwtime.data.readers.TimeMLReader;
import edu.uw.cs.lil.uwtime.data.readers.WikiWarsReader;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.utils.DependencyUtils.DependencyParseToken;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Qiang (John) Ning on 8/29/16.
 */
/*Read from TempEval-3 corpus and save into "private TemporalDataset dataset", whose data structure is very easy to understand.*/

public class TempEval3Reader {
    private AbstractTemporalReader reader;
    private String type;
    private String datalabel;
    private String datasetName;
    private String dir;
    private TemporalDataset dataset;
    private AnnotatorService pipeline;
    private static boolean createTAonly = false;
    public static final String Type_Event = "event";
    public static final String Type_Timex = "timex";

    public TempEval3Reader(String type, String datalabel, String dir) throws Exception {
        Properties cacheDir = new Properties();
        cacheDir.put("cacheDirectory", "serialized_data/annotation-cache-" + datalabel);
        ResourceManager rm = Configurator.mergeProperties(new PipelineConfigurator().getConfig(new ResourceManager("config/pipeline-config.properties")),
                new ResourceManager(cacheDir));
        pipeline = PipelineFactory.buildPipeline(rm);

        switch (type.toLowerCase()) {
            case "timeml":
                this.type = "TIMEML";
                System.out.println("Data type: " + this.type);
                reader = new TimeMLReader();
                System.out.println("TimeMLReader created successfully.");
                break;
            case "wikiwars":
                this.type = "WIKIWARS";
                System.out.println("Data type: " + this.type);
                reader = new WikiWarsReader();
                System.out.println("WikiWarsReader created successfully.");
                break;
            default:
                throw new IllegalArgumentException("Invalid corpus type: " + type + ". Valid types are TIMEML and WIKIWARS.");
        }
        this.datalabel = datalabel;
        this.dir = dir;
        this.dataset = null;
        this.datasetName = this.type + ":" + this.datalabel;
    }
    public static List<TemporalDocument> deserialize(String dir) throws Exception{
        return deserialize(dir,Integer.MAX_VALUE,false);
    }
    public static List<TemporalDocument> deserialize(String[] dir) throws Exception{
        return deserialize(dir,Integer.MAX_VALUE,false);
    }
    public static List<TemporalDocument> deserialize(String dir, int MAX_EVENTS, boolean verbose) throws Exception{
        List<TemporalDocument> allDocs = new ArrayList<>();
        File file = new File(dir);
        File[] filelist = file.listFiles();
        if(filelist==null) {
            if(verbose)
                System.out.println(dir+" is empty.");
            return null;
        }
        if(verbose)
            System.out.println("Loading from "+dir+". In total "+filelist.length+" files.");
        for(File f:filelist){
            if(f.isFile()) {
                //deserialize doc
                TemporalDocument doc = TemporalDocument.deserialize(dir,f.getName().substring(0,f.getName().indexOf(".ser")),verbose);
                /*serialization doesn't exist.*/
                if(doc==null){
                    continue;
                }
                //<max events
                if(doc.getBodyEventMentions().size()>MAX_EVENTS){
                    if(verbose)
                        System.out.println("Too many events. "+doc.getDocID()+" is skipped.");
                    continue;
                }
                //annotation exists
                if(doc.getTextAnnotation()==null){
                    if(verbose)
                        System.out.println("TextAnnotation doesn't exist. "+doc.getDocID()+" is skipped.");
                    continue;
                }
                allDocs.add(doc);
            }
        }
        return allDocs;
    }
    public static List<TemporalDocument> deserialize(String[] dir, int MAX_EVENTS, boolean verbose) throws Exception{
        int n = dir.length;
        List<TemporalDocument> docs = deserialize(dir[0],MAX_EVENTS,verbose);
        for(int i=1;i<n;i++){
            assert docs!=null;
            List<TemporalDocument> tmp = deserialize(dir[i],MAX_EVENTS,verbose);
            assert tmp!=null;
            docs.addAll(tmp);
        }
        return docs;
    }
    public static String label2dir(String datalabel) throws Exception{
        return label2dir(datalabel,true);
    }
    public static String label2dir(String datalabel, boolean bethard_chambers) throws Exception{
        ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
        String label = "platinum_label";
        if(datalabel.toLowerCase().equals("chambers_only"))
            return rm.getString("ser_dir3");
        switch(datalabel.toLowerCase()){
            case "platinum":
                label = "platinum_label";
                break;
            case "timebank":
                label = "timebank_label";
                break;
            case "aquaint":
                label = "aquaint_label";
                break;
            case "silver0":
                label = "silver0_label";
                break;
            case "silver1":
                label = "silver1_label";
                break;
            case "silver2":
                label = "silver2_label";
                break;
            case "silver3":
                label = "silver3_label";
                break;
            case "silver4":
                label = "silver4_label";
                break;
            case "silver5":
                label = "silver5_label";
                break;
            default:
                System.out.println("Invalid datalabel.");
                System.exit(-1);
        }
        if(bethard_chambers)
            return rm.getString("ser_dir")+ File.separator+rm.getString(label);
        return rm.getString("ser_dir2")+ File.separator+rm.getString(label);
    }

    public static void main(String[] args) throws Exception {
        int dataset = args.length==0?1:Integer.parseInt(args[0]);

        boolean chambers = false;
        boolean bethard = true;
        boolean sat = false;

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
        if(chambers) {
            myReader.readChambers();
        }
        if(bethard){
            myReader.readBethard();
        }
        List<TemporalDocument> allDocs = myReader.getDataset().getDocuments();
        for(TemporalDocument doc:allDocs){
            try {
                System.out.println("Processing [" + (allDocs.indexOf(doc) + 1) + "/" + allDocs.size() + "] " + doc.getDocID());
                if(sat) {
                    System.out.println("Saturating...");
                    doc.saturateTlinks(Integer.MAX_VALUE, false, true);
                }
                System.out.println("Annotating...");
                doc.createTextAnnotation(myReader.getPipeline());
                System.out.println("Serializing...");
                if(bethard&&chambers)
                    doc.serialize(rm.getString("ser_dir")+ File.separator+data_label, doc.getDocID(), true);
                else if(bethard)
                    doc.serialize(rm.getString("ser_dir")+ File.separator + "aug_Bethard" + File.separator + data_label, doc.getDocID(), true);
                else if(chambers)
                    doc.serialize(rm.getString("ser_dir")+ File.separator + "aug_Chambers" + File.separator + data_label, doc.getDocID(), true);
                else
                    doc.serialize(rm.getString("ser_dir2")+ File.separator+data_label, doc.getDocID(), true);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        timer.end();
        timer.print();
    }

    public AnnotatorService getPipeline() {
        return pipeline;
    }

    public void clearData() {
        dataset = null;
    }

    public void ReadData() throws IOException, SAXException, ParserConfigurationException, InterruptedException {
        if (dataset == null) {
            dataset = new TemporalDataset(datasetName);
            for (TemporalDocument document : reader.getDataset(dir, datalabel).getDocuments()) {
                dataset.addDocument(document);
            }
            dataset.sort();
        } else {
            System.out.println("TempEval3Reader.dataset is not empty!");
        }
    }

    private void ApplyCorrection() throws IOException {
        AnnotationCorrections corrections = new AnnotationCorrections("./data/fromUWTime/corrections.csv");
        corrections.applyCorrections(dataset);
    }

    private void dataset2CoNLL(String out_dir, String out_fname, boolean skipEmpty, boolean singleLabel) {
        LinkedList<String> conll = new LinkedList<String>();
        for (TemporalDocument document : dataset.getDocuments()) {
            for (TemporalSentence sentence : document.withoutDCT().getSentences()) {
                /*Get tokens*/
                List<String> tokens = sentence.getTokens();
                int N = tokens.size();
                /*Get POS tags*/
                List<DependencyParseToken> dependencyParse = sentence.getDependencyParse();
                int M = dependencyParse.size();
                if (N != M) {
                    System.out.println("tokens and dependencyParse do not have the same size: " + sentence.toString());
                    return;
                }
                String[] pos_tag = new String[N];
                for (int n = 0; n < N; n++) {
                    pos_tag[n] = dependencyParse.get(n).getPOS();
                }
                /*Get golden B/I/O labels with type (DATE/TIME/SET/DURATION)*/
                String[] label = new String[N];
                for (int i = 0; i < N; i++)
                    label[i] = "O";
                ChunkSequence<TemporalJointChunk, LogicalExpression> goldChunkSequence =
                        sentence.getLabel();
                List<TemporalJointChunk> chunks = goldChunkSequence.getChunks();
                if (chunks.size() <= 0 && skipEmpty) {
                    continue;
                }
                String[] type = new String[chunks.size()];
                for (int i = 0; i < chunks.size(); i++) {
                    MentionResult result = chunks.get(i).getResult();
                    if (!singleLabel) {
                        type[i] = result.getType();
                    } else {
                        type[i] = "null";
                    }
                    IChunk<LogicalExpression> baseChunk = chunks.get(i).getBaseChunk();
                    label[baseChunk.getStart()] = "B-" + type[i];
                    for (int j = baseChunk.getStart() + 1; j <= baseChunk.getEnd(); j++) {
                        label[j] = "I-" + type[i];
                    }
                }
                /*Add to output stream*/
                for (int i = 0; i < N; i++) {
                    conll.add(tokens.get(i) + " " + pos_tag[i] + " " + label[i]);
                }
                conll.add("\n");
            }
        }

        /*Output to file*/
        Path file = Paths.get(out_dir + out_fname);
        try {
            Files.write(file, conll, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("CoNLL format file generated successfully and saved to " + out_dir + out_fname);
    }

    private void dataset2sentence(String out_dir, String out_fname, boolean skipEmpty) {
        LinkedList<String> output = new LinkedList<String>();
        for (TemporalDocument document : dataset.getDocuments()) {
            for (TemporalSentence sentence : document.withoutDCT().getSentences()) {
                ChunkSequence<TemporalJointChunk, LogicalExpression> goldChunkSequence =
                        sentence.getLabel();
                List<TemporalJointChunk> chunks = goldChunkSequence.getChunks();
                if (chunks.size() <= 0 && skipEmpty) {
                    continue;
                }
                output.add(sentence.toString());
            }
        }
        Path file = Paths.get(out_dir + out_fname);
        try {
            Files.write(file, output, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getType() {
        return type;
    }

    public TemporalDataset getDataset() {
        return dataset;
    }

    public void createTextAnnotation() throws Exception {
        for (TemporalDocument doc : getDataset().getDocuments()) {
            if(createTAonly) {//release the memory for other documents
                System.out.println("Creating textannotation for doc: "+doc.getDocID());
            }
            doc.createTextAnnotation(pipeline);
        }
    }

    public void saturateTlinks() throws Exception{
        saturateTlinks(false);
    }
    public void saturateTlinks(boolean debug) throws Exception{saturateTlinks(Integer.MAX_VALUE,debug);}
    public void saturateTlinks(int maxIter, boolean debug) throws Exception {
        int cnt = 1;
        for (TemporalDocument doc : getDataset().getDocuments()) {
            System.out.println("Saturating [" + cnt + "/" + getDataset().getDocuments().size() + "] " + doc.getDocID());
            doc.saturateTlinks(maxIter,debug);
            cnt++;
        }
    }
    public void removeDuplicatedTlinks(){
        for(TemporalDocument doc:getDataset().getDocuments()){
            doc.removeDuplicatedTlinks();
        }
    }

    public void removeEElinks() {
        int cnt = 1;
        for (TemporalDocument doc : getDataset().getDocuments()) {
            System.out.println("Removing EE links [" + cnt + "/" + getDataset().getDocuments().size() + "] " + doc.getDocID());
            doc.removeEElinks();
            cnt++;
        }
    }

    public void removeETlinks() {
        int cnt = 1;
        for (TemporalDocument doc : getDataset().getDocuments()) {
            System.out.println("Removing ET links [" + cnt + "/" + getDataset().getDocuments().size() + "] " + doc.getDocID());
            doc.removeETlinks();
            cnt++;
        }
    }

    public void removeTTlinks() {
        int cnt = 1;
        for (TemporalDocument doc : getDataset().getDocuments()) {
            System.out.println("Removing TT links [" + cnt + "/" + getDataset().getDocuments().size() + "] " + doc.getDocID());
            doc.removeTTlinks();
            cnt++;
        }
    }

    public void addVagueTlinks(double th) {
        int cnt = 1;
        for (TemporalDocument doc : getDataset().getDocuments()) {
            System.out.println("Adding NONE labels [" + cnt + "/" + getDataset().getDocuments().size() + "] " + doc.getDocID());
            doc.addVagueTlinks(th);
            cnt++;
        }
    }

    public void orderTimexes() {
        int cnt = 1;
        for (TemporalDocument doc : getDataset().getDocuments()) {
            System.out.println("Ordering timexes [" + cnt + "/" + getDataset().getDocuments().size() + "] " + doc.getDocID());
            doc.orderTimexes();
            cnt++;
        }
    }

    public void readBethard() throws Exception{
        BethardVerbClauseReader reader = new BethardVerbClauseReader();
        reader.readData();
        HashMap<String, List<TLINK>> extraRels = reader.getExtraRels();
        for(TemporalDocument doc:this.getDataset().getDocuments()){
            int lid = doc.getMaxLID();
            lid++;
            for (Map.Entry<String, List<TLINK>> entry : extraRels.entrySet()) {
                String key = entry.getKey();
                List<TLINK> tlinks = entry.getValue();
                if (doc.getDocID().equals(key)) {
                    for (TLINK tlink : tlinks) {
                        if(!doc.checkTlinkExistence(tlink)) {
                            if(!doc.validateTlink(tlink))//check if this tlink is valid
                                continue;
                            tlink.setLid(lid);
                            doc.insertMention(tlink);
                            lid++;
                        }
                    }
                }
            }
        }
    }

    public void readChambers() throws Exception{
        ChambersDenseReader reader = new ChambersDenseReader();
        reader.readData();
        HashMap<String, List<TLINK>> extraRels = reader.getExtraRels();
        for(TemporalDocument doc:this.getDataset().getDocuments()){
            int lid = doc.getMaxLID();
            lid++;
            for (Map.Entry<String, List<TLINK>> entry : extraRels.entrySet()) {
                String key = entry.getKey();
                List<TLINK> tlinks = entry.getValue();
                if (doc.getDocID().equals(key)) {
                    for (TLINK tlink : tlinks) {
                        if(tlink.getSourceType().equals(TempEval3Reader.Type_Event)){
                            EventChunk ec = doc.getEventMentionFromEID(tlink.getSourceId(),false);
                            if(ec==null)
                                continue;
                            tlink.setSourceId(ec.getEiid());
                        }
                        if(tlink.getTargetType().equals(TempEval3Reader.Type_Event)){
                            EventChunk ec = doc.getEventMentionFromEID(tlink.getTargetId(),false);
                            if(ec==null)
                                continue;
                            tlink.setTargetId(ec.getEiid());
                        }
                        if(!doc.checkTlinkExistence(tlink)) {
                            if(!doc.validateTlink(tlink))//check if this tlink is valid
                                continue;
                            tlink.setLid(lid);
                            doc.insertMention(tlink);
                            lid++;
                        }
                    }
                }
            }
        }
    }

}

