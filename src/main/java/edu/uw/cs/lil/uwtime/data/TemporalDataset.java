package edu.uw.cs.lil.uwtime.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;

public class TemporalDataset implements IDataCollection<TemporalSentence>, java.io.Serializable{
	private String name;
	private List<TemporalSentence> sentences;
	private List<TemporalDocument> documents; // Should refer to the same sentences.

	private static final long serialVersionUID = -9138434230690313768L;

	public TemporalDataset(String name) {
		this.name = name;
		documents = new LinkedList<TemporalDocument>();
		sentences = new LinkedList<TemporalSentence>();
	}

	public TemporalDataset(String name, List<TemporalDocument> documents) {
		this(name);
		for (TemporalDocument doc : documents)
			addDocument(doc);
	}

	public Iterator<TemporalSentence> iterator() {
		return sentences.iterator();
	}

	public int size() {
		return sentences.size();
	}

	public void addDocument(TemporalDocument document) {
		addDocument(document, false);
	}

	public void addDocument(TemporalDocument document, boolean ignoreDCT) {
		documents.add(ignoreDCT ? document.withoutDCT() : document);
		sentences.addAll(document.getSentences());
	}

	public List<TemporalDocument> getDocuments() {
		return documents;
	}

	public String getName() {
		return name;
	}

	public void serialize(String filename) throws IOException {
		FileOutputStream fileOut = new FileOutputStream(filename);
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
		out.writeObject(this);
		out.close();
		fileOut.close();
	}

	public static TemporalDataset deserialize(String filename) throws IOException, ClassNotFoundException {
		FileInputStream fileIn = new FileInputStream(filename);
		ObjectInputStream in = new ObjectInputStream(fileIn);
		TemporalDataset dataset = (TemporalDataset) in.readObject();
		in.close();
		fileIn.close();
		return dataset;
	}

	public List<List<TemporalDocument>> partition(int k) {
		List<List<TemporalDocument>> partitions = new ArrayList<List<TemporalDocument>>(k);
		for(int i = 0 ; i < k ; i++)
			partitions.add(new LinkedList<TemporalDocument>());

		List<TemporalDocument> shuffledDocuments = new LinkedList<TemporalDocument>(documents);
		Random seed = new Random(0);
		Collections.shuffle(shuffledDocuments, seed);
		
		int count = 0;
		for (TemporalDocument s : shuffledDocuments) {
			partitions.get(count % k).add(s);
			count++;
		}
		return partitions;
	}

	public String toString() {
		String s = "";
		for(TemporalSentence sentence : sentences)
			s += sentence + "\n";
		return s;
	}

	public TemporalDataset withoutDocumentCreationTimes() {
		TemporalDataset newDataset = new TemporalDataset(name);
		for (TemporalDocument d : documents) {
			newDataset.addDocument(d, true);
		}
		return newDataset;
	}

	public void sort() {
		Collections.sort(documents, new Comparator<TemporalDocument>() {
			@Override
			public int compare(TemporalDocument d1, TemporalDocument d2) {
				return d1.getDocID().compareTo(d2.getDocID());
			}
		});
	}

    public void dump(String dir) {
        String outputDir = dir + name + "/";
        new File(outputDir).mkdirs();
        for (TemporalDocument document : documents) {
            TemporalLog.println("progress", "Dumping contents of " + document.getDocID());
            try {
                PrintStream ps = new PrintStream(new File(outputDir + document.getDocID() + ".tml"));
                ps.print(document.dump());
                ps.close();
            } catch (FileNotFoundException e) {
                System.err.println("Unable to open file");
            }
        }
    }
}
