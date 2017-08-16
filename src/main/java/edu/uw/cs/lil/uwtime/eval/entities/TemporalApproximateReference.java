package edu.uw.cs.lil.uwtime.eval.entities;

public class TemporalApproximateReference extends TemporalEntity {
	private String label;
	public TemporalApproximateReference(String label) {
		this.label = label;
	}

	@Override
	public String getValue() {
		return label;
	}
	
	@Override
	public String toString() {
		return label;
	}

	@Override
	public String getType() {
		return "DATE";
	}

	@Override
	public String getMod() {
		return null;
	}

	@Override
	public String getStructure() {
		return getValue();
	}	
}
