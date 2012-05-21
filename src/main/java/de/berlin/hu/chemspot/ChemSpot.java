package de.berlin.hu.chemspot;

import de.berlin.hu.types.PubmedDocument;
import de.berlin.hu.uima.cc.eval.ComparableAnnotation;
import de.berlin.hu.wbi.common.research.Evaluator;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.u_compare.shared.semantic.NamedEntity;
import org.u_compare.shared.syntactic.Token;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.util.JCasUtil;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class ChemSpot {
	private TypeSystemDescription typeSystem;
	private AnalysisEngine sentenceDetector;
	private AnalysisEngine sentenceConverter;
	private AnalysisEngine bannerTagger;
	private AnalysisEngine linnaeusTagger;
	private AnalysisEngine annotationMerger;
	private AnalysisEngine fineTokenizer;
	private AnalysisEngine stopwordFilter;	
	private Map<String,Integer> abstractOffsets = new HashMap<String,Integer>();

    /**
     * Initialize ChemSpot without a dictionary automaton.
     * @param pathToModelFile the Path to a CRF model
     */
    public ChemSpot(String pathToModelFile) {
        new ChemSpot(pathToModelFile, "");
    }

    /**
     * Initialize ChemSpot with a CRF model and a dictionary automaton.
     * @param pathToModelFile the path to a CRF model
     * @param pathToDictionaryFile the ath to a dictionary automaton
     */
	public ChemSpot(String pathToModelFile, String pathToDictionaryFile) {
		try {
            //FIXME: find a way to access the descriptors in the jar rather then outside
			typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescription("desc/TypeSystem");
			fineTokenizer = AnalysisEngineFactory.createAnalysisEngine("desc/util/fineTokenizerAEDescriptor");
			sentenceDetector = AnalysisEngineFactory.createAnalysisEngine("desc/ae/opennlp/SentenceDetector");
			sentenceConverter = AnalysisEngineFactory.createAnalysisEngine("desc/util/openNLPToUCompareSentenceConverterAEDescriptor");
			System.out.println("Loading CRF...");
			bannerTagger = AnalysisEngineFactory.createAnalysisEngine("desc/banner/tagger/bannerTaggerAEDescriptor", "BannerModelFile", pathToModelFile);
			if (pathToDictionaryFile!= null) System.out.println("Loading dictionary..."); else System.out.println("No dictionary location specified! Tagging without dictionary...");
			if (pathToDictionaryFile != null) linnaeusTagger = AnalysisEngineFactory.createAnalysisEngine("desc/ae/drugTagger/drugTaggerAEDescriptor", "DrugBankMatcherDictionaryAutomat", pathToDictionaryFile);
			annotationMerger = AnalysisEngineFactory.createAnalysisEngine("desc/ae/annotationMergerAEDescriptor");
			stopwordFilter = AnalysisEngineFactory.createAnalysisEngine("desc/ae/filter/stopwordFilterAEDescriptor");
			System.out.println("Finished initialization! Start tagging...");
		} catch (UIMAException e) {
			System.err.println("Failed initializing ChemSpot.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Failed initializing ChemSpot.");
			e.printStackTrace();
		}
	}

    /**
     * Given a {@code JCas} object, this method extracts chemicals from the document text and returns a list of {@code Mention}.
     * @param jcas contains the document text
     * @return a list of mentions
     */
	public List<Mention> tag(JCas jcas) {
		try {
			fineTokenizer.process(jcas);
			sentenceDetector.process(jcas);
			sentenceConverter.process(jcas);
			bannerTagger.process(jcas);
			if (linnaeusTagger != null) linnaeusTagger.process(jcas);
			annotationMerger.process(jcas);
			stopwordFilter.process(jcas);
			
			List<Mention> mentions = new ArrayList<Mention>();
			Iterator<NamedEntity> entities = JCasUtil.iterator(jcas, NamedEntity.class);
			while (entities.hasNext()) {
				NamedEntity entity = (NamedEntity) entities.next();
                //do not include gold-standard mentions!
				if (!"goldstandard".equals(entity.getSource())) {
					mentions.add(new Mention(entity.getBegin(), entity.getEnd(), entity.getCoveredText(), entity.getId(), entity.getSource()));
				}
			}
			return mentions;
		} catch (AnalysisEngineProcessException e) {
			System.err.println("Failed to extract chemicals from text.");
			e.printStackTrace();
		}
		return null;
	}

    /**
     * Extracts chemicals from a {@code text} and returns a list of {@code Mention}.
     * @param text natural language text from which you want to extract chemical entities
     * @return a list of mentions
     */
	public List<Mention> tag(String text) {
		try {
			JCas jcas = JCasFactory.createJCas(typeSystem);
			jcas.setDocumentText(text);
			PubmedDocument pd = new PubmedDocument(jcas);
			pd.setBegin(0);
			pd.setEnd(text.length());
			pd.setPmid("");
			pd.addToIndexes(jcas);
			return tag(jcas);
		} catch (UIMAException e) {
			System.err.println("Failed to extract chemicals from text.");
			e.printStackTrace();
		}
		return null;
	}
	

	public String tagJCas(JCas jcas) throws AnalysisEngineProcessException {
		return tagJCas(jcas, false, true);	
	}

	public String tagString(String text) throws UIMAException, IOException {
		JCas jcas = JCasFactory.createJCas(typeSystem);
		jcas.setDocumentText(text);
		PubmedDocument pd = new PubmedDocument(jcas);
		pd.setBegin(0);
		pd.setEnd(text.length());
		pd.setPmid("");
		pd.addToIndexes(jcas);
		return tagJCas(jcas, false, true);
	}

	public JCas tagGZ(String pathToGZ) throws UIMAException, FileNotFoundException, IOException {
		JCas jcas = JCasFactory.createJCas(typeSystem);
		readGZFile(jcas, pathToGZ);
		tagJCas(jcas, false, false);
		return jcas;	
	}

	public String tagJCas(JCas jcas, boolean evaluate, boolean convertToIOB) throws AnalysisEngineProcessException {
		//TODO change to buffered string builder!
		StringBuilder sb = new StringBuilder();
		fineTokenizer.process(jcas);
		sentenceDetector.process(jcas);
		sentenceConverter.process(jcas);
		bannerTagger.process(jcas);
		if (linnaeusTagger != null) linnaeusTagger.process(jcas);
		annotationMerger.process(jcas);
		stopwordFilter.process(jcas);


		HashMap<String, ArrayList<NamedEntity>> goldAnnotations = new HashMap<String, ArrayList<NamedEntity>>();
		HashMap<String, ArrayList<NamedEntity>> pipelineAnnotations = new HashMap<String, ArrayList<NamedEntity>>();

		if (convertToIOB) {
			System.out.println("Converting annotations to IOB format...");
			Iterator<PubmedDocument> abstracts = JCasUtil.iterator(jcas, PubmedDocument.class);

			while (abstracts.hasNext()) {
				PubmedDocument pubmedAbstract = abstracts.next();		
				sb.append("### " + pubmedAbstract.getPmid() + "\n");		
				int offset = pubmedAbstract.getBegin();
				String pmid = pubmedAbstract.getPmid();

				abstractOffsets.put(pmid, offset);

				List<Token> tokens = JCasUtil.selectCovered(Token.class, pubmedAbstract);
				for (Token token : tokens) {		
					token.setLabel("O");
				}


				List<NamedEntity> entities = JCasUtil.selectCovered(NamedEntity.class, pubmedAbstract);
				for (NamedEntity entity : entities) {								
					int firstTokenBegin = 0;
					int lastTokenEnd = 0;

					String id = entity.getId();
					if (id == null) id = "";
					if (!"goldstandard".equals(entity.getSource())) {					
						if (pipelineAnnotations.containsKey(pmid)) {
							pipelineAnnotations.get(pmid).add(entity);						
						} else {
							ArrayList<NamedEntity> tempArray = new ArrayList<NamedEntity>();
							tempArray.add(entity);
							pipelineAnnotations.put(pmid, tempArray);
						}			
						List<Token> entityTokens = JCasUtil.selectCovered(Token.class, entity);
						boolean first = true;		
						for (Token token : entityTokens) {														
							if (first) {
								if (id.isEmpty()) token.setLabel("B-CHEMICAL"); else token.setLabel("B-CHEMICAL" + "\t" + id);							
								first = false;
								firstTokenBegin = token.getBegin();	
							} else {
								token.setLabel("I-CHEMICAL" + "\t" + id);
							}						
							lastTokenEnd = token.getEnd();
						}	
						assert entity.getBegin() == firstTokenBegin : (id + ": " + entity.getBegin() + " -> " + firstTokenBegin);
						assert entity.getEnd() == lastTokenEnd : (id + ": " + entity.getEnd() + " -> " + lastTokenEnd);
					} else {
						if (goldAnnotations.containsKey(pmid)) {
							goldAnnotations.get(pmid).add(entity);						
						} else {
							ArrayList<NamedEntity> tempArray = new ArrayList<NamedEntity>();
							tempArray.add(entity);
							goldAnnotations.put(pmid, tempArray);
						}
					}
				}					

				List<Token> tokensToPrint = JCasUtil.selectCovered(Token.class, pubmedAbstract);
				boolean firstToken = true;
				for (Token token : tokensToPrint) {					
					if (firstToken && (token.getBegin() - offset) != 0) {
						sb.append(" " + "\t" + 0 + "\t" + (token.getBegin()-offset) + "\t\t|O\n");
					}
					firstToken = false;
					sb.append(token.getCoveredText() + "\t" + (token.getBegin()-offset) + "\t" + (token.getEnd()-offset) + "\t\t|" + token.getLabel() +"\n");
				}		
			}
		}

		if (evaluate) {
			System.out.println("Starting evaluation...");
			evaluate(goldAnnotations, pipelineAnnotations, 0);		
		}
		return sb.toString();
	}

	private int TP = 0;
	private int FP = 0;
	private int FN = 0;

	public void evaluate(HashMap<String, ArrayList<NamedEntity>> goldAnnotations, HashMap<String, ArrayList<NamedEntity>> pipelineAnnotations, int softBoundary) {
		List<ComparableAnnotation> goldAnnoationsComparable = new ArrayList<ComparableAnnotation>();
		List<ComparableAnnotation> pipelineAnnotationsComparable = new ArrayList<ComparableAnnotation>();

		for (String pmid : goldAnnotations.keySet()) {
			for (NamedEntity namedEntity : goldAnnotations.get(pmid)) {
				goldAnnoationsComparable.add(ComparableAnnotation.createInstance(namedEntity.getBegin(), namedEntity.getEnd(), namedEntity.getCoveredText(), softBoundary, namedEntity.getCAS(), pmid));
			}
		}
		for (String pmid : pipelineAnnotations.keySet()) {
			for (NamedEntity namedEntity : pipelineAnnotations.get(pmid)) {
				pipelineAnnotationsComparable.add(ComparableAnnotation.createInstance(namedEntity.getBegin(), namedEntity.getEnd(), namedEntity.getCoveredText(), softBoundary, namedEntity.getCAS(), pmid));
			}
		}

		if (goldAnnoationsComparable.size() == 0) {
			FP += pipelineAnnotationsComparable.size();
		} else if (pipelineAnnotationsComparable.size() == 0) {
			FN += goldAnnoationsComparable.size();
		} else {
			Evaluator<ComparableAnnotation,ComparableAnnotation> evaluator = new Evaluator<ComparableAnnotation,ComparableAnnotation>(pipelineAnnotationsComparable, goldAnnoationsComparable);
			evaluator.evaluate();

			TP += evaluator.getTruePositives().size();
			FP += evaluator.getFalsePositives().size();
			FN += evaluator.getFalseNegatives().size();

			System.out.format("True Positives:\t\t%d\nFalse Positives:\t%d\nFalse Negatives:\t%d\n", TP, FP, FN);
			double precision = (double) TP / ((double) TP + FP);
			double recall = (double) TP / ((double) TP + FN);
			double fscore = 2 * (precision * recall) / (precision + recall);
			System.out.format("Precision:\t\t%f\nRecall:\t\t\t%f\nF1 Score:\t\t%f\n", precision, recall, fscore);
			System.out.println();
		}
	}

	private static void readGZFile(JCas jcas, String pathToFile) throws FileNotFoundException, IOException {
		File file = new File(pathToFile);
		String text;   
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(
						new GZIPInputStream(
								new FileInputStream(file)) ) );

		StringBuffer textBuffer = new StringBuffer();
		Integer currindex = -1;
		while(reader.ready()){
			PubmedDocument pmdoc = new PubmedDocument(jcas);
			String s = reader.readLine();
			if (s != null) {
				//split line into pmid and text		
				//String[] two = new String[2];
				String pmid = s.substring(0, s.indexOf("\t"));
				String annot = s.substring(s.indexOf("\t"));
				//two = splitFirst(s, "\t");				
				pmdoc.setPmid(pmid);
	
				//String annot = new String(two[1]);
				//append text
				textBuffer.append(annot + "\n");
				pmdoc.setBegin(currindex + 1);
				Integer len = annot.length();
				currindex = currindex + len + 1;
				pmdoc.setEnd(currindex);	
				pmdoc.addToIndexes();
			}
		}

		text = textBuffer.toString();

		// put document in CAS
		jcas.setDocumentText(text);
		SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jcas);
		srcDocInfo.setUri(file.getAbsoluteFile().toURI().toString());
		srcDocInfo.setOffsetInSource(0);
		srcDocInfo.setDocumentSize((int) file.length());
		srcDocInfo.setBegin(0);
		srcDocInfo.setEnd(currindex);
		srcDocInfo.addToIndexes();
	}
}