package edu.uw.cs.lil.uwtime.chunking.chunks;

import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;
import edu.uw.cs.utils.composites.Pair;

import java.util.Map;

/**
 * Created by qning2 on 11/15/16.
 */
public class EventChunk implements java.io.Serializable {
    private static final long serialVersionUID = 956934971208060284L;
    private String eventclass;
    private String tense;
    private String aspect;
    private String polarity;
    private String pos;
    private String text;
    private int eid;
    private int eiid;
    private int charStart;
    private int charEnd;
    private String cardinality;
    private TemporalSentence sentence;

    public EventChunk(String eventclass, String tense, String aspect, int charStart, int charEnd, int eid){
        this.eventclass = eventclass;
        this.tense = tense;
        this.aspect = aspect;
        this.charStart = charStart;
        this.charEnd = charEnd;
        this.eid = eid;
    }
    public void setSentence(TemporalSentence sentence) {
        this.sentence = sentence;
    }
    public void setEiid(int eiid){
        this.eiid = eiid;
    }
    public void setTense(String tense){this.tense = tense;}
    public void setAspect(String aspect){this.aspect = aspect;}
    public void setPolarity(String polarity){
        this.polarity = polarity;
    }
    public void setPos(String pos){
        this.pos = pos;
    }
    public void setCharEnd(String text) {
        charEnd = charStart + text.length();
    }
    public void setCardinality(String cardinality){this.cardinality = cardinality;}
    public String getEventclass(){return eventclass;}
    public String getTense(){return  tense;}
    public String getAspect(){return aspect;}
    public String getPolarity(){return polarity;}
    public String getPos(){return pos;}
    public int getEid(){return eid;}
    public int getEiid(){return eiid;}
    public int getCharStart(){return charStart;}
    public int getCharEnd(){return charEnd;}
    public int Eiid2Eid(EventChunk[] ec_array, int eiid){
        for(EventChunk ec:ec_array){
            if(ec.getEiid()==eiid){
                return ec.getEid();
            }
        }
        return -1;
    }
    public String getCardinality() {
        return cardinality;
    }
    public TemporalSentence getSentence() {return sentence; }
    @Override
    public String toString() {
        return "EventChunk{" +
                "eiid=" + eiid +
                ", eid=" + eid +
                ", text=" + text +
                ", eventclass='" + eventclass + '\'' +
                ", tense='" + tense + '\'' +
                ", aspect='" + aspect + '\'' +
                ", polarity='" + polarity + '\'' +
                ", pos='" + pos + '\'' +
                ", charStart=" + charStart +
                ", charEnd=" + charEnd +
                ", cardinality=" + cardinality +
                '}';
    }

    public String toStringConcise(){
        return "EventChunk{" +
                "eid=" + eid +
                ", text=" + text +
                ", eventclass='" + eventclass + '\'' +
                '}';
    }

    public String toStringLabel(){
        return "e"+eiid;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setCharStart(int charStart) {
        this.charStart = charStart;
    }

    public void setCharEnd(int charEnd) {
        this.charEnd = charEnd;
    }

    public String makeInstanceAnnotation() {
        if (aspect == null || polarity == null || pos == null || tense == null) {
            return "";
        }
        return String.format("<MAKEINSTANCE %s %s %s %s %s %s />\n",
                "aspect=\"" + aspect + "\"",
                "eiid=\"ei" + eiid + "\"",
                "eventID=\"e" + eid + "\"",
                "polarity=\"" + polarity + "\"",
                "pos=\"" + pos + "\"",
                "tense=\"" + tense + "\"");
    }

    public String beginAnnotation() {
        return String.format("<EVENT %s %s>",
                "class=\"" + eventclass + "\"",
                "eid=\"e" + eid + "\"");
    }

    public String endAnnotation() {
        return "</EVENT>";
    }

    private boolean alignHelper(Map<Integer, Pair<TemporalSentence, Integer>> startCharToToken,
                                Map<Integer, Pair<TemporalSentence, Integer>> endCharToToken,
                                int tempCharStart, int tempCharEnd) {
        // Wrapper around core alignment code to accommodate mistakes in annotation
        // where "10 p.m", rather than "10 p.m." is labeled as the mention
        // Performs two-way linking between mention and sentence. Return whether it was successful or not
        if(startCharToToken.containsKey(tempCharStart) && endCharToToken.containsKey(tempCharEnd)) {
            Pair<TemporalSentence, Integer> startIndexes = startCharToToken.get(tempCharStart);
            Pair<TemporalSentence, Integer> endIndexes = endCharToToken.get(tempCharEnd);
            // Assume start and end occur in the same sentence
            TemporalSentence alignedSentence = startIndexes.first();
            sentence = alignedSentence;
            return true;
        }
        else
            return false;
    }

    public void alignTokens(Map<Integer, Pair<TemporalSentence, Integer>> startCharToToken,
                            Map<Integer, Pair<TemporalSentence, Integer>> endCharToToken) {
        if(charStart != -1) {
            if (alignHelper(startCharToToken, endCharToToken, charStart, charEnd))
                return;
            else if (alignHelper(startCharToToken, endCharToToken, charStart, charEnd + 1))
                return;
            else if (alignHelper(startCharToToken, endCharToToken, charStart + 1, charEnd))
                return;
            else if (alignHelper(startCharToToken, endCharToToken, charStart, charEnd - 1))
                return;
            else {
                TemporalLog.printf("error", "Unable to find offset for mention [#%d -> #%d](%s)\n", charStart, charEnd, this.toString());
            }
        }
    }
}
