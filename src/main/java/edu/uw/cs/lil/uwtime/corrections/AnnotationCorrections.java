package edu.uw.cs.lil.uwtime.corrections;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.utils.composites.Pair;

public class AnnotationCorrections {
	final private Map<String, Map<Integer, MentionCorrection>> corrections;
	final private Map<String, List<MentionCorrection>> additions;

	public AnnotationCorrections() {
		corrections = new HashMap<String, Map<Integer, MentionCorrection>>();
		additions = new HashMap<String, List<MentionCorrection>>();
	}

	public AnnotationCorrections(String correctionsFile) throws IOException {
		this();
		BufferedReader br;
		br = new BufferedReader(new FileReader(correctionsFile));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.trim().length() > 0 && !line.startsWith("#")) {
				String[] fields = line.split(",");
				String docID = fields[0];
				int tid = fields[1].endsWith("x") ? -1 : Integer.parseInt(fields[1].replaceFirst("t", ""));
				String value = fields[2];
				String type = fields[3];
				String mod = fields[4];
				int startChar = fields.length > 5 ? Integer.parseInt(fields[5]) : -1;
				int endChar = fields.length > 5 ? Integer.parseInt(fields[6]) : -1;

				if (tid == -1) {
					if (!additions.containsKey(docID))
						additions.put(docID, new LinkedList<MentionCorrection>());
					additions.get(docID).add(new MentionCorrection(value, type, mod, startChar, endChar));
				}
				else {
					if (!corrections.containsKey(docID))
						corrections.put(docID, new HashMap<Integer, MentionCorrection>());
					corrections.get(docID).put(tid, new MentionCorrection(value, type, mod, startChar, endChar));
				}
			}
		}
		br.close();
	}

	public MentionCorrection getCorrection(String docid, int tid) {
		Map<Integer, MentionCorrection> documentMap = corrections.get(docid);
		return documentMap == null ? null : documentMap.get(tid);
	}

	public String toString() {
		return corrections + "\n" +
				additions;
	}


	public void applyCorrections(TemporalDataset dataset) {
		for (TemporalDocument document : dataset.getDocuments()) {
			int maxID = -1;
			for (TemporalSentence sentence : document) {
				maxID = Math.max(maxID, sentence.applyCorrections(this));
			}
			if (additions.containsKey(document.getDocID())) {
				for (MentionCorrection addition : additions.get(document.getDocID())) {
					Pair<TemporalSentence, Integer> start = document.getTokenFromStartChar(addition.getStartChar());
					Pair<TemporalSentence, Integer> end = document.getTokenFromEndChar(addition.getEndChar());
					if (start == null)
						System.err.println("Bad start character offset: " + addition);
					if (end == null)
						System.err.println("Bad end character offset: " + addition);

					if (start.first() != end.first())
						System.err.println("Invalid addition spanning across sentences: " + addition);
					maxID++;
					start.first().addCorrectedMention(addition, maxID, start.second(), end.second());
				}
			}
		}
	}
}
