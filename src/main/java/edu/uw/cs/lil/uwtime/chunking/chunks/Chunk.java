package edu.uw.cs.lil.uwtime.chunking.chunks;

import java.io.Serializable;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.uwtime.utils.FormattingUtils;
import edu.uw.cs.utils.composites.Pair;

public class Chunk<MR> extends AbstractChunk<MR> implements Serializable{
	private static final long serialVersionUID = 2210841885856062351L;
	private static final String INDENTATION = "\t\t";
	private final int start, end;
	private final IDerivation<MR> derivation;
	
	public Chunk(int start, int end, IDerivation<MR> derivation) {
		this.start = start;
		this.end = end;
		this.derivation = derivation;
	}
	
	@Override
	public int getStart() {
		return start;
	}

	@Override
	public int getEnd() {
		return end;
	}

	@Override
	public IDerivation<MR> getDerivation() {
		return derivation;
	}
	
	@Override
	public boolean equals(Object other) {
		return (other instanceof Chunk<?>) && this.start == ((Chunk<?>) other).start && this.end == ((Chunk<?>) other).end;
	}
	
	@Override
	public int hashCode() {
		return Pair.of(start, end).hashCode();
	}
	
	@Override
	public String toString() {
		String s = "\n";
		s += FormattingUtils.formatContents(INDENTATION, "Derivations", derivation);
		return s;
	}
}
