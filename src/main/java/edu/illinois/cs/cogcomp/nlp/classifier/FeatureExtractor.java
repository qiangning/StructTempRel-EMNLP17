package edu.illinois.cs.cogcomp.nlp.classifier;

import edu.illinois.cs.cogcomp.core.datastructures.IQueryable;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.core.transformers.Predicate;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.EventStruct;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TimexStruct;
import edu.illinois.cs.cogcomp.nlp.util.wordnet.WNSim;
import edu.illinois.cs.cogcomp.nlp.utilities.ParseTreeProperties;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.utils.composites.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by qning2 on 11/28/16.
 * This FeatureExtractor were only for local EE/ET classifiers.
 */
public class FeatureExtractor {
    private static final String WORD_FEAT_STR = "WORD_FEAT";
    private static final String POS_FEAT_STR = "POS_FEAT";
    private static final String LEMMA_FEAT_STR = "LEMMA_FEAT";
    private static final String EVENT_HEAD_TOKEN_ID = "EVENT_HEAD_TOKEN_ID";
    private TemporalDocument doc;
    private List<String> eeFeatures = new ArrayList<>();
    private List<String> etFeatures = new ArrayList<>();

    private String[] connectivesArray = new String[] { "before", "after",
            "since", "hence", "thus", "because", "when", "while", "previously",
            "eventually", "initially", "recently", "meanwhile", "lately",
            "afterwards", "if", "even though", "although", "so that",
            "however", "nevertheless", "otherwise", "therefore", "as a result" };
    private Set<String> connectivesSet = new HashSet<String>();
    private String[] modalVerbs = new String[] { "will", "would", "could" };
    private Set<String> modalVerbSet = new HashSet<String>();
    private WNSim wnsim = null;

    public FeatureExtractor(TemporalDocument doc){
        this.doc = doc;
        for (String c : connectivesArray) {
            connectivesSet.add(c);
        }
        for (String mv : modalVerbs) {
            modalVerbSet.add(mv);
        }
        String pathWordNet = "data/WordNet-3.0";
        wnsim = WNSim.getInstance(pathWordNet);
    }
    public String getFeatureString(Map<String, Double> feature, String label) {
        List<String> featList = new ArrayList<String>(feature.keySet());
        Collections.sort(featList);
        int featSize = featList.size();
        StringBuffer featStr = new StringBuffer();
        for (int f = 0; f < featSize; f++) {
            String ff = featList.get(f);
            featStr.append(ff);
            featStr.append(ParamLBJ.FEAT_DELIMITER);
        }
        featStr.append(label);
        return featStr.toString();
    }

    public List<String> getEeFeatures() {
        return eeFeatures;
    }

    public List<String> getEtFeatures() {
        return etFeatures;
    }

