/**
 * 
 */
package edu.illinois.cs.cogcomp.nlp.util.wordnet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dxquang Apr 14, 2010
 */
public class SynsetNode {

	public String nodeId;
	public List<String> words = null;
	public List<String> hyperIds = null;
	public List<String> entailIds = null;
	public List<String> antonIds = null;
	public List<String> derivationIds = null;

	/**
	 * 
	 */
	public SynsetNode() {
		nodeId = "NULL";
		words = new ArrayList<String>();
		hyperIds = new ArrayList<String>();
		entailIds = new ArrayList<String>();
		antonIds = new ArrayList<String>();
		derivationIds = new ArrayList<String>();
	}

}
