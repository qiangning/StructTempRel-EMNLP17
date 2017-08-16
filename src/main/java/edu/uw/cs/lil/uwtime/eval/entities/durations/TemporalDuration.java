package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalEntity;

public abstract class TemporalDuration extends TemporalEntity{
	protected TemporalDuration parent, child;
	private int n;
	protected TemporalQuantifier quantifier;
	protected TemporalModifier modifier;

	public TemporalDuration(int n) {
		this(n, null);
	}

	public TemporalDuration(int n, TemporalDuration child) {
		this(n, child, TemporalQuantifier.CARDINAL, TemporalModifier.NONE);
	}

	public TemporalDuration(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		this.n = n;
		if (child != null) {
			this.child = child;
			child.parent = this;
		}
		this.quantifier = quantifier;
		this.modifier = modifier;
	}

	public abstract String getName();
	public abstract String getLocalSequenceValue(); 
	public abstract TemporalDuration getDefaultParent();
	public abstract int getGranularity();
	public abstract TemporalDuration clone();
	public abstract int getMaximumN();
	public abstract LocalDateTime getStartJodaTime(LocalDateTime jodaDate);
	public abstract Period getJodaUnitPeriod();
	public abstract int atSameGranularity(LocalDateTime jodaTime);
	public abstract String getDurationValue();

	@Override
	public String getValue() {
		if (quantifier == TemporalQuantifier.SOME)
			setN(0);
		return getDurationValue();
	}

	public String getSequenceValue() {
		String localValue = getLocalSequenceValue();
		if (localValue == null)
			return null;
		else if (child == null)
			return localValue;
		else {
			String childValue = child.getSequenceValue();
			if (childValue == null)
				return null;
			else
				return localValue + childValue;
		}
	}

	public Period getJodaFullPeriod() {
		return getJodaUnitPeriod().multipliedBy(n);
	}

	public void increment() {
		setN(n + 1);
	}

	public void decrement() {
		// Skip 0
		if (n - 1 == 0)
			setN( -1 );
		else
			setN(n - 1);
	}

	public boolean canDecrement() {
		return n - 1 > 0;
	}

	public boolean canIncrement() {
		return n + 1 <= getMaximumN();
	}

	public TemporalDuration getParent() {
		return parent;
	}

	public TemporalDuration getChild() {
		return child;
	}

	public String toString() {
		return getName() + "(" + n + ")" + (child == null ? "" : "-->" + child);
	}

	public boolean isFixed() {
		return n > 0;
	}

	public boolean isFullyFixed() {
		return isFixed() && (child == null || child.isFullyFixed());
	}

	public boolean intersect(TemporalDuration other) {
		if(other == null)
			return true;
		else if(other.getGranularity() > this.getGranularity()) {
			if (this.child == null) {
				this.setChild(other);
				return true;
			}
			else {
				return this.child.intersect(other);
			}
		}
		else if (other.getGranularity() == this.getGranularity()) {
			if (this.isFixed() && other.isFixed()) {
				if(this.n == other.n) {
					if(this.child != null)
						return this.child.intersect(other.child);
					else				
						return true;
				}
				else
					return false; // Empty intersection
			}
			else if (this.isFixed() && !other.isFixed()) {
				this.child = other.child;
				return true;
			}
			else if (!this.isFixed() && !other.isFixed()) {
				if (this.child != null)
					return this.child.intersect(other.child);
				else
					return true;
			}
			else {
				this.n = other.n;
				return true;
			}
		}
		else {
			return false;
		}
	}

	public int getN() {
		return n;
	}

	public void setN(int n){
		this.n = n;
	}

	public TemporalDuration getHead() {
		if (parent != null)
			return parent.getHead();
		else
			return this;
	}

	public void setChild(TemporalDuration child) {
		this.child = child;
		if (child != null)
			child.parent = this;
	}

	public TemporalDuration getDeepestFreeVariable() {
		if (child == null)
			return isFixed() ? null : this;
		else {
			TemporalDuration deepest = child.getDeepestFreeVariable();
			if (deepest != null)
				return deepest;
			else
				return isFixed() ? null : this;
		}
	}

	public void fixCommonAncestors(TemporalDuration other) {
		if (other != null && this.getGranularity() == other.getGranularity() && !this.isFixed()) {
			this.n = other.n;
			if(child != null && other != null)
				child.fixCommonAncestors(other.child);
		}
	}

	public boolean followsPath(String[] path, int i) {
		if (!path[i].equals(getName())) // This node does not match
			return false;
		else if (i >= path.length - 1) // All names in path were consumed
			return true;
		else if (child == null) // No more children left to consume remaining names in path
			return false;
		else
			return child.followsPath(path, i + 1); // This node is fine, but the children can fail
	}

	public TemporalDuration getDeepestNode() {
		return child == null ? this : child.getDeepestNode();
	}

	@Override
	public String getType() {
		return "DURATION";
	}

	public TemporalDuration getNodeWithGranularity(TemporalDuration duration) {
		if (this.getGranularity() == duration.getGranularity())
			return this;
		else
			return child == null ? null : child.getNodeWithGranularity(duration);
	}

	public TemporalDuration emptyClone() {
		TemporalDuration emptyClone = clone();
		emptyClone.child = null;
		emptyClone.parent = null;
		emptyClone.n = 0;
		return emptyClone;
	}

	public void pruneGranularityTo(int granularity) {
		if (getGranularity() >= granularity) {
			child = null;
		}
		else if (child != null) {
			child.pruneGranularityTo(granularity);
		}
	}

	public void setQuantifier(TemporalQuantifier quantifier) {
		this.quantifier = quantifier;
	}

	public boolean hasQuantifier(TemporalQuantifier quantifier) {
		return this.quantifier == quantifier;
	}

	public TemporalDuration withQuantifier(TemporalQuantifier quantifier) {
		TemporalDuration newNode = this.clone();
		newNode.setQuantifier(quantifier);
		return newNode;
	}

	public void setModifier(TemporalModifier modifier) {
		this.modifier = modifier;
	}

	public boolean hasModifier(TemporalModifier modifier) {
		return this.modifier == modifier;
	}

	public TemporalDuration withModifier(TemporalModifier modifier) {
		TemporalDuration newNode = this.clone();
		newNode.setModifier(modifier);
		return newNode;
	}

	public TemporalDuration getStructuralCopy() {
		TemporalDuration copy = clone();
		if (copy.isFixed())
			copy.setN(1);
		return copy;
	}

	@Override
	public String getMod() {
		return modifier.toString();
	}

	@Override
	public String getStructure() {
		return getStructuralCopy().toString();
	}
}
