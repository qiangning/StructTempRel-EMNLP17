package edu.uw.cs.lil.uwtime.learn.temporal;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.corrections.MentionCorrection;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalEntity;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;
import edu.uw.cs.lil.uwtime.utils.FormattingUtils;

public class MentionResult implements java.io.Serializable{
	private static final long serialVersionUID = -5859852309843402300L;
	private static String INDENTATION = "\t\t";
	private final IDerivation<LogicalExpression> derivation;
	private final TemporalContext context;
	private final TemporalExecutionHistory executionHistory;
	private final TemporalEntity entity;
	private final String type, value, mod;
	private final TemporalJointChunk chunk;

	// Main constructor
	public MentionResult(TemporalJointChunk chunk, 
			String type, 
			String value, 
			String mod, 
			IDerivation<LogicalExpression> derivation, 
			TemporalContext context, 
			TemporalExecutionHistory executionHistory, 
			TemporalEntity entity){
		this.chunk = chunk;
		this.derivation = derivation;
		this.context = context;
		this.executionHistory = executionHistory;
		this.entity = entity;

		this.value = value;
		this.type = type;
		this.mod = mod;
	}

	// For gold results with latent variables
	public MentionResult(TemporalJointChunk chunk, String type, String value, String mod) {
		this(chunk, type, value, mod, null, new TemporalContext(), new TemporalExecutionHistory(), null);
	}

	// Temporary and incomplete results for detection
	public MentionResult(TemporalJointChunk chunk, TemporalEntity entity) {
		this(chunk, null, null, null, null, new TemporalContext(), new TemporalExecutionHistory(), entity);
	}

	// For annotation corrections
	public MentionResult(TemporalJointChunk chunk, MentionCorrection mentionCorrection) {
		this(chunk, mentionCorrection.getType(), mentionCorrection.getValue(), mentionCorrection.getMod());
	}

	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public String getMod() {
		return mod;
	}

	public TemporalJointChunk getChunk() {
		return chunk;
	}

	public LogicalExpression getSemantics() {
		return derivation == null ? null : derivation.getSemantics();
	}

	public IDerivation<LogicalExpression> getDerivation() {
		return derivation;
	}

	public TemporalContext getContext() {
		return context;
	}

	public TemporalExecutionHistory getExecutionHistory() {
		return executionHistory;
	}

	public TemporalEntity getEntity() {
		return entity;
	}

	public String toString() {
		String s = "\n";
		s += FormattingUtils.formatContents(INDENTATION, "Value", value);
		s += FormattingUtils.formatContents(INDENTATION, "Type", type);
		if (mod != null)
			s += FormattingUtils.formatContents(INDENTATION, "Mod", mod);
		if (getSemantics() != null)
			s += FormattingUtils.formatContents(INDENTATION, "Semantics", getSemantics());
		if (context != null)
			s += FormattingUtils.formatContents(INDENTATION, "Context", context);
		if (entity != null)
			s += FormattingUtils.formatContents(INDENTATION, "Entity", entity);
		if (derivation != null)
			s += FormattingUtils.formatContents(INDENTATION, "Lexical entries", derivation.getMaxLexicalEntries());
		return s;
	}

	public String beginAnnotation(String tid) {
		return String.format("<TIMEX3 %s %s %s%s>", 
				"tid=\"" + tid + "\"", 
				"type=\"" + (type == null ? "DATE" : type ) + "\"", 
						"value=\"" + value + "\"", 
						mod == null ? "" : " mod=\"" + mod + "\"");
	}

	public String endAnnotation() {
		return "</TIMEX3>";
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MentionResult))
			return false;
		return this.getValue().equals(((MentionResult) other).getValue());
	}

	@Override
	public int hashCode() {
		return this.getValue().hashCode();
	}
}