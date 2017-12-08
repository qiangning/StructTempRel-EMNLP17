package edu.uw.cs.lil.uwtime.data;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.nlp.classifier.sl.temporalStructure;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.EventStruct;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TimexStruct;
import edu.illinois.cs.cogcomp.nlp.graph.GraphJavaScript;

import edu.illinois.cs.cogcomp.nlp.util.TransitivityTriplets;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;

import edu.illinois.cs.cogcomp.pipeline.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;
import edu.stanford.nlp.ling.CoreAnnotations;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.CoreMap;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.TemporalLexicalOriginFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;
import edu.uw.cs.utils.composites.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.awt.*;
import java.io.*;
import java.util.*;


import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.List;

public class TemporalDocument implements Serializable, IDataCollection<TemporalSentence> {
    private static final long serialVersionUID = -3478876003961945902L;
    final private static String DOCUMENT_FORMAT =
            "<?xml version=\"1.0\" ?>\n" +
                    "<TimeML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd\">\n" +
                    "<DOCID>%s</DOCID>\n" +
                    "<DCT><TIMEX3 tid=\"t0\" type=\"DATE\" value=\"%s\" temporalFunction=\"false\" functionInDocument=\"CREATION_TIME\">%s</TIMEX3></DCT>\n" +
                    "<TEXT>%s</TEXT>\n" + "%s" + "%s" +
                    "\n</TimeML>";
    private String docID;
    private String bodyText;
    private String documentCreationText;
    private Map<Pair<TemporalSentence, Integer>, Pair<Integer, Integer>> tokenToChar;
    private Map<Integer, Pair<TemporalSentence, Integer>> startCharToToken, endCharToToken;
    private List<TemporalJointChunk> bodyTimexMentions;
    private List<EventChunk> bodyEventMentions;
    private List<TLINK> bodyTlinks;
    private List<TLINK> bodyTlinks_original;//place-holder when saturation is used
    private List<TemporalSentence> bodySentences;

    private TextAnnotation textAnnotation;
    private TemporalJointChunk documentCreationMention;

    public TemporalDocument() {
        this.bodyTimexMentions = new LinkedList<TemporalJointChunk>();
        this.bodyEventMentions = new LinkedList<EventChunk>();
        this.bodyTlinks = new LinkedList<TLINK>();
        this.tokenToChar = new HashMap<Pair<TemporalSentence, Integer>, Pair<Integer, Integer>>();
    }

    public TemporalDocument(TemporalDocument other) {
        this.docID = other.docID;
        this.bodyText = other.bodyText;
        this.documentCreationText = other.documentCreationText;

        this.tokenToChar = other.tokenToChar;
        this.startCharToToken = other.startCharToToken;
        this.endCharToToken = other.endCharToToken;

        this.bodyTimexMentions = other.bodyTimexMentions;
        this.bodyEventMentions = other.bodyEventMentions;
        this.bodyTlinks = other.bodyTlinks;
        this.bodyTlinks_original = other.bodyTlinks_original;
        this.bodySentences = other.bodySentences;
        this.textAnnotation = other.textAnnotation;
        this.documentCreationMention = other.documentCreationMention;
    }

    private static String prefilterText(String s) {
        return s.replace("-", " ").replace("/", " ").replace("&", " ");
        //return s.replace("&", " ");
    }

    private static String postfilterText(String s) {
        return s.replace("\\/", "/");
    }

    public void setText(String text) {
        this.bodyText = text;
    }

    public String getDocID() {
        return docID;
    }

    public void setDocID(String docID) {
        this.docID = docID;
    }

    public void insertMention(TemporalJointChunk chunk) {
        bodyTimexMentions.add(chunk);
    }

    public void insertMention(EventChunk chunk) {
        bodyEventMentions.add(chunk);
    }

    public void insertMention(TLINK tlink) {
        bodyTlinks.add(tlink);
    }

    // For timeml with embedded mentions
    public void insertMention(String type, String value, String mod, int offset, int tid) {
        insertMention(new TemporalJointChunk(type, value, mod, offset, -1, tid));
    }

    public void insertEventMention(String eventclass, String tense, String aspect, int offset, int eid) {
        insertMention(new EventChunk(eventclass, tense, aspect, offset, -1, eid));
    }

    public void insertTlink(int lid, String relType, String sourceType, String targetType, int sourceId, int targetId) {
        insertMention(new TLINK(lid, relType, sourceType, targetType, sourceId, targetId));
    }

    // For clinical narratives with separate mentions
    public void insertMention(String type, String value, String mod, int startChar, int endChar, int tid) {
        insertMention(new TemporalJointChunk(type, value, mod, startChar, endChar, tid));
    }

    public void insertDCTMention(TemporalJointChunk chunk) {
        documentCreationMention = chunk;
    }

    public void insertDCTMention(String type, String value, String mod, int offset, int tid) {
        insertDCTMention(new TemporalJointChunk(type, value, mod, offset, -1, tid));
    }

    public void setLastMentionText(String text) {
        setLastMentionText(text, TempEval3Reader.Type_Timex);
    }

    public void setLastMentionText(String text, String type) {
        if (type.equals(TempEval3Reader.Type_Timex)) {
            bodyTimexMentions.get(bodyTimexMentions.size() - 1).setCharEnd(text);
        } else if (type.equals(TempEval3Reader.Type_Event)) {
            bodyEventMentions.get(bodyEventMentions.size() - 1).setCharEnd(text);
            bodyEventMentions.get(bodyEventMentions.size() - 1).setText(text);
        } else {
            System.out.println("Undefined type in TemporalDocument-->setLastMentionText");
            System.exit(-1);
        }
    }

    public void setDCTMentionText(String text) {
        documentCreationMention.setCharEnd(text);
    }

    public void setDCTText(String text) {
        documentCreationText = text;
    }

    public boolean makeInstance(int eid, int eiid, String tense, String aspect, String polarity, String pos, String cardinality) {
        //Find eid
        boolean success = false;
        for (EventChunk ec : bodyEventMentions) {
            if (ec.getEid() != eid)
                continue;
            success = true;
            //Modify EventChunk
            ec.setEiid(eiid);
            ec.setPolarity(polarity);
            ec.setPos(pos);
            ec.setTense(tense);
            ec.setAspect(aspect);
            ec.setCardinality(cardinality);//break;
        }
        return success;
    }

    private List<TemporalSentence> getPreprocessedSentences(String text, List<TemporalJointChunk> mentions,
                                                            StanfordCoreNLP pipeline, GrammaticalStructureFactory gsf) {
        Annotation a = new Annotation(text);
        pipeline.annotate(a);

        startCharToToken = new HashMap<Integer, Pair<TemporalSentence, Integer>>();
        endCharToToken = new HashMap<Integer, Pair<TemporalSentence, Integer>>();

        List<TemporalSentence> sentences = new LinkedList<TemporalSentence>();

        for (CoreMap sentence : a.get(CoreAnnotations.SentencesAnnotation.class)) {
            TemporalSentence newSentence = new TemporalSentence(this, getDocumentCreationTime().getResult().getValue());
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                Pair<TemporalSentence, Integer> tokenIndex = Pair.of(newSentence, newSentence.getNumTokens());
                startCharToToken.put(token.beginPosition(), tokenIndex);
                endCharToToken.put(token.endPosition(), tokenIndex);
                tokenToChar.put(tokenIndex, Pair.of(token.beginPosition(), token.endPosition()));
                newSentence.insertToken(postfilterText(token.get(edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation.class)));
            }
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
            String dp = EnglishGrammaticalStructure.dependenciesToString(gs, gs.typedDependencies(false), tree, true, false);
            newSentence.saveDependencyParse(dp);
            sentences.add(newSentence);
        }

        for (TemporalJointChunk t : mentions)
            t.alignTokens(startCharToToken, endCharToToken);

