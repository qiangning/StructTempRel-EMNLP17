package edu.uw.cs.lil.uwtime.chunking.chunks;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.nlp.util.mySimpleDate;
import edu.stanford.nlp.dcoref.Mention;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.uwtime.corrections.MentionCorrection;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.utils.FormattingUtils;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;
import edu.uw.cs.utils.composites.Pair;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TemporalJointChunk extends AbstractJointChunk<LogicalExpression, MentionResult> implements ILabeledDataItem<ISituatedDataItem<Sentence, TemporalJointChunk>, String>, java.io.Serializable {
	private static final long serialVersionUID = -5859852309847402300L;
	private static String INDENTATION = "\t";

	private int tid;
	private TemporalSentence sentence;
	private IChunk<LogicalExpression> baseChunk;
	private Sentence phrase;
	private MentionResult result;
	private ISituatedDataItem<Sentence, TemporalJointChunk> sample;

	// Temporary variables used only during preprocessing
	private int charStart; //character offset
	private int charEnd;

	// For predicted chunks
	public TemporalJointChunk(TemporalSentence sentence, IChunk<LogicalExpression> baseChunk, MentionResult result) {
		this.sentence = sentence;
		this.result = result;
		this.baseChunk = baseChunk;
		this.tid = -1; //no tid assigned to predicted chunks
	}

	// For gold chunks
	public TemporalJointChunk(String type, String value, String mod, int charStart, int charEnd, int tid) {
		this.result = new MentionResult(this, type, value, mod);
		this.charStart = charStart;
		this.charEnd = charEnd;
		this.tid = tid;
	}

	public TemporalJointChunk(TemporalJointChunk other) {
		this(other, null);
	}

	public TemporalJointChunk(TemporalJointChunk other, MentionCorrection mentionCorrection) {
		this.tid = other.tid;
		this.sentence = other.sentence;
		if (mentionCorrection != null && mentionCorrection.useSpan()) {
			Pair<TemporalSentence, Integer> start = sentence.getDocument().getTokenFromStartChar(mentionCorrection.getStartChar());
			Pair<TemporalSentence, Integer> end =  sentence.getDocument().getTokenFromEndChar(mentionCorrection.getEndChar());
			if (start == null)
				System.err.println("No start token for: " + mentionCorrection);
			if (end == null)
				System.err.println("No end token for: " + mentionCorrection);
			if (start.first() != sentence || end.first() != sentence)
				System.err.println("Invalid correction (bad tid): " + mentionCorrection);

			this.baseChunk = new Chunk<LogicalExpression>(start.second(), end.second(), null);
		}
		else {
			this.baseChunk = other.baseChunk;
		}
		if (mentionCorrection != null)
			this.result = new MentionResult(this, mentionCorrection);
		else
			this.result = other.result;
	}

	public TemporalJointChunk(TemporalSentence sentence, int tid, int start, int end, MentionCorrection correction) {
		this.tid = tid;
		this.sentence = sentence;
		this.baseChunk = new Chunk<LogicalExpression>(start, end, null);
		this.result = new MentionResult(this, correction);
	}

	private void setTokenRange(int start, int end) {
		baseChunk = new Chunk<LogicalExpression>(start, end, null);
	}

	public void setCharEnd(String text) {
		charEnd = charStart + text.length();
	}

	public TemporalSentence getSentence() {
		return sentence;
	}

	public Sentence getPhrase() {
		if (phrase == null) {
			List<String> tokenList = new LinkedList<String>();
			if (getEnd() < getStart()) {
				TemporalLog.printf("error", "Multi-sentential phrase (%d->%d). Using end of sentence (%s)\n",getStart(),getEnd(),sentence.toString());
				setTokenRange(getStart(), sentence.getNumTokens() - 1);
			}
			for (String s : sentence.getTokens().subList(getStart(), getEnd() + 1))
				tokenList.add(s.toLowerCase());
			phrase = new Sentence(tokenList);
		}
		return phrase;
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
			this.setTokenRange(startIndexes.second(), endIndexes.second());
			// Assume start and end occur in the same sentence
			TemporalSentence alignedSentence = startIndexes.first();
			sentence = alignedSentence;
			alignedSentence.insertGoldChunk(this);
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
				TemporalLog.printf("error", "Unable to find offset for mention [#%d -> #%d](%s)\n", charStart, charEnd, result.getValue());
			}
		}
	}

	public int getCharStart() {
		return sentence.getDocument().getCharRange(sentence, getStart()).first();
	}

	public int getCharEnd() {
		return sentence.getDocument().getCharRange(sentence, getEnd()).second();
	}

	public String getOriginalText() {
		if (this == sentence.getDocument().getDocumentCreationTime())
			return sentence.getDocument().getDocumentCreationText();
		else
			return sentence.getDocument().getOriginalText(getCharStart(), getCharEnd());
	}

	@Override
	public int getStart() {
		return baseChunk.getStart();
	}

	@Override
	public int getEnd() {
		return baseChunk.getEnd();
	}

	@Override
	public MentionResult getResult() {
		return result;
	}

	@Override
	public IDerivation<LogicalExpression> getDerivation() {
		return baseChunk.getDerivation();
	}

	@Override
	public IChunk<LogicalExpression> getBaseChunk() {
		return baseChunk;
	}

	@Override
	public ISituatedDataItem<Sentence, TemporalJointChunk> getSample() {
		if (sample == null) {
			sample = new ISituatedDataItem<Sentence, TemporalJointChunk> () {
				private static final long serialVersionUID = 1L;
				@Override
				public Sentence getSample() {
					return getPhrase();
				}

				@Override
				public TemporalJointChunk getState() {
					return TemporalJointChunk.this;
				}
			};
		}
		return sample;
	}

	public void setResult(MentionResult result) {
		this.result = result;
	}

	@Override
	public String toString() {
		String s = "\n";
		s += FormattingUtils.formatContents(INDENTATION, "Phrase", getPhrase());
		if (tid != -1)
			s += FormattingUtils.formatContents(INDENTATION, "TID", "t" + tid);
		if (result != null) {
			if (getDerivation() != null)
				s += FormattingUtils.formatContents(INDENTATION, "Base derivation", getDerivation());
			s += FormattingUtils.formatContents(INDENTATION, "Result", result);
		}
		return s;
	}

	public String toStringConcise(){
		String s = "t"+tid+"\t\t"
				+result.getType()+"\t"
				+result.getValue()+"\t\t"
				+result.getMod()+"\t"
				+"\""+getPhrase().toString()+"\"";
		return s;
	}
	public TlinkType compareResult(MentionResult otherresult, MentionResult dctresult){
		if(!result.getType().equals("DATE") || !otherresult.getType().equals("DATE"))
			return TlinkType.UNDEF;
		String timex1 = result.getValue();
		String timex2 = otherresult.getValue();
		/*If both are PRESENT_REF/PAST_REF/FUTURE_REF*/
		if((timex1.equals("PRESENT_REF")||timex1.equals("PAST_REF")||timex1.equals("FUTURE_REF"))
				&&(timex2.equals("PRESENT_REF")||timex2.equals("PAST_REF")||timex2.equals("FUTURE_REF"))){
			switch(timex1){
				case "PRESENT_REF":
					switch(timex2){
						case "PAST_REF":
							return TlinkType.AFTER;
						case "FUTURE_REF":
							return TlinkType.BEFORE;
						case "PRESENT_REF":
							return TlinkType.EQUAL;
						default:
							return TlinkType.UNDEF;
					}
				case "PAST_REF":
					switch(timex2){
						case "PAST_REF":
							return TlinkType.UNDEF;
						case "FUTURE_REF":
							return TlinkType.BEFORE;
						case "PRESENT_REF":
							return TlinkType.BEFORE;
						default:
							return TlinkType.UNDEF;
					}
				case "FUTURE_REF":
					switch(timex2){
						case "PAST_REF":
							return TlinkType.AFTER;
						case "FUTURE_REF":
							return TlinkType.UNDEF;
						case "PRESENT_REF":
							return TlinkType.AFTER;
						default:
							return TlinkType.UNDEF;
					}
				default:
					return TlinkType.UNDEF;
			}
		}
		/*If either one is PRESENT_REF/PAST_REF/FUTURE_REF*/
		String dct = dctresult.getValue();
		if(timex1.equals("PRESENT_REF")||timex1.equals("PAST_REF")||timex1.equals("FUTURE_REF")) {
			TlinkType comp = mySimpleDate.compareString(dct,timex2);
			switch(timex1){
				case "PRESENT_REF":
					return comp;
				case "PAST_REF"://timex1 before dct
					if(comp==TlinkType.BEFORE||comp==TlinkType.EQUAL||comp==TlinkType.INCLUDES)//dct before/equal to/includes timex2
						return TlinkType.BEFORE;
					else
						return TlinkType.UNDEF;
				case "FUTURE_REF"://timex1 after dct
					if(comp==TlinkType.AFTER||comp==TlinkType.EQUAL||comp==TlinkType.INCLUDES)//dct after/equal to/includes timex2
						return TlinkType.AFTER;
					else
						return TlinkType.UNDEF;
				default:
					return TlinkType.UNDEF;
			}
		}
		if(timex2.equals("PRESENT_REF")||timex2.equals("PAST_REF")||timex2.equals("FUTURE_REF")){
			TlinkType comp = mySimpleDate.compareString(timex1,dct);
			switch(timex2){
				case "PRESENT_REF"://timex2 is dct
					return comp;
				case "PAST_REF"://timex2 before dct
					if(comp==TlinkType.AFTER||comp==TlinkType.EQUAL||comp==TlinkType.INCLUDES)//timex1 after/equal to/includes dct
						return TlinkType.AFTER;
					else
						return TlinkType.UNDEF;
				case "FUTURE_REF"://timex2 after dct
					if(comp==TlinkType.BEFORE||comp==TlinkType.EQUAL||comp==TlinkType.INCLUDES)//timex1 before/equal to/includes dct
						return TlinkType.BEFORE;
					else
						return TlinkType.UNDEF;
				default:
					return TlinkType.UNDEF;
			}
		}
		/*both are standard dates*/
		return mySimpleDate.compareString(timex1,timex2);
	}


	public int spanSize() {
		return getEnd() - getStart();
	}

	@Override
	public double calculateLoss(String label) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean prune(String y) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double quality() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLabel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCorrect(String label) {
		throw new UnsupportedOperationException();
	}

	public int getTID() {
		return tid;
	}

	public boolean isStrictMatch() {
		for (TemporalJointChunk chunk : sentence.getLabel())
			if (this.strictlyMatches(chunk)) 
				return true;
		return false;
	}

	public boolean isRelaxedMatch() {
		for (TemporalJointChunk chunk : sentence.getLabel())
			if (this.overlapsWith(chunk)) 
				return true;
		return false;
	}

	public boolean isSubmentionMatch() {
		for (TemporalJointChunk chunk : sentence.getLabel())
			if (chunk.contains(this)) 
				return true;
		return false;
	}

	public boolean isStrictSubmentionMatch() {
		for (TemporalJointChunk chunk : sentence.getLabel())
			if (chunk.contains(this) && !chunk.strictlyMatches(this)) 
				return true;
		return false;
	}

	public void setTID(int tid) {
		this.tid = tid;
	}

	public void setSentence(TemporalSentence sentence) {
		this.sentence = sentence;
	}

	public void setBaseChunk(IChunk<LogicalExpression> baseChunk) {
		this.baseChunk = baseChunk;
	}

	public void setPhrase(Sentence phrase) {
		this.phrase = phrase;
	}

	public void setSample(ISituatedDataItem<Sentence, TemporalJointChunk> sample) {
		this.sample = sample;
	}
	public TemporalJointChunk deepCopy(){
		TemporalJointChunk new_tjc = new TemporalJointChunk(
				this.getResult().getType(),
				this.getResult().getValue(),
				this.getResult().getMod(),
				charStart,
				charEnd,
				this.getTID());
		new_tjc.setSentence(this.getSentence());
		new_tjc.setBaseChunk(this.getBaseChunk());
		new_tjc.setPhrase(this.getPhrase());
		new_tjc.setSample(this.getSample());
		return new_tjc;
	}
}