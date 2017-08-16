package edu.illinois.cs.cogcomp.nlp.corpuswriters;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by qning2 on 11/25/16.
 */
public class TimelineQuangWriter {
    private String fname;
    private String text;
    private List<EventChunk> eventChunks;
    private List<TemporalJointChunk> timexes;
    private List<TLINK> tlinks;

    public TimelineQuangWriter(String fname) {
        this.fname = fname;
        text = "";
        eventChunks = new LinkedList<>();
        timexes = new LinkedList<>();
        tlinks = new LinkedList<>();
    }
    public void setText(String text){
        this.text = text;
    }
    public void addEventAll(List<EventChunk> ecAll){
        for(EventChunk ec:ecAll)
            addEvent(ec);
    }
    public void addTimexAll(List<TemporalJointChunk> timexAll){
        for(TemporalJointChunk t:timexAll)
            addTimex(t);
    }
    public void addTlinkAll(List<TLINK> tlinkAll){
        for(TLINK tlink:tlinkAll)
            addTlink(tlink);
    }
    public void addEvent(EventChunk ec){
        eventChunks.add(ec);
    }
    public void addTimex(TemporalJointChunk timex){
        timexes.add(timex);
    }
    public void addTlink(TLINK tlink){
        tlinks.add(tlink);
    }
    public void write2file(){
        List<String> output = new ArrayList<>();
        output.add("<DOC>");
        output.addAll(module2string("ORG_TEXT",text));
        output.addAll(module2string("EVENTS",events2string()));
        //output.addAll(module2string("TIMEXES",timexes2string()));
        output.addAll(module2string("TIMEXES","id=0\t(03-19-2003, [2003-03-19T00:00:00.000,2003-03-19T23:59:59.059]) (-1, -1)"));
        output.addAll(module2string("ET_RELATIONS",""));
        output.addAll(module2string("EE_RELATIONS",eetlink2string()));
        output.add("</DOC>");
        Path file = Paths.get(fname);
        try{
            Files.write(file,output, Charset.forName("UTF-8"));
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public List<String> module2string(String modname, List<String> text){
        List<String> module = new ArrayList<>();
        module.add("<"+modname+">");
        module.addAll(text);
        module.add("</"+modname+">");
        return module;
    }
    public List<String> module2string(String modname, String text){
        List<String> tmp = new ArrayList<>();
        if(text.length()!=0)
            tmp.add(text);
        return module2string(modname,tmp);
    }
    private List<String> events2string(){
        List<String> str_events = new ArrayList<>();
        for(EventChunk ec:eventChunks){
            str_events.add("Event: notype (id=EV"+ec.getEiid()+"-1)");
            str_events.add("\tPrimaryTrigger ("+(ec.getCharStart()-1)+", "+(ec.getCharEnd()-1)+") "+ec.getText());
        }
        return str_events;
    }
    private List<String> timexes2string(){
        List<String> str_timexes = new ArrayList<>();
        for(TemporalJointChunk timex:timexes){
            str_timexes.add("id="+timex.getTID()+"\t("+timex.getPhrase().toString()+
                    ", ["+timex.getResult().getValue()+", "+timex.getResult().getValue()+
                    "]) ("+ timex.getCharStart()+", "+timex.getCharEnd()+")");
        }
        return  str_timexes;
    }
    private List<String> eetlink2string(){
        List<String> str_eelinks = new ArrayList<>();
        String rel = "";
        for(TLINK tlink:tlinks){
            if(tlink.getSourceType().equals(TempEval3Reader.Type_Event)&&tlink.getTargetType().equals(TempEval3Reader.Type_Event)){
                switch(tlink.getReducedRelType()){
                    case BEFORE:
                        rel = "before";
                        break;
                    case AFTER:
                        rel = "after";
                        break;
                    case INCLUDES:
                        rel = "overlap";
                        break;
                    case IS_INCLUDED:
                        rel = "overlap";
                        break;
                    case EQUAL:
                        rel = "overlap";
                        break;
                    default:
                        System.out.println("Undefed TlinkType occurred.");
                }
                str_eelinks.add("EV"+tlink.getSourceId()+"-1\tEV"+tlink.getTargetId()+"-1\t"+rel);
            }
        }
        return str_eelinks;
    }
    public static void main(String[] args) throws Exception{
        String type = "TIMEML";
        String corpus = "TimeBank";
        String dir = "data/TempEval3/Training/TBAQ-cleaned/";
        TempEval3Reader myReader;
        myReader = new TempEval3Reader(type,corpus,dir);
        try{
            myReader.ReadData();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        TemporalDataset dataset = myReader.getDataset();
        for(TemporalDocument doc:dataset.getDocuments()){
            TimelineQuangWriter writer = new TimelineQuangWriter("/home/qning2/TimelineConstruction/data/TimeBank"+File.separator+doc.getDocID()+".annotation");
            writer.setText(doc.getOriginalText());
            writer.addTimex(doc.getDocumentCreationTime());
            writer.addTimexAll(doc.getBodyTimexMentions());
            writer.addTlinkAll(doc.getBodyTlinks());
            writer.addEventAll(doc.getBodyEventMentions());
            writer.write2file();
        }
    }
}