        return sentences;
    }

    public void addSentenceToEventChunk(List<EventChunk> bodyEventMentions) {
        for (EventChunk e : bodyEventMentions)
            e.alignTokens(startCharToToken, endCharToToken);
    }

    public void doPreprocessing(StanfordCoreNLP pipeline, GrammaticalStructureFactory gsf) {
        bodySentences = getPreprocessedSentences(documentCreationText, Collections.singletonList(documentCreationMention), pipeline, gsf);
        bodySentences.addAll(getPreprocessedSentences(prefilterText(bodyText), bodyTimexMentions, pipeline, gsf));
        this.addSentenceToEventChunk(bodyEventMentions);
    }

    public TextAnnotation getTextAnnotation(){
        return textAnnotation;
    }
    public void clearTextAnnotation(){textAnnotation = null;}
    public void createTextAnnotation(AnnotatorService pipeline)throws Exception {
        boolean force_update = false;
        File serializedFile = new File("./serialized_data/textannotation/" + docID + "_ta.ser");
        if(serializedFile.exists()&&!force_update) {
            System.out.println("Serialization of "+docID +" exist. Loading from "+serializedFile.getPath());
            FileInputStream fileIn = new FileInputStream(serializedFile.getPath());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            textAnnotation = (TextAnnotation) in.readObject();
            in.close();
            fileIn.close();
            return;
        }
        try {
            textAnnotation = pipeline.createAnnotatedTextAnnotation( docID, "", bodyText );
            /*AnnotatorService annotator = CuratorFactory.buildCuratorClient();
            textAnnotation = annotator.createBasicTextAnnotation(docID, "", bodyText);
            annotator.addView(textAnnotation, ViewNames.TOKENS);
            annotator.addView(textAnnotation, ViewNames.SENTENCE);
            annotator.addView(textAnnotation, ViewNames.PARSE_CHARNIAK);
            annotator.addView(textAnnotation, ViewNames.COREF);
            annotator.addView(textAnnotation, ViewNames.SRL_VERB);
            annotator.addView(textAnnotation, ViewNames.SRL_NOM);
            annotator.addView(textAnnotation, ViewNames.SRL_PREP);*/
            FileOutputStream fileOut = new FileOutputStream(serializedFile.getPath());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(textAnnotation);
            out.close();
            fileOut.close();
            System.out.println("Serialization of "+docID+" has been saved to "+serializedFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TemporalJointChunk getDocumentCreationTime() {
        return documentCreationMention;
    }

    public String getDocumentCreationText() {
        return documentCreationText;
    }

    public List<TemporalSentence> getSentences() {
        return bodySentences;
    }

    public String toString() {
        return bodyText;
    }

    public void printAll() {
        System.out.println("/***********docID: " + docID + ": DCT: " + documentCreationText + "***********/");
        System.out.println("\n-----------Event list-----------\n");
        for (EventChunk ec : bodyEventMentions) {
            System.out.println(ec.toStringConcise());
            for (TLINK tlink : bodyTlinks) {
                if (!tlink.getSourceType().equals(TempEval3Reader.Type_Event) ||
                        tlink.getSourceId() != ec.getEiid())
                    continue;
                if (tlink.getTargetType().equals(TempEval3Reader.Type_Event)) {
                    int eid = -1;
                    for (EventChunk tmp : bodyEventMentions) {
                        if (tmp.getEiid() == tlink.getTargetId()) {
                            eid = tmp.getEid();
                        }
                    }
                    System.out.println("\t--->" + tlink.getTargetType() + "\t" + eid + "\t\t" + tlink.getRelType());
                } else
                    System.out.println("\t--->" + tlink.getTargetType() + "\t" + tlink.getTargetId() + "\t\t" + tlink.getRelType());
            }
            System.out.println("\n");
        }
        System.out.println("\n-----------Timex list-----------\n");
        for (TemporalJointChunk timex : bodyTimexMentions) {
            System.out.println(timex.toStringConcise());
            for (TLINK tlink : bodyTlinks) {
                if (!tlink.getSourceType().equals(TempEval3Reader.Type_Timex) ||
                        tlink.getSourceId() != timex.getTID())
                    continue;
                System.out.println("\t--->" + tlink.getTargetType() + "\t\t" + tlink.getTargetId() + "\t\t" + tlink.getRelType());
            }
            System.out.println("\n");
        }
        System.out.println("\n");
    }

    public void createJavaScript(String dir){createJavaScript(dir,0);}
    public void createJavaScript(String dir, int mode) {
        IOUtils.mkdir(dir);
        GraphJavaScript gjs = new GraphJavaScript(dir + File.separator + docID + ".html");
        /*Add Vertices*/
        if(mode!=1) {
            for (EventChunk ec : bodyEventMentions)
                gjs.addVertex(ec);
        }
        if(mode!=3) {
            gjs.addVertex(documentCreationMention);
            for (TemporalJointChunk timex : bodyTimexMentions)
                gjs.addVertex(timex);
        }
        /*Add tlinks*/
        if(mode==1)
            bodyTlinks = getTTlinks();
        else if(mode==2)
            bodyTlinks = getETlinks();
        else if(mode==3)
            bodyTlinks = getEElinks();
        for (TLINK tlink : bodyTlinks) {
            if (tlink.getReducedRelType().equals(TlinkType.AFTER)
                    || tlink.getReducedRelType().equals(TlinkType.INCLUDES))
                gjs.addEdge(tlink.converse());
            else if (tlink.getReducedRelType().equals(TlinkType.BEFORE)
                    || tlink.getReducedRelType().equals(TlinkType.IS_INCLUDED)
                    || tlink.getReducedRelType().equals(TlinkType.EQUAL))
                gjs.addEdge(tlink);
            else
                System.out.println("TLINK type undefined: " + tlink.getRelType());
        }

        /*The follow snippet is no longer needed given orderTimexes()*/
        /*if(mode>0) {
            *//*Add relations between timexes*//*
            for (TemporalJointChunk timex1 : bodyTimexMentions) {
                gjs.addEdge(documentCreationMention, timex1, documentCreationMention);
                for (TemporalJointChunk timex2 : bodyTimexMentions) {
                    gjs.addEdge(timex1, timex2, documentCreationMention);
                }
            }
        }*/
        gjs.createJS();
    }

    public Pair<Integer, Integer> getCharRange(TemporalSentence sentence, int i) {
        return tokenToChar.get(Pair.of(sentence, i));
    }

    public Map<Pair<TemporalSentence, Integer>, Pair<Integer, Integer>> getTokenToChar() {
        return tokenToChar;
    }

    public Pair<TemporalSentence, Integer> getTokenFromStartChar(int startChar) {
        return startCharToToken.get(startChar);
    }

    public Pair<TemporalSentence, Integer> getTokenFromEndChar(int endChar) {
        return endCharToToken.get(endChar);
    }

    @Override
    public Iterator<TemporalSentence> iterator() {
        return bodySentences.iterator();
    }

    @Override
    public int size() {
        return bodySentences.size();
    }

    public String annotatePredictions(List<Pair<TemporalSentence, TemporalJointChunk>> allPredictions) {
        char[] originalDocumentText = bodyText.toCharArray();
        Map<Integer, String> annotationInsertionMap = new HashMap<Integer, String>();
        int tidCount = 1;
        for (Pair<TemporalSentence, TemporalJointChunk> prediction : allPredictions) {
            Pair<Integer, Integer> startTokenSpan = tokenToChar.get(Pair.of(prediction.first(), prediction.second().getStart()));
            Pair<Integer, Integer> endTokenSpan = tokenToChar.get(Pair.of(prediction.first(), prediction.second().getEnd()));
            annotationInsertionMap.put(startTokenSpan.first(), prediction.second().getResult().beginAnnotation("t" + tidCount));
            annotationInsertionMap.put(endTokenSpan.second(), prediction.second().getResult().endAnnotation());
            tidCount++;
        }

        StringBuilder annotatedDocument = new StringBuilder();
        for (int i = 0; i < originalDocumentText.length; i++) {
            if (annotationInsertionMap.containsKey(i))
                annotatedDocument.append(annotationInsertionMap.get(i));
            annotatedDocument.append(originalDocumentText[i]);
        }
        return annotate(annotatedDocument.toString());
    }

    public String annotate(String text) {
        return String.format(DOCUMENT_FORMAT, docID, getDocumentCreationTime().getResult().getValue(), getDocumentCreationTime().getPhrase(), text.replace("&", "&amp;"));
    }

    public String getOriginalText() {
        return bodyText;
    }

    public String getOriginalText(int charStart, int charEnd) {
        return bodyText.substring(charStart, charEnd);
    }

    public TemporalDocument withoutDCT() {
        TemporalDocument newDoc = new TemporalDocument(this);
        newDoc.bodySentences.remove(documentCreationMention.getSentence());
        newDoc.bodyTimexMentions.remove(documentCreationMention);
        return newDoc;
    }

    public String dump() {
        char[] originalDocumentText = bodyText.toCharArray();
        Map<Integer, List<String>> annotationInsertionMap = new HashMap<Integer, List<String>>();
        int tidCount = 1;
        for (TemporalSentence ts : this) {
            List<TemporalJointChunk> annotatedChunks = new LinkedList<TemporalJointChunk>();
            for (TemporalJointChunk goldChunk : ts.getLabel()) {
                boolean hasOverlap = false;
                for (TemporalJointChunk possibleOverlap : annotatedChunks)
                    if (possibleOverlap.overlapsWith(goldChunk)) {
                        hasOverlap = true;
                        break;
                    }
                if (hasOverlap || goldChunk.getEnd() < goldChunk.getStart())
                    continue;
                MentionResult goldResult = goldChunk.getResult();
                String tid = "t" + (goldChunk.getTID() != -1 ? goldChunk.getTID() : tidCount);
                Pair<Integer, Integer> startTokenSpan = tokenToChar.get(Pair.of(ts, goldChunk.getStart()));
                Pair<Integer, Integer> endTokenSpan = tokenToChar.get(Pair.of(ts, goldChunk.getEnd()));

                if (!annotationInsertionMap.containsKey(startTokenSpan.first()))
                    annotationInsertionMap.put(startTokenSpan.first(), new LinkedList<String>());
                annotationInsertionMap.get(startTokenSpan.first()).add(goldResult.beginAnnotation(tid));

                if (!annotationInsertionMap.containsKey(endTokenSpan.second()))
                    annotationInsertionMap.put(endTokenSpan.second(), new LinkedList<String>());
                annotationInsertionMap.get(endTokenSpan.second()).add(goldResult.endAnnotation());
                tidCount++;
                annotatedChunks.add(goldChunk);
            }
        }
        StringBuilder annotatedDocument = new StringBuilder();
        for (int i = 0; i < originalDocumentText.length; i++) {
            if (annotationInsertionMap.containsKey(i))
                for (String s : annotationInsertionMap.get(i))
                    annotatedDocument.append(s);
            annotatedDocument.append(originalDocumentText[i]);
        }
        return annotate(annotatedDocument.toString());
    }

    public List<EventChunk> getBodyEventMentions() {
        return bodyEventMentions;
    }

    public void setBodyTimexMentions(List<TemporalJointChunk> bodyTimexMentions) {
        this.bodyTimexMentions = bodyTimexMentions;
    }

    public List<TemporalJointChunk> getBodyTimexMentions() {
        return bodyTimexMentions;
    }

    public List<TLINK> getBodyTlinks() {
        return bodyTlinks;
    }

    public int getIndexFromEIID(int eiid){
        for(int i=0;i<bodyEventMentions.size();i++){
            if(bodyEventMentions.get(i).getEiid()==eiid){
                return i;
            }
        }
        return -1;
    }
    public EventChunk getEventMentionFromEIID(int eiid){return getEventMentionFromEIID(eiid,true);}
    public EventChunk getEventMentionFromEIID(int eiid,boolean debug) {
        for (EventChunk ec : bodyEventMentions) {
            if (ec.getEiid() == eiid) {
                return ec;
            }
        }
        if(debug)
            System.out.println("docID: " + docID + " cannot find event (eiid=" + eiid + ")");
        return null;
    }
    public EventChunk getEventMentionFromEID(int eid){return getEventMentionFromEID(eid,true);}
    public EventChunk getEventMentionFromEID(int eid,boolean debug) {
        for (EventChunk ec : bodyEventMentions) {
            if (ec.getEid() == eid) {
                return ec;
            }
        }
        if(debug)
            System.out.println("docID: " + docID + " cannot find event (eid=" + eid + ")");
        return null;
    }
    public int getSentId(EventChunk ec){
        if(textAnnotation==null)
            return -1;
        EventStruct targetEvent = new EventStruct(ec,textAnnotation);
        IntPair targetEventOff = targetEvent.getPrimaryTriggerWordOffset();
        return textAnnotation.getSentenceId(targetEventOff.getFirst());
    }

    public TemporalJointChunk getTimexMentionFromTID(int tid){return getTimexMentionFromTID(tid,true);}
    public TemporalJointChunk getTimexMentionFromTID(int tid,boolean debug){
        if(tid==0)
            return documentCreationMention;
        for(TemporalJointChunk t:bodyTimexMentions){
            if(t.getTID()==tid)
                return t;
        }
        if (debug)
            System.out.println("docID: " + docID + " cannot find timex (tid=" + tid + ")");
        return null;
    }

    public void deleteSpacesInEvents(){
        String text;
        for(EventChunk ec:bodyEventMentions){
            text = ec.getText();
            while(text.startsWith(" ")){
                text = text.substring(1);
                ec.setText(text);
                ec.setCharStart(ec.getCharStart()+1);
            }
            while(text.endsWith(" ")){
                text = text.substring(0,text.length()-1);
                ec.setText(text);
                ec.setCharEnd(ec.getCharEnd()-1);
            }
        }
    }

    public String getObjectTypeIdStr(Object o1){
        Pair<String,Integer> pair = getObjectTypeId(o1);
        return pair.first().substring(0,1)+pair.second();
    }
    public Pair<String, Integer> getObjectTypeId(Object o1){
        if(o1==null)
            return null;
        String type = "";
        int id = -1;
        if(o1 instanceof EventChunk) {
            type = TempEval3Reader.Type_Event;
            id = ((EventChunk) o1).getEiid();
        }
        else if(o1 instanceof TemporalJointChunk){
            type = TempEval3Reader.Type_Timex;
            id = ((TemporalJointChunk) o1).getTID();
        }
        else{
            System.out.println("Wrong usage of getObjectTypeId");
            System.exit(-1);
        }
        return Pair.of(type,id);
    }
    @Nullable
    public TLINK getTlink(Object o1, Object o2){
        return getTlink(o1,o2,false);
    }
    public TLINK getTlink(Object o1, Object o2, boolean original_or_not){
        List<TLINK> tlink2check = original_or_not&&bodyTlinks_original!=null?//when bodyTlinks_original==null, the doc isn't saturated.
                bodyTlinks_original:bodyTlinks;
        Pair<String, Integer> p1 = getObjectTypeId(o1), p2 = getObjectTypeId(o2);
        if(p1==null||p2==null)
            return null;
        String sourceType = p1.first();
        String targetType = p2.first();
        int sourceId = p1.second(), targetId = p2.second();
        for(TLINK tlink:tlink2check){
            if(!tlink.getSourceType().equals(sourceType)
                    || !tlink.getTargetType().equals(targetType))
                continue;
            if(tlink.getSourceId()==sourceId
                    && tlink.getTargetId()==targetId)
                return tlink;
        }
        return null;
    }
    public TlinkType getTlinkType(Object o1, Object o2){
        TLINK tlink = getTlink(o1,o2);
        return tlink!=null?tlink.getReducedRelType():null;
    }
    public TlinkType getTlinkType_general(Object o1, Object o2){
        TLINK tlink = getTlink(o1,o2);
        if(tlink!=null)
            return tlink.getReducedRelType();
        TLINK tlink_r = getTlink(o2,o1);
        if(tlink_r!=null)
            return tlink_r.converse().getReducedRelType();
        return null;
    }
    public boolean checkTlinkExistence(TLINK tlink){
        boolean ext = false;
        for(TLINK t:bodyTlinks){
            if(t.equals(tlink)){
                ext = true;
                break;
            }
        }
        return ext;
    }
    public int getMaxLID(){
        if(bodyTlinks==null||bodyTlinks.size()==0)
            return -1;
        int maxTID = bodyTlinks.get(0).getLid();
        for(int i=1;i<bodyTlinks.size();i++){
            if(maxTID<bodyTlinks.get(i).getLid()){
                maxTID = bodyTlinks.get(i).getLid();
            }
        }
        return maxTID;
    }
    public void saturateTlinks()throws Exception{saturateTlinks(false);}
    public void saturateTlinks(boolean debug)throws Exception{saturateTlinks(Integer.MAX_VALUE,debug);}

    public List<TLINK> getBodyTlinks_original() {
        return bodyTlinks_original;
    }

    public void saturateTlinks(int maxIter, boolean debug) throws Exception{
        saturateTlinks(maxIter,debug,false);
    }
    public void saturateTlinks(int maxIter, boolean debug, boolean force_update) throws Exception{
        saturateTlinks(maxIter,debug,force_update,true);
    }
    public void saturateTlinks(int maxIter, boolean debug, boolean force_update, boolean serialize) throws Exception{
        /*deep copy bodyTlinks to bodyTlinks_original*/
        bodyTlinks_original = new ArrayList<>();
        for(TLINK tlink : bodyTlinks)
            bodyTlinks_original.add(tlink.deepCopy());

        File serializedFile = new File("./serialized_data/saturation/" + docID + "_sat.ser");
        if(serializedFile.exists()&&!force_update) {
            System.out.println("Serialization of "+docID +" exist. Loading from "+serializedFile.getPath());
            FileInputStream fileIn = new FileInputStream(serializedFile.getPath());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            bodyTlinks = (List<TLINK>) in.readObject();
            in.close();
            fileIn.close();
        }
        else {
            if(force_update)
                System.out.println("Force update: "+docID);
            else
                System.out.println("Serialization of "+docID +" doesn't exist.");
            int lid = getMaxLID() + 1;
            boolean bodyTlinksUpdated = true;
            int cnt = 0;
            HashMap<String, List<Pair<String, String>>> safeIgnores = new HashMap<>();
            List<Object> mentions = new ArrayList<Object>();//place holder for both event and timex
            mentions.addAll(bodyEventMentions);
            mentions.addAll(bodyTimexMentions);
            int iter = 0;
            while (bodyTlinksUpdated) {
                if (iter >= maxIter)
                    break;
                iter++;

                bodyTlinksUpdated = false;
                /*Symmetry*/
                List<TLINK> newtlinks = new ArrayList<>();
                for (int i = cnt; i < bodyTlinks.size(); i++) {
                    TLINK tlink = bodyTlinks.get(i);
                    TLINK reverseLink = tlink.converse();
                    if (debug) {
                        TemporalLog.print("SaturationProgress",
                                docID + ": Symmetry: " + tlink.toStringConcise() + "-->" + reverseLink.toStringConcise() + ". ");
                    }
                    if (!checkTlinkExistence(reverseLink)) {
                        reverseLink.setLid(lid);
                        newtlinks.add(reverseLink);
                        lid++;
                        bodyTlinksUpdated = true;
                        if (debug)
                            TemporalLog.println("SaturationProgress",
                                    "Added!");
                    } else {
                        if (debug)
                            TemporalLog.println("SaturationProgress",
                                    "Already exists!");
                    }
                }
                cnt = bodyTlinks.size();

                /*Transitivity*/
                TlinkType tt12, tt23, tt13 = null;
                TLINK tlink12, tlink23;
                for (Object ec1 : mentions) {
                    Pair<String, Integer> p1 = getObjectTypeId(ec1);
                    for (Object ec2 : mentions) {
                        if (ec1.equals(ec2))
                            continue;//same event
                        Pair<String, Integer> p2 = getObjectTypeId(ec2);
                        tlink12 = getTlink(ec1, ec2);
                        tt12 = tlink12 != null ? tlink12.getReducedRelType() : null;
                        if (tt12 == null || tt12 == TlinkType.UNDEF)
                            continue;//no rel existing
                        for (Object ec3 : mentions) {
                            if (ec3.equals(ec1) || ec3.equals(ec2))
                                continue;//same event
                            tlink23 = getTlink(ec2, ec3);
                            tt23 = tlink23 != null ? tlink23.getReducedRelType() : null;
                            boolean update = false;
                            if (tt23 == null || tt23 == TlinkType.UNDEF)
                                continue;//no rel existing
                            /*Check if the current triplet can be safely ignored*/
                            Pair<String, Integer> p3 = getObjectTypeId(ec3);
                            String p1_key = p1.first().substring(0, 1) + p1.second();
                            String p2_key = p2.first().substring(0, 1) + p2.second();
                            String p3_key = p3.first().substring(0, 1) + p3.second();
                            if (safeIgnores.containsKey(p1_key)
                                    && safeIgnores.get(p1_key).contains(Pair.of(p2_key, p3_key)))
                                continue;
                            /*Add current triplet and its reverse to a safe-to-ignore set*/
                            if (safeIgnores.containsKey(p1_key)) {
                                safeIgnores.get(p1_key).add(Pair.of(p2_key, p3_key));
                            } else {
                                List<Pair<String, String>> tmp = new ArrayList<>();
                                tmp.add(Pair.of(p2_key, p3_key));
                                safeIgnores.put(p1_key, tmp);
                            }
                            if (safeIgnores.containsKey(p3_key)) {
                                safeIgnores.get(p3_key).add(Pair.of(p2_key, p1_key));
                            } else {
                                List<Pair<String, String>> tmp = new ArrayList<>();
                                tmp.add(Pair.of(p2_key, p1_key));
                                safeIgnores.put(p3_key, tmp);
                            }
                            List<TransitivityTriplets> transTriplets = TransitivityTriplets.transTriplets();
                            for (TransitivityTriplets triplet : transTriplets) {
                                int n = triplet.getThird().length;
                                if (n > 1)
                                    continue;
                                if (tt12 == triplet.getFirst()
                                        && tt23 == triplet.getSecond()) {
                                    tt13 = triplet.getThird()[0];
                                    update = true;
                                }
                            }
                            if (update) {
                                String relType = "";
                                switch (tt13) {
                                    case BEFORE:
                                        relType = "BEFORE";
                                        break;
                                    case AFTER:
                                        relType = "AFTER";
                                        break;
                                    case EQUAL:
                                        relType = "SIMULTANEOUS";
                                        break;
                                    case INCLUDES:
                                        relType = "INCLUDES";
                                        break;
                                    case IS_INCLUDED:
                                        relType = "IS_INCLUDED";
                                        break;
                                    default:
                                        System.out.println("TlinkType undefined.");
                                        System.exit(-1);
                                }
                                TLINK tmplink = new TLINK(lid, relType, p1.first(), p3.first(), p1.second(), p3.second());
                                if (debug) {
                                    TemporalLog.print("SaturationProgress",
                                            docID + ": Transitivity: " + tlink12.toStringConcise() + "+" + tlink23.toStringConcise() + " --> " + tmplink.toStringConcise() + ". ");
                                }
                                if (!checkTlinkExistence(tmplink)) {
                                    newtlinks.add(tmplink);
                                    lid++;
                                    bodyTlinksUpdated = true;
                                    if (debug)
                                        TemporalLog.println("SaturationProgress",
                                                "Added!");
                                } else {
                                    if (debug)
                                        TemporalLog.println("SaturationProgress",
                                                "Already exists!");
                                }
                            }
                        }
                    }
                }
                newtlinks = TLINK.removeDuplicates(newtlinks);
                bodyTlinks.addAll(newtlinks);
            }
            if(serialize) {
                FileOutputStream fileOut = new FileOutputStream(serializedFile.getPath());
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(bodyTlinks);
                out.close();
                fileOut.close();
                System.out.println("Serialization of " + docID + " has been saved to " + serializedFile.getPath());
            }
        }

    }
    public void removeDuplicatedTlinks(){
        List<TLINK> newtlinks = TLINK.removeDuplicates(bodyTlinks);
        bodyTlinks = newtlinks;
    }
    public void addVagueTlinks(double th){
        /*th: only keep a small portion of the none links*/
        List<Object> mentions = new ArrayList<Object>();//place holder for both event and timex
        mentions.addAll(bodyEventMentions);
        mentions.addAll(bodyTimexMentions);
        int lid = getMaxLID()+1;
        /*Add vague tlinks*/
        for(Object o1:mentions){
            for(Object o2:mentions){
                if(o1==o2){
                    continue;
                }
                if(Math.random()>th)
                    continue;
                TLINK tlink = getTlink(o1,o2);
                if(tlink==null){
                    Pair<String, Integer> p1 = getObjectTypeId(o1), p2 = getObjectTypeId(o2);
                    String sourceType = p1.first();
                    String targetType = p2.first();
                    int sourceId = p1.second(), targetId = p2.second();
                    bodyTlinks.add(new TLINK(lid,"undef",sourceType,targetType,sourceId,targetId));
                    lid++;
                }
            }
        }
    }
    public void removeVagueTlinks(){
        List<TLINK> newlinks = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            if(tlink.getReducedRelType().equals(TlinkType.UNDEF))
                continue;
            newlinks.add(tlink);
        }
        bodyTlinks = newlinks;
    }
    public void orderTimexes(){
        for (TemporalJointChunk timex1 : bodyTimexMentions) {
            TLINK tlink = TlinkTimexes(timex1,documentCreationMention,documentCreationMention);
            if(tlink!=null&&!checkTlinkExistence(tlink))
                bodyTlinks.add(tlink);
            for (TemporalJointChunk timex2 : bodyTimexMentions) {
                tlink = TlinkTimexes(timex1,timex2,documentCreationMention);
                if(tlink!=null&&!checkTlinkExistence(tlink))
                    bodyTlinks.add(tlink);
            }
        }
    }
    public TLINK TlinkTimexes(TemporalJointChunk timex1, TemporalJointChunk timex2, TemporalJointChunk dct){
        if(timex1.equals(timex2))
            return null;
        TLINK.TlinkType comp = timex1.compareResult(timex2.getResult(),dct.getResult());
        if(comp.equals(TLINK.TlinkType.UNDEF))
            return null;
        int lid = getMaxLID()+1;
        String relType = "";
        switch(comp){
            case BEFORE:
                relType = "BEFORE";
                break;
            case AFTER:
                relType = "AFTER";
                break;
            case EQUAL:
                relType = "SIMULTANEOUS";
                break;
            case INCLUDES:
                relType = "INCLUDES";
                break;
            case IS_INCLUDED:
                relType = "IS_INCLUDED";
                break;
            default:
                System.out.println("TlinkType undefined.");
        }
        return new TLINK(lid,relType,TempEval3Reader.Type_Timex,TempEval3Reader.Type_Timex,timex1.getTID(),timex2.getTID());
    }

    public void setBodyTlinks(List<TLINK> bodyTlinks) {
        this.bodyTlinks = bodyTlinks;
    }
    public void removeETlinks(){
        List<TLINK> newlinks = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            if(tlink.getSourceType().equals(TempEval3Reader.Type_Event)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Timex)
                    ||tlink.getSourceType().equals(TempEval3Reader.Type_Timex)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Event))
                continue;
            newlinks.add(tlink);
        }
        bodyTlinks = newlinks;
    }
    public void removeEElinks(){
        List<TLINK> newlinks = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            if(tlink.getSourceType().equals(TempEval3Reader.Type_Event)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Event))
                continue;
            newlinks.add(tlink);
        }
        bodyTlinks = newlinks;
    }
    public void removeTTlinks(){
        List<TLINK> newlinks = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            if(tlink.getSourceType().equals(TempEval3Reader.Type_Timex)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Timex))
                continue;
            newlinks.add(tlink);
        }
        bodyTlinks = newlinks;
    }
    public List<TLINK> getETlinks(){
        List<TLINK> ETlinks = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            if(tlink.getSourceType().equals(TempEval3Reader.Type_Event)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Timex)
                    ||tlink.getSourceType().equals(TempEval3Reader.Type_Timex)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Event))
                ETlinks.add(tlink);
        }
        return ETlinks;
    }
    public List<TLINK> getTTlinks(){
        List<TLINK> TTlinks = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            if(tlink.getSourceType().equals(TempEval3Reader.Type_Timex)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Timex))
                TTlinks.add(tlink);
        }
        return TTlinks;
    }
    public List<TLINK> getEElinks(){
        List<TLINK> EElinks = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            if(tlink.getSourceType().equals(TempEval3Reader.Type_Event)&&
                    tlink.getTargetType().equals(TempEval3Reader.Type_Event))
                EElinks.add(tlink);
        }
        return EElinks;
    }
    public HashMap<Integer,List<Integer>> getEENONEmap(){
        return getEENONEmap(false);
    }
    public HashMap<Integer,List<Integer>> getEENONEmap(boolean index_or_eiid){
        //index_or_eiid:
        //true: index
        //false: eiid
        HashMap<Integer,List<Integer>> eeMaps = new HashMap<>();
        for(EventChunk ec1:bodyEventMentions){
            int id1 = index_or_eiid?bodyEventMentions.indexOf(ec1):ec1.getEiid();
            for(EventChunk ec2:bodyEventMentions){
                int id2 = index_or_eiid?bodyEventMentions.indexOf(ec2):ec2.getEiid();
                if(ec1==ec2)
                    continue;
                TLINK tlink = getTlink(ec1,ec2);
                if(tlink==null||tlink.getReducedRelType()==TlinkType.UNDEF){
                    if(eeMaps.containsKey(id1)){
                        eeMaps.get(id1).add(id2);
                    }
                    else{
                        List<Integer> newlist = new ArrayList<>();
                        newlist.add(id2);
                        eeMaps.put(id1,newlist);
                    }
                }
            }
        }
        return eeMaps;
    }
    /*
    * When adding tlinks, some of them may not be valid (say, wrong event id or timex id).
    * */
    public boolean validateTlink(TLINK tlink){
        boolean valid = true;
        switch (tlink.getSourceType()) {
            case TempEval3Reader.Type_Event:
                if (getEventMentionFromEIID(tlink.getSourceId(),false) == null)
                    valid = false;
                break;
            case TempEval3Reader.Type_Timex:
                if (getTimexMentionFromTID(tlink.getSourceId(),false) == null)
                    valid = false;
                break;
            default:
                valid = false;
                break;
        }
        switch (tlink.getTargetType()) {
            case TempEval3Reader.Type_Event:
                if (getEventMentionFromEIID(tlink.getTargetId(),false) == null)
                    valid = false;
                break;
            case TempEval3Reader.Type_Timex:
                if (getTimexMentionFromTID(tlink.getTargetId(),false) == null)
                    valid = false;
                break;
            default:
                valid = false;
                break;
        }
        return valid;
    }

    public List<TemporalJointChunk> deepCopyTimex(){
        List<TemporalJointChunk> newtimex = new ArrayList<>();
        for(TemporalJointChunk tjc:getBodyTimexMentions()){
            newtimex.add(tjc.deepCopy());
        }
        return newtimex;
    }
    public List<TLINK> deepCopyTlink(){
        List<TLINK> newtlink = new ArrayList<>();
        for(TLINK tlink:bodyTlinks){
            newtlink.add(tlink.deepCopy());
        }
        return newtlink;
    }

    public void serialize(String dir, String name, boolean verbose) throws Exception{
        IOUtils.mkdir(dir);
        File serializedFile = new File(dir+File.separator+name+".ser");
        FileOutputStream fileOut = new FileOutputStream(serializedFile.getPath());
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(this);
        out.close();
        fileOut.close();
        if(verbose)
            System.out.println("Serialization of "+name+" has been saved to "+serializedFile.getPath());
    }
    public static TemporalDocument deserialize(String dir, String name, boolean verbose) throws  Exception{
        File serializedFile = new File(dir+File.separator+name+".ser");
        TemporalDocument doc = null;
        if(serializedFile.exists()){
            if(verbose)
                System.out.println("Serialization of "+ name +" exists. Loading from "+serializedFile.getPath());
            FileInputStream fileIn = new FileInputStream(serializedFile.getPath());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            doc = (TemporalDocument) in.readObject();
            in.close();
            fileIn.close();
        }
        else {
            if(verbose)
                System.out.println("Serialization of "+ name +" doesn't exist.");
        }
        return doc;
    }

    /**
     * Format using TempEval3, including tlinks and events
     * @param text
     * @param tlinks
     * @return
     */
    public String formatTempEval3(String text, String tlinks, String makeInstances) {
        return String.format(DOCUMENT_FORMAT,
                docID,
                getDocumentCreationTime().getResult().getValue(),
                getDocumentCreationTime().getPhrase(),
                text.replace("&", "&amp;"),
                makeInstances,
                tlinks);
    }
    /**
     * Write TemporalDocument to the designated output file
     * @param outputFilename
     */
    public void temporalDocumentToText(String outputFilename) {
        char[] originalDocumentText = bodyText.toCharArray();
        Map<Integer, String> timexInsertionMap = new HashMap<Integer, String>();
        Map<Integer, String> eventInsertionMap = new HashMap<Integer, String>();
        int tidCount = 1;
        int eidCount = 1;
        for (TemporalJointChunk prediction : this.bodyTimexMentions) {
            TemporalSentence origSentence = prediction.getSentence();
            Pair<Integer, Integer> startTokenSpan = tokenToChar.get(Pair.of(origSentence, prediction.getStart()));
            Pair<Integer, Integer> endTokenSpan = tokenToChar.get(Pair.of(origSentence, prediction.getEnd()));
            timexInsertionMap.put(startTokenSpan.first(), prediction.getResult().beginAnnotation("t" + prediction.getTID()));
            timexInsertionMap.put(endTokenSpan.second(), prediction.getResult().endAnnotation());
            tidCount++;
        }

        StringBuilder instanceBuilder = new StringBuilder();
        for (EventChunk prediction : this.bodyEventMentions) {
            TemporalSentence origSentence = prediction.getSentence();
            timexInsertionMap.put(prediction.getCharStart(), prediction.beginAnnotation());
            timexInsertionMap.put(prediction.getCharEnd(), prediction.endAnnotation());
            instanceBuilder.append(prediction.makeInstanceAnnotation());
        }

        StringBuilder annotatedDocument = new StringBuilder();
        for (int i = 0; i < originalDocumentText.length; i++) {
            if (timexInsertionMap.containsKey(i))
                annotatedDocument.append(timexInsertionMap.get(i));
            if (eventInsertionMap.containsKey(i))
                annotatedDocument.append(eventInsertionMap.get(i));
            annotatedDocument.append(originalDocumentText[i]);
        }

        StringBuilder tlinkBuilder = new StringBuilder();
        int lid = 0;
        for (TLINK tlink : bodyTlinks) {
            if(tlink.getReducedRelType()==TlinkType.UNDEF)
                continue;
            tlinkBuilder.append(tlink.toXmlAnnotation(lid));
            lid++;
        }

        String outputContent = formatTempEval3(annotatedDocument.toString(), tlinkBuilder.toString(), instanceBuilder.toString());
        try {
            PrintStream ps = new PrintStream(outputFilename);
            ps.print(outputContent);
            ps.close();
        } catch (FileNotFoundException e) {
            System.err.println("Unable to open file");
        }
    }
}