    public void extractEEfeats_doc() {
        List<TLINK> tlinks = doc.getBodyTlinks();
        for (TLINK tlink : tlinks) {
            if (!tlink.getSourceType().equals(TempEval3Reader.Type_Event) || !tlink.getTargetType().equals(TempEval3Reader.Type_Event))
                continue;
            int eiid1 = tlink.getSourceId();
            int eiid2 = tlink.getTargetId();
            EventChunk ec1 = doc.getEventMentionFromEIID(eiid1);
            EventChunk ec2 = doc.getEventMentionFromEIID(eiid2);
            if (ec1 == null || ec2 == null)
                continue;
            try {
                eeFeatures.add(getFeatureString(extractEEfeats(ec1, ec2), tlink.getReducedRelType().toStringfull()));
                //eeFeatures.add(getFeatureString(extractEEfeats(ec1, ec2), tlink.getRelType()));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public void extractETfeats_doc() {
        List<TLINK> tlinks = doc.getBodyTlinks();
        for (TLINK tlink : tlinks) {
            if (!tlink.getSourceType().equals(TempEval3Reader.Type_Event) || !tlink.getTargetType().equals(TempEval3Reader.Type_Timex))
                continue;
            int eiid1 = tlink.getSourceId();
            int tid2 = tlink.getTargetId();
            EventChunk ec1 = doc.getEventMentionFromEIID(eiid1);
            TemporalJointChunk t2 = doc.getTimexMentionFromTID(tid2);
            if (ec1 == null || t2 == null)
                continue;
            try {
                etFeatures.add(getFeatureString(extractETfeats(ec1, t2), tlink.getReducedRelType().toStringfull()));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public void extractEEfeats_Vague_doc(double th){
        //@@change return type from void to List<String>
        List<EventChunk> mentions = doc.getBodyEventMentions();
        for(EventChunk o1:mentions){
            for(EventChunk o2:mentions){
                TLINK tlink = doc.getTlink(o1,o2);
                if(tlink==null){
                    if(Math.random()>th)
                        continue;
                    try {
                        eeFeatures.add(getFeatureString(extractEEfeats(o1, o2), TLINK.TlinkType.UNDEF.toStringfull()));
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void extractETfeats_Vague_doc(double th){
        for(EventChunk o1:doc.getBodyEventMentions()){
            for(TemporalJointChunk o2:doc.getBodyTimexMentions()){
                TLINK tlink = doc.getTlink(o1,o2);
                if(tlink==null){
                    if(Math.random()>th)
                        continue;
                    try {
                        etFeatures.add(getFeatureString(extractETfeats(o1, o2), TLINK.TlinkType.UNDEF.toStringfull()));
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private Map<String, String> extractWordPosLemmaFeats(
            TokenLabelView posView, TokenLabelView lemmaView,
            IntPair targetEventOffset) {
        List<Constituent> cons = posView.getConstituentsCoveringSpan(
                targetEventOffset.getFirst(), targetEventOffset.getSecond());
        assert (cons.size() > 0) : "ERROR:  Couldn't find event POS.\nPosView: "
                + posView + "\nEvent offset: " + targetEventOffset;
        String wordFeat, posFeat, lemmaFeat;
        int eventHeadTokenId = cons.get(0).getStartSpan();
        if (cons.size() == 1) {
            String label = cons.get(0).getLabel();
            wordFeat = cons.get(0).toString();
            posFeat = (label.startsWith("N") ? "N" : label);
            lemmaFeat = lemmaView.getConstituentAtToken(
                    cons.get(0).getStartSpan()).getLabel();
        } else if (cons.size() > 1) {
            Collections.sort(cons,
                    TextAnnotationUtilities.constituentStartComparator);
            Constituent conAtStart = cons.get(0);
            Constituent conAtEnd = cons.get(cons.size() - 1);
            String labelStart = cons.get(0).getLabel();
            String labelEnd = cons.get(cons.size() - 1).getLabel();
            if (labelStart.startsWith("V") || labelEnd.startsWith("V")) {
                eventHeadTokenId = (labelStart.startsWith("V") ? conAtStart
                        .getStartSpan() : conAtEnd.getStartSpan());
                wordFeat = posView.getConstituentAtToken(eventHeadTokenId)
                        .toString();
                posFeat = posView.getConstituentAtToken(eventHeadTokenId)
                        .getLabel();
                lemmaFeat = lemmaView.getConstituentAtToken(eventHeadTokenId)
                        .getLabel();
            } else if (labelStart.startsWith("N") || labelEnd.startsWith("N")) {
                eventHeadTokenId = (labelStart.startsWith("N") ? conAtStart
                        .getStartSpan() : conAtEnd.getStartSpan());
                wordFeat = posView.getConstituentAtToken(eventHeadTokenId)
                        .toString();
                posFeat = "N";
                lemmaFeat = lemmaView.getConstituentAtToken(eventHeadTokenId)
                        .getLabel();
            } else {
                eventHeadTokenId = cons.get(0).getStartSpan();
                wordFeat = posView.getConstituentAtToken(eventHeadTokenId)
                        .toString();
                posFeat = posView.getConstituentAtToken(eventHeadTokenId)
                        .getLabel();
                lemmaFeat = lemmaView.getConstituentAtToken(eventHeadTokenId)
                        .getLabel();
            }
        } else {
            wordFeat = "N/A";
            posFeat = "N/A";
            lemmaFeat = "N/A";
            eventHeadTokenId = -1;
        }
        Map<String, String> results = new HashMap<String, String>();
        results.put(WORD_FEAT_STR, wordFeat);
        results.put(POS_FEAT_STR, posFeat);
        results.put(LEMMA_FEAT_STR, lemmaFeat);
        results.put(EVENT_HEAD_TOKEN_ID, Integer.toString(eventHeadTokenId));
        return results;
    }
    public Map<String, Double> extractEEfeats(EventChunk e1, EventChunk e2) {
        Map<String, Double> features = new HashMap<String, Double>();
        TextAnnotation ta = doc.getTextAnnotation();
        EventStruct targetEvent1 = new EventStruct(e1,ta);
        EventStruct targetEvent2 = new EventStruct(e2,ta);

        List<EventChunk> bodyEventMentions = doc.getBodyEventMentions();
        List<EventStruct> events = new ArrayList<>();
        for (EventChunk ec : bodyEventMentions)
            events.add(new EventStruct(ec,ta));
        IntPair targetEvent1Off = targetEvent1.getPrimaryTriggerWordOffset();
        IntPair targetEvent2Off = targetEvent2.getPrimaryTriggerWordOffset();
        // Features storing
        List<String> leftFeatures = new ArrayList<String>();
        List<String> rightFeatures = new ArrayList<String>();

        // =======================
        // Start extract features
        // =======================

        // Event 1 first: the appearance order of events(before, after)

        String e1First = "E1_FIRST:"
                + (targetEvent1Off.getFirst() < targetEvent2Off.getFirst() ? "YES"
                : "NO");
        features.put(e1First, 1.0);
        leftFeatures.add(e1First);

        // Same sentence: do the events appear in the same sentence
        int e1SentId=0,e2SentId=0;
        try {
            e1SentId = ta.getSentenceId(targetEvent1Off.getFirst());
            e2SentId = ta.getSentenceId(targetEvent2Off.getFirst());
        }
        catch(Exception e){
            System.out.println();
        }
        String sameSent = "SAME_SENT:" + (e1SentId == e2SentId ? "YES" : "NO");
        features.put(sameSent, 1.0);
        rightFeatures.add(sameSent);

        // NUM_SENT_DIFF: quantized number of sentences between the target
        // event1 and the target event2
        int diffSent = Math.abs(e1SentId - e2SentId);
        /*if(diffSent>=1)
            throw new EmptyStackException();*/
        String numSentDiff;
        if (diffSent == 0) {
            numSentDiff = "NUM_SENT_DIFF:NONE";
        } else if (diffSent == 1) {
            numSentDiff = "NUM_SENT_DIFF:ONE";
        } else if (diffSent == 2) {
            numSentDiff = "NUM_SENT_DIFF:TWO";
        } else if (diffSent < 5) {
            numSentDiff = "NUM_SENT_DIFF:SOME";
        } else {
            numSentDiff = "NUM_SENT_DIFF:ALOT";
        }
        features.put(numSentDiff, 1.0);
        rightFeatures.add(numSentDiff);

        // Event between: for a related event/event pair, do any other events
        // appear between them?
        int ce = 0;
        for (EventStruct event : events) {
            int eoff = event.getPrimaryTriggerWordOffset().getFirst();
            if ((eoff > targetEvent1Off.getFirst() && eoff < targetEvent2Off
                    .getFirst())
                    || (eoff < targetEvent1Off.getFirst() && eoff > targetEvent2Off
                    .getFirst())) {
                ce++;
            }
        }
        if (ce == 0) {
            features.put("E_BETWEEN:NO", 1.0);
            features.put("E_BETWEEN_COUNT:NONE", 1.0);
        } else {
            features.put("E_BETWEEN:YES", 1.0);
            if (ce < 3) {
                features.put("E_BETWEEN_COUNT:FEW", 1.0);
            } else if (ce < 6) {
                features.put("E_BETWEEN_COUNT:SOME", 1.0);
            } else {
                features.put("E_BETWEEN_COUNT:ALOT", 1.0);
            }
        }
        /*------
         E_WORD
		 E_POS
		 E_LEMMA
		 COL_POS1_POS2
		-------*/
        TokenLabelView posView = (TokenLabelView) ta.getView(ViewNames.POS);
        TokenLabelView lemmaView = (TokenLabelView) ta.getView(ViewNames.LEMMA);

        Map<String, String> wplFeats1 = extractWordPosLemmaFeats(posView,
                lemmaView, targetEvent1Off);
        Map<String, String> wplFeats2 = extractWordPosLemmaFeats(posView,
                lemmaView, targetEvent2Off);
        String e1Word = "E1_WORD:" + wplFeats1.get(WORD_FEAT_STR);
        features.put(e1Word, 1.0);
        rightFeatures.add(e1Word);
        String e1Pos = "E1_POS:" + wplFeats1.get(POS_FEAT_STR);
        features.put(e1Pos, 1.0);
        rightFeatures.add(e1Pos);
        String e1Lemma = "E1_LEMMA:" + wplFeats1.get(LEMMA_FEAT_STR);
        features.put(e1Lemma, 1.0);
        rightFeatures.add(e1Lemma);
        String e2Word = "E2_WORD:" + wplFeats2.get(WORD_FEAT_STR);
        features.put(e2Word, 1.0);
        rightFeatures.add(e2Word);
        String e2Pos = "E2_POS:" + wplFeats2.get(POS_FEAT_STR);
        features.put(e2Pos, 1.0);
        rightFeatures.add(e2Pos);
        String e2Lemma = "E2_LEMMA:" + wplFeats2.get(LEMMA_FEAT_STR);
        features.put(e2Lemma, 1.0);
        rightFeatures.add(e2Lemma);
        //
        features.put("COL_POS1_POS2:" + wplFeats1.get(POS_FEAT_STR) + "_"
                + wplFeats2.get(POS_FEAT_STR), 1.0);

        /*------
		 E_TENSE
		 E_ASPECT
		 E_CLASS
		 E_POLARITY
		 COL_E_TENSE_ASPECT
		 COL_E_POS_TENSE : hurts!
		 COL_E_POS_ASPECT
		 COL_E_POS_TENSE_ASPECT
		 COL_TENSE1_TENSE2
		 COL_ASPECT1_ASPECT2
		 COL_TENSE1_ASPECT1_TENSE2_ASPECT2
		-------*/

        String e1Tense = "E1_TENSE:" + e1.getTense();
        features.put(e1Tense, 1.0);
        rightFeatures.add(e1Tense);
        String e1Aspect = "E1_ASPECT:" + e1.getAspect();
        features.put(e1Aspect, 1.0);
        rightFeatures.add(e1Aspect);
        String e1Class = "E1_CLASS:"+e1.getEventclass();
        features.put(e1Class,1.0);
        rightFeatures.add(e1Class);
        String e1Polarity = "E1_POLARITY:"+e1.getPolarity();
        features.put(e1Polarity,1.0);
        rightFeatures.add(e1Polarity);

        features.put("COL_E1_TENSE_ASPECT:" + e1.getTense()
                + "_" + e1.getAspect(), 1.0);
        features.put("COL_E1_POS_TENSE:" + wplFeats1.get(POS_FEAT_STR) + "_"
                + e1.getTense(), 1.0);
        features.put("COL_E1_POS_ASPECT:" + wplFeats1.get(POS_FEAT_STR) + "_"
                + e1.getAspect(), 1.0);
        features.put(
                "COL_E1_POS_TENSE_ASPECT:" + wplFeats1.get(POS_FEAT_STR) + "_"
                        + e1.getTense() + "_"
                        + e1.getAspect(), 1.0);

        String e2Tense = "E2_TENSE:" + e2.getTense();
        features.put(e2Tense, 1.0);
        rightFeatures.add(e2Tense);
        String e2Aspect = "E2_ASPECT:" + e2.getAspect();
        features.put(e2Aspect, 1.0);
        rightFeatures.add(e2Aspect);
        String e2Class = "E2_CLASS:"+e2.getEventclass();
        features.put(e2Class,1.0);
        rightFeatures.add(e2Class);
        String e2Polarity = "E2_POLARITY:"+e2.getPolarity();
        features.put(e2Polarity,1.0);
        rightFeatures.add(e2Polarity);

        features.put("COL_E2_TENSE_ASPECT:" + e2.getTense()
                + "_" + e2.getAspect(), 1.0);
        features.put("COL_E2_POS_TENSE:" + wplFeats2.get(POS_FEAT_STR) + "_"
                + e2.getTense(), 1.0);
        features.put("COL_E2_POS_ASPECT:" + wplFeats2.get(POS_FEAT_STR) + "_"
                + e2.getAspect(), 1.0);
        features.put(
                "COL_E2_POS_TENSE_ASPECT:" + wplFeats2.get(POS_FEAT_STR) + "_"
                        + e2.getTense() + "_"
                        + e2.getAspect(), 1.0);

        features.put("COL_TENSE1_TENSE2:" + e1.getTense() + "_"
                + e2.getTense(), 1.0);
        features.put("COL_ASPECT1_ASPECT2:" + e1.getAspect()
                + "_" + e2.getAspect(), 1.0);
        features.put("COL_CLASS1_CLASS2:" + e1.getEventclass() + "_"
                + e2.getEventclass(), 1.0);
        features.put("COL_POLARITY1_POLARITY2:" + e1.getPolarity()
                + "_" + e2.getPolarity(), 1.0);
        features.put(
                "COL_TENSE1_ASPECT1_TENSE2_ASPECT2:"
                        + e1.getTense() + "_"
                        + e1.getAspect() + "_"
                        + e2.getTense() + "_"
                        + e2.getAspect(), 1.0);
        features.put(
                "COL_CLASS1_POLARITY1_CLASS2_POLARITY2:"
                        + e1.getEventclass() + "_"
                        + e1.getPolarity() + "_"
                        + e2.getEventclass() + "_"
                        + e2.getPolarity(), 1.0);
        /*------
		 E1_CONNECTIVE_E2: if E1 and E2 are in a short distance, is there any
		 connective between them
		 E2_CONNECTIVE_E1: if E1 and E2 are in a short sentence, is there any
		 connective between them
		 NOTE: Empirically, the best result happens with distance = 0 (the same sentence)
		------*/
        if (Math.abs(e1SentId - e2SentId) <= 0) {
            int from = Math.min(targetEvent1Off.getFirst(),
                    targetEvent2Off.getFirst());
            int to = Math.max(targetEvent1Off.getFirst(),
                    targetEvent2Off.getFirst());
            String[] tokens = ta.getTokensInSpan(from, to);
            boolean found_connective = false;
            for (String t : tokens) {
                String tLowerCase = t.toLowerCase();
                if (connectivesSet.contains(tLowerCase)) {
                    features.put("CONNECTIVE_BETWEEN:" + tLowerCase, 1.0);
                    found_connective = true;
                }
            }
            if(!found_connective)
                features.put("CONNECTIVE_BETWEEN:N/A", 1.0);
        }
        else{
            features.put("CONNECTIVE_BETWEEN:N/A", 1.0);
        }

        // =======================================
        // Get the Charniak parse tree.
        // Create a query that focuses on PP
        TreeView charniakView = (TreeView) ta.getView(ViewNames.PARSE_STANFORD);
        Predicate<Constituent> ppQuery = new Predicate<Constituent>() {
            private static final long serialVersionUID = -8421140892037175370L;

            @Override
            public Boolean transform(Constituent arg0) {
                return ParseTreeProperties.isNonTerminalPP(arg0.getLabel());
            }
        };

        /*------
		 E1_COVERED_BY_PP: if the target event1 is contained by a prepositional phrase
		 E2_COVERED_BY_PP: if the target event2 is contained by a prepositional phrase
		------*/
        Constituent targetEvent1Constituent = new Constituent("", "", ta,
                targetEvent1Off.getFirst(), targetEvent1Off.getSecond());
        Constituent targetEvent2Constituent = new Constituent("", "", ta,
                targetEvent2Off.getFirst(), targetEvent2Off.getSecond());

        Predicate<Constituent> e1query = ppQuery.and(Queries
                .containsConstituent(targetEvent1Constituent));
        IQueryable<Constituent> e1output = charniakView.where(e1query).orderBy(
                TextAnnotationUtilities.constituentLengthComparator);
        Iterator<Constituent> e1it = e1output.iterator();
        if (e1it.hasNext()) {
            features.put("E1_COVERED_BY_PP:" + "YES", 1.0);
            Constituent pp = e1it.next();
            int spp = pp.getStartSpan();
            String prep = ta.getToken(spp);
            features.put("E1_PP_HEAD:" + prep, 1.0);
        } else {
            features.put("E1_COVERED_BY_PP:" + "NO", 1.0);
            features.put("E1_PP_HEAD:" + "N/A", 1.0);
        }

        Predicate<Constituent> e2query = ppQuery.and(Queries
                .containsConstituent(targetEvent2Constituent));
        IQueryable<Constituent> e2output = charniakView.where(e2query).orderBy(
                TextAnnotationUtilities.constituentLengthComparator);
        Iterator<Constituent> e2it = e2output.iterator();
        if (e2it.hasNext()) {
            features.put("E2_COVERED_BY_PP:" + "YES", 1.0);
            Constituent pp = e2it.next();
            int spp = pp.getStartSpan();
            String prep = ta.getToken(spp);
            features.put("E2_PP_HEAD:" + prep, 1.0);
        } else {
            features.put("E2_COVERED_BY_PP:" + "NO", 1.0);
            features.put("E2_PP_HEAD:" + "N/A", 1.0);
        }
        // SYN_REL_LCS: if the target event and timex are in the same sentence,
        // this feat. encodes the least common subsumer (LCS) of the two.
        if (e1SentId == e2SentId) {
            Predicate<Constituent> query = Queries.containsConstituent(
                    targetEvent1Constituent).and(
                    Queries.containsConstituent(targetEvent2Constituent));
            IQueryable<Constituent> resultSet = charniakView
                    .where(query)
                    .orderBy(
                            TextAnnotationUtilities.constituentLengthComparator);
            Iterator<Constituent> rsIterator = resultSet.iterator();
            if (rsIterator.hasNext()) {
                Constituent pp = rsIterator.next();
                features.put("SYN_REL_LCS:" + pp.getLabel(), 1.0);
            }
            else
                features.put("SYN_REL_LCS:" + "NONE", 1.0);
        } else {
            features.put("SYN_REL_LCS:" + "NONE", 1.0);
        }

        /*-------
		// E1_SYNSET : bad/hurts
		// E2_SYNSET : bad/hurts
		// SAME_SYNSET: 2 event triggers are in the same synset?
		// SAME_DERIVATION: the derivation related forms of event 1 and 2 overlap with each other?
		-------*/
        List<String> e1Synsets = wnsim.getSynset(wplFeats1.get(LEMMA_FEAT_STR),
                wplFeats1.get(POS_FEAT_STR));
        List<String> e2Synsets = wnsim.getSynset(wplFeats2.get(LEMMA_FEAT_STR),
                wplFeats2.get(POS_FEAT_STR));
        Set<String> e1SetSynsets = new HashSet<String>(e1Synsets);
        Set<String> e2SetSynsets = new HashSet<String>(e2Synsets);
        e1SetSynsets.retainAll(e2SetSynsets);
        String sameSynset = "SAME_SYNSET:"+(e1SetSynsets.size() > 0 ? "YES" : "NO");
        features.put(sameSynset, 1.0);
        rightFeatures.add(sameSynset);

        List<String> e1Derivations = wnsim.getDerivations(
                wplFeats1.get(LEMMA_FEAT_STR), wplFeats1.get(POS_FEAT_STR));
        e1Derivations.add(wplFeats1.get(LEMMA_FEAT_STR));
        Set<String> e1SetDerivations = new HashSet<String>(e1Derivations);
        List<String> e2Derivations = wnsim.getDerivations(
                wplFeats2.get(LEMMA_FEAT_STR), wplFeats2.get(POS_FEAT_STR));
        e2Derivations.add(wplFeats2.get(LEMMA_FEAT_STR));
        Set<String> e2SetDerivations = new HashSet<String>(e2Derivations);
        e1SetDerivations.retainAll(e2SetDerivations);
        String sameDerivation = "SAME_DERIVATION:"+(e1SetDerivations.size() > 0 ? "YES" : "NO");
        features.put(sameDerivation, 1.0);
        rightFeatures.add(sameDerivation);

        /*---
		// Collocations
		---*/
        for (String l : leftFeatures) {
            for (String r : rightFeatures) {
                features.put(l + "&" + r, 1.0);
            }
        }

        return features;
    }
    public Map<String,Double> extractETfeats(EventChunk ec1, TemporalJointChunk t2) {
        Map<String, Double> features = new HashMap<String, Double>();
        boolean isDCT = t2.equals(doc.getDocumentCreationTime());
        TextAnnotation ta = doc.getTextAnnotation();
        EventStruct targetEvent = new EventStruct(ec1,ta);
        TimexStruct targetTimex;
        if(isDCT)
            targetTimex = new TimexStruct(t2);//for DCT, we cannot get wordoffset from TA.
        else
            targetTimex = new TimexStruct(t2,ta);
        List<TimexStruct> timexes = new ArrayList<>();
        timexes.add(new TimexStruct(doc.getDocumentCreationTime()));
        timexes.addAll(doc.getBodyTimexMentions().stream().map(t -> new TimexStruct(t, ta)).collect(Collectors.toList()));
        List<EventStruct> events = new ArrayList<>();
        for (EventChunk ec : doc.getBodyEventMentions())
            events.add(new EventStruct(ec,ta));
        // ==========================
        // Start extracting features
        // ==========================
        // Event first: the appearance order of event and timex (before, after)
        IntPair targetEventOff = targetEvent.getPrimaryTriggerWordOffset();
        IntPair targetTimexOff = targetTimex.getWordOffset();
        if ((targetEventOff.getFirst() < targetTimexOff.getFirst())) {
            features.put("E_FIRST:true", 1.0);
        }
        else
            features.put("E_FIRST:false", 1.0);
        // Same sentence: do the event and the timex appear in the same sentence
        int eSentId = ta.getSentenceId(targetEventOff.getFirst());
        int tSentId = -1;
        if(!isDCT)
            tSentId = ta.getSentenceId(targetTimexOff.getFirst());
        if (eSentId == tSentId) {
            features.put("SAME_SENT:true", 1.0);
        }
        else
            features.put("SAME_SENT:false", 1.0);
        // Closest distance: is the distance between the target event and the
        // target timex closest, or there is another timex that is closer to the
        // event?
        boolean closest = true;
        int minDist = Math.min(
                Math.abs(targetEventOff.getSecond()
                        - targetTimexOff.getFirst()),
                Math.abs(targetEventOff.getFirst()
                        - targetTimexOff.getSecond()));
        for (TimexStruct timex : timexes) {
            int dist = Math.min(
                    Math.abs(targetEventOff.getSecond()
                            - timex.getWordOffset().getFirst()),
                    Math.abs(targetEventOff.getFirst()
                            - timex.getWordOffset().getSecond()));
            if (dist < minDist) {
                closest = false;
                break;
            }
        }
        if (closest) {
            features.put("CLOSEST:true", 1.0);
        }
        else
            features.put("CLOSEST:false", 1.0);

        // NUM_SENT_DIFF: quantized number of sentences between the target timex
        // and
        // the target event
        int diffSent = Math.abs(tSentId - eSentId);
        if (diffSent == 0) {
            features.put("NUM_SENT_DIFF:NO", 1.0);
        } else if (diffSent == 1) {
            features.put("NUM_SENT_DIFF:ONE", 1.0);
        } else if (diffSent == 2) {
            features.put("NUM_SENT_DIFF:TWO", 1.0);
        } else if (diffSent < 5) {
            features.put("NUM_SENT_DIFF:SOME", 1.0);
        } else {
            features.put("NUM_SENT_DIFF:ALOT", 1.0);
        }
        /*------
         E_WORD
		 E_POS
		 E_LEMMA
		-------*/
        TokenLabelView posView = (TokenLabelView) ta.getView(ViewNames.POS);
        TokenLabelView lemmaView = (TokenLabelView) ta.getView(ViewNames.LEMMA);
        Map<String, String> wplFeats = extractWordPosLemmaFeats(posView,
                lemmaView, targetEventOff);
        features.put("E_WORD:" + wplFeats.get(WORD_FEAT_STR), 1.0);
        features.put("E_POS:" + wplFeats.get(POS_FEAT_STR), 1.0);
        features.put("E_LEMMA:" + wplFeats.get(LEMMA_FEAT_STR), 1.0);
        //Event properties
        features.put("E_TENSE:" + ec1.getTense(), 1.0);
        features.put("E_ASPECT:" + ec1.getAspect(), 1.0);
        features.put("E_CLASS:"+ec1.getEventclass(),1.0);
        features.put("E_POLARITY:"+ec1.getPolarity(),1.0);
        //Timex properties
        features.put("T_TYPE:"+t2.getResult().getType(),1.0);
        features.put("T_MOD:"+t2.getResult().getMod(),1.0);
        features.put("IS_DCT:"+isDCT,1.0);

        //Collocation
        features.put("COL_E_TENSE_ASPECT:" + ec1.getTense()
                + "_" + ec1.getAspect(), 1.0);
        features.put("COL_E_POS_TENSE:" + wplFeats.get(POS_FEAT_STR) + "_"
                + ec1.getTense(), 1.0);
        features.put("COL_E_POS_ASPECT:" + wplFeats.get(POS_FEAT_STR) + "_"
                + ec1.getAspect(), 1.0);
        features.put(
                "COL_E_POS_TENSE_ASPECT:" + wplFeats.get(POS_FEAT_STR) + "_"
                        + ec1.getTense() + "_"
                        + ec1.getAspect(), 1.0);
        features.put("COL_T_TYPE_MOD:"+t2.getResult().getType()+t2.getResult().getMod(),1.0);
        features.put("COL_T_MOD_ISDCT:"+t2.getResult().getMod()+isDCT,1.0);
        features.put("COL_T_TYPE_ISDCT:"+t2.getResult().getType()+isDCT,1.0);
        features.put("COL_T_TYPE_MOD_ISDCT:"+t2.getResult().getType()+t2.getResult().getMod()+isDCT,1.0);

        // =======================================
        // Get the Charniak parse tree.
        // Create a query that focuses on PP
        TreeView charniakView = (TreeView) ta.getView(ViewNames.PARSE_STANFORD);
        Predicate<Constituent> ppQuery = new Predicate<Constituent>() {
            private static final long serialVersionUID = -8421140892037175370L;

            @Override
            public Boolean transform(Constituent arg0) {
                return ParseTreeProperties.isNonTerminalPP(arg0.getLabel());
            }
        };
        /*------
		 E_COVERED_BY_PP: if the target event is contained by a prepositional phrase
		------*/
        Constituent targetEventConstituent = new Constituent("", "", ta,
                targetEventOff.getFirst(), targetEventOff.getSecond());
        Predicate<Constituent> equery = ppQuery.and(Queries
                .containsConstituent(targetEventConstituent));
        IQueryable<Constituent> eoutput = charniakView.where(equery).orderBy(
                TextAnnotationUtilities.constituentLengthComparator);
        Iterator<Constituent> eit = eoutput.iterator();
        if (eit.hasNext()) {
            features.put("E_COVERED_BY_PP:true", 1.0);
            Constituent pp = eit.next();
            int spp = pp.getStartSpan();
            String prep = ta.getToken(spp);
            features.put("E_PP_HEAD:" + prep, 1.0);
        }
        else{
            features.put("E_COVERED_BY_PP:false", 1.0);
            features.put("E_PP_HEAD:"+ "N/A", 1.0);
        }

		/*------
		 T_COVERED_BY_PP: if the target timex is contained by a prepositional phrase
		 T_PP_HEAD: the preposition head of the prepositional phrase
		------*/
        Constituent targetTimexConstituent = new Constituent("", "", ta,
                targetTimexOff.getFirst(), targetTimexOff.getSecond());
        Predicate<Constituent> tquery = ppQuery.and(Queries
                .containsConstituent(targetTimexConstituent));
        IQueryable<Constituent> toutput = charniakView
                .where(tquery)
                .orderBy(
                        TextAnnotationUtilities.constituentLengthComparator);
        Iterator<Constituent> tit = toutput.iterator();
        if (tit.hasNext()) {
            features.put("T_COVERED_BY_PP:true", 1.0);
            Constituent pp = tit.next();
            int spp = pp.getStartSpan();
            String prep = ta.getToken(spp);
            features.put("T_PP_HEAD:" + prep, 1.0);
        }
        else{
            features.put("T_COVERED_BY_PP:false", 1.0);
            features.put("T_PP_HEAD:" + "N/A", 1.0);
        }
        /*-----------
		// SAME_SYNSET_LEFT_EVENT
		// SAME_SYNSET_RIGHT_EVENT
		------------*/
        int closestLeft = Integer.MIN_VALUE;
        int closestRight = Integer.MAX_VALUE;
        for (int j = 0; j < events.size(); j++) {
            EventStruct eStruct = events.get(j);
            IntPair offset = eStruct.getPrimaryTriggerWordOffset();
            if (offset.getSecond() < targetTimexOff.getFirst()) {
                if (closestLeft < j) {
                    closestLeft = j;
                }
            }
            if (offset.getFirst() > targetTimexOff.getSecond()) {
                if (closestRight > j) {
                    closestRight = j;
                }
            }
        }

        List<String> eSynsets = wnsim.getSynset(
                wplFeats.get(LEMMA_FEAT_STR), wplFeats.get(POS_FEAT_STR));
        Set<String> eSetSynsets = new HashSet<String>(eSynsets);

        if (closestLeft != Integer.MIN_VALUE) {
            EventStruct eStructLeft = events.get(closestLeft);
            Map<String, String> wplFeatsLeft = extractWordPosLemmaFeats(
                    posView, lemmaView,
                    eStructLeft.getPrimaryTriggerWordOffset());
            List<String> eLeftSynsets = wnsim.getSynset(
                    wplFeatsLeft.get(LEMMA_FEAT_STR),
                    wplFeatsLeft.get(POS_FEAT_STR));
            Set<String> eSetLeftSynsets = new HashSet<String>(eLeftSynsets);
            eSetLeftSynsets.retainAll(eSetSynsets);
            if (eSetLeftSynsets.size() > 0) {
                features.put("SAME_SYNSET_LEFT_EVENT:true", 1.0);
            }
            else
                features.put("SAME_SYNSET_LEFT_EVENT:false", 1.0);
        }
        if (closestRight != Integer.MAX_VALUE) {
            EventStruct eStructRight = events.get(closestRight);
            Map<String, String> wplFeatsRight = extractWordPosLemmaFeats(
                    posView, lemmaView,
                    eStructRight.getPrimaryTriggerWordOffset());
            List<String> eRightSynsets = wnsim.getSynset(
                    wplFeatsRight.get(LEMMA_FEAT_STR),
                    wplFeatsRight.get(POS_FEAT_STR));
            Set<String> eSetRightSynsets = new HashSet<String>(
                    eRightSynsets);
            eSetRightSynsets.retainAll(eSetSynsets);
            if (eSetRightSynsets.size() > 0) {
                features.put("SAME_SYNSET_RIGHT_EVENT:true", 1.0);
            }
            else
                features.put("SAME_SYNSET_RIGHT_EVENT:true", 1.0);
        }
        return features;
    }
    public static void main(String[] args) throws Exception{
        TempEval3Reader myReader;
        myReader = new TempEval3Reader("TIMEML","TimeBank","data/TempEval3/Training/TBAQ-cleaned/");
        myReader.ReadData();
        myReader.createTextAnnotation();
        TemporalDataset dataset = myReader.getDataset();
        TemporalDocument doc = dataset.getDocuments().get(0);
        FeatureExtractor featureExtractor = new FeatureExtractor(doc);
        //featureExtractor.extractEEfeats(doc.getBodyEventMentions().get(0),doc.getBodyEventMentions().get(1));
        featureExtractor.extractEEfeats_doc();
        featureExtractor.extractEEfeats_Vague_doc(0.1);
        System.out.println();
    }

}
