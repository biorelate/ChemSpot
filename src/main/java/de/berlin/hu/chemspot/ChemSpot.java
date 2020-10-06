/*
 * Copyright (c) 2012. Humboldt-Universität zu Berlin, Dept. of Computer Science and Dept.
 * of Wissensmanagement in der Bioinformatik
 * -------------------------------
 *
 * THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS COMMON PUBLIC
 * LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM
 * CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 *
 * http://www.opensource.org/licenses/cpl1.0
 */

package de.berlin.hu.chemspot;

import de.berlin.hu.chemspot.ChemSpotConfiguration.Component;
import de.berlin.hu.types.PubmedDocument;
import de.berlin.hu.uima.ae.feature.FeatureTokenGenerator;
import de.berlin.hu.uima.ae.feature.FeatureTokenGenerator.Feature_Phase;
import de.berlin.hu.uima.ae.tagger.brics.BricsTagger;
import de.berlin.hu.uima.ae.tagger.drug.EumedNERTagger;
import de.berlin.hu.util.Constants;
import de.berlin.hu.util.Constants.ChemicalID;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;
import org.u_compare.shared.semantic.NamedEntity;
import org.u_compare.shared.syntactic.Token;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.util.JCasUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class ChemSpot {
	private static final String CRF_MODEL_RESOURCE_PATH = "resources/banner/model.bin";
	private static final String SENTENCE_MODEL_RESOURCE_PATH = "resources/genia/SentDetectGenia.bin.gz";
	
	// map for holding jCas objects for threads that are using the tag(String) method
    private static Map<Long, JCas> jCases = new HashMap<Long, JCas>();
	
    private TypeSystemDescription typeSystem;
    private AnalysisEngine posTagger;
    private AnalysisEngine sentenceDetector;
    private AnalysisEngine sentenceConverter;
    private AnalysisEngine tokenConverter;
    private AnalysisEngine crfTagger;
    private AnalysisEngine dictionaryTagger;
    private AnalysisEngine chemicalFormulaTagger;
    private AnalysisEngine abbrevTagger;
    private AnalysisEngine drugTagger;
    private AnalysisEngine annotationMerger;
    private AnalysisEngine fineTokenizer;
    private AnalysisEngine stopwordFilter;
    private AnalysisEngine mentionExpander;
    private AnalysisEngine normalizer;
    private FeatureTokenGenerator featureGenerator;
    
    private ChemicalNEREvaluator evaluator;

    public ChemSpot() {
    	this(null, null, null, null);
    }
    
    /**
     * Initializes ChemSpot without a dictionary automaton and a normalizer.
     * @param pathToCRFModelFile the Path to a CRF model
     */
    public ChemSpot(String pathToCRFModelFile, String pathToSentenceModelFile) {
        this(pathToCRFModelFile, null, pathToSentenceModelFile, null);
    }

    /**
     * Initializes ChemSpot without a normalizer.
     * @param pathToCRFModelFile the Path to a CRF model
     */
    public ChemSpot(String pathToCRFModelFile, String pathToDictionaryFile, String pathToSentenceModelFile) {
    	this(pathToCRFModelFile, pathToDictionaryFile, pathToSentenceModelFile, null);
    }
    
    /**
     * Initializes ChemSpot without a normalizer.
     * @param pathToCRFModelFile the Path to a CRF model
     */
    public ChemSpot(String pathToCRFModelFile, String pathToDictionaryFile, String pathToSentenceModelFile, String pathToIDs) {
    	this(pathToCRFModelFile, pathToDictionaryFile, pathToSentenceModelFile, pathToIDs, null);
    }

    /**
     * Initializes ChemSpot with a CRF model, an OpenNLP sentence model and a dictionary automaton.
     * @param pathToCRFModelFile the path to a CRF model
     * @param pathToDictionaryFile the path to a dictionary automaton
     */
    public ChemSpot(String pathToCRFModelFile, String pathToDictionaryFile, String pathToSentenceModelFile, String pathToIDs, String pathToEumedModel) {
    	try {
    		// converting CRF and sentence model paths to URLs to allow loading of models from jar file
    		pathToCRFModelFile = pathToCRFModelFile == null ? this.getClass().getClassLoader().getResource(CRF_MODEL_RESOURCE_PATH).toString() : new File(pathToCRFModelFile).toURI().toURL().toString(); 
        	pathToSentenceModelFile = pathToSentenceModelFile == null ? this.getClass().getClassLoader().getResource(SENTENCE_MODEL_RESOURCE_PATH).toString() : new File(pathToSentenceModelFile).toURI().toURL().toString();
    		
            typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(new XMLInputSource(this.getClass().getClassLoader().getResource("desc/TypeSystem.xml")));
            
            if (ChemSpotConfiguration.useComponent(Component.TOKENIZER)) {
	            fineTokenizer = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/ae/tokenizer/FineGrainedTokenizerAE.xml"))), CAS.NAME_DEFAULT_SOFA);
	            tokenConverter = AnalysisEngineFactory.createPrimitive(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/ae/converter/OpenNLPToUCompareTokenConverterAE.xml"))));
            }
            
            if (ChemSpotConfiguration.useComponent(Component.POS_TAGGER)) {
	            posTagger = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                   .getResource("desc/ae/tagger/opennlp/PosTagger.xml"))), CAS.NAME_DEFAULT_SOFA);
            }
            
            if (ChemSpotConfiguration.useComponent(Component.SENTENCE_DETECTOR)) {
	            sentenceDetector = AnalysisEngineFactory.createPrimitive(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/ae/tagger/opennlp/SentenceDetector.xml"))), "opennlp.uima.ModelName", pathToSentenceModelFile);
	            sentenceConverter = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/ae/converter/OpenNLPToUCompareSentenceConverterAE.xml"))), CAS.NAME_DEFAULT_SOFA);
            }
            
            if (ChemSpotConfiguration.useComponent(Component.CRF)) {
	            System.out.println("Loading CRF...");
	            crfTagger = AnalysisEngineFactory.createPrimitive(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/banner/tagger/BANNERTaggerAE.xml"))),  "BannerModelFile", pathToCRFModelFile);
            }
            
            if (ChemSpotConfiguration.useComponent(Component.DICTIONARY)) {
            	if (pathToDictionaryFile != null) {
            		if (new File(pathToDictionaryFile).exists()) {
            			System.out.println("Loading dictionary...");
		                dictionaryTagger = AnalysisEngineFactory.createPrimitive(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
		                        .getResource("desc/ae/tagger/BricsTaggerAE.xml"))), "DrugBankMatcherDictionaryAutomat", pathToDictionaryFile);
            		}  else {
             			System.out.println("Dictionary file '" + pathToDictionaryFile +  "' does not exist. Tagging without dictionary...");
             		}
            	} else {
            		System.out.println("No dictionary location specified! Tagging without dictionary...");
            	}
            }
            
            if (ChemSpotConfiguration.useComponent(Component.SUM_TAGGER)) {
	            chemicalFormulaTagger = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/ae/tagger/ChemicalFormulaTaggerAE.xml"))), CAS.NAME_DEFAULT_SOFA);
        	}
            
            if (ChemSpotConfiguration.useComponent(Component.ABBREV)) {
	            abbrevTagger = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/ae/tagger/AbbreviationTaggerAE.xml"))), CAS.NAME_DEFAULT_SOFA);
            }
            
            if (ChemSpotConfiguration.useComponent(Component.EUMED_TAGGER)) {
            	if (pathToEumedModel != null) {
            		if (new File(pathToEumedModel).exists()) {
            			System.out.println("Initializing multi-class tagger...");
            			drugTagger = AnalysisEngineFactory.createPrimitive(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
        	                    .getResource("desc/ae/tagger/EumedTaggerAE.xml"))), EumedNERTagger.PATH_TO_EUMED_MODEL, pathToEumedModel);
            		}  else {
             			System.out.println("Multi-class model file '" + pathToEumedModel +  "' does not exist. Tagging without multi-class tagger...");
             		}
            	} else {
            		System.out.println("No multi-class model location specified! Tagging without multi-class tagger...");
            	}
            }
            
            if (ChemSpotConfiguration.useComponent(Component.MENTION_EXPANDER)) {
            	mentionExpander = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
            			.getResource("desc/ae/expander/MentionExpanderAE.xml"))), CAS.NAME_DEFAULT_SOFA);
            }
            
            if (ChemSpotConfiguration.useComponent(Component.ANNOTATION_MERGER)) {
            	annotationMerger = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
                        .getResource("desc/ae/AnnotationMergerAE.xml"))), CAS.NAME_DEFAULT_SOFA);
            }
            
            if (ChemSpotConfiguration.useComponent(Component.NORMALIZER) || ChemSpotConfiguration.useComponent(Component.CHEMHITS)) {
            	if (pathToIDs != null) {
            		if (new File(pathToIDs).exists()) {
	            		normalizer = AnalysisEngineFactory.createPrimitive(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	            				.getResource("desc/ae/normalizer/NormalizerAE.xml"))), "PathToIDs", pathToIDs);
	            		if (ChemSpotConfiguration.useComponent(Component.DICTIONARY) && ChemSpotConfiguration.initializeDictionaryFromNormalizer()) {
	            			dictionaryTagger = AnalysisEngineFactory.createPrimitive(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
			                        .getResource("desc/ae/tagger/BricsTaggerAE.xml"))), BricsTagger.PATH_TO_DICTIONARY, "");
	            		}
            		} else {
            			System.out.println("Normalization ids file '" + pathToIDs +  "' does not exist. Tagging without subsequent normalization...");
            		}
            	} else System.out.println("No location for ids specified! Tagging without subsequent normalization...");
            }
            
            if (ChemSpotConfiguration.useComponent(Component.STOPWORD_FILTER)) {
	            stopwordFilter = AnalysisEngineFactory.createAnalysisEngine(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(this.getClass().getClassLoader()
	                    .getResource("desc/ae/filter/StopwordFilterAE.xml"))), CAS.NAME_DEFAULT_SOFA);
            }
            
            if (ChemSpotConfiguration.useComponent(Component.FEATURE_GENERATOR)) {
	            featureGenerator = new FeatureTokenGenerator();
            }
            
            setEvaluator(new ChemicalNEREvaluator());
            
            System.out.println("Finished initializing ChemSpot.");
        } catch (UIMAException e) {
            System.err.println("Failed initializing ChemSpot.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Failed initializing ChemSpot.");
            e.printStackTrace();
        }
    }

    /**
     * Returns all mentions (non-goldstandard entities) from a jcas object.
     * 
     * @param jcas the jcas
     * @return
     */
    public static List<Mention> getMentions(JCas jcas) {
    	List<Mention> mentions = new ArrayList<Mention>();
        Iterator<NamedEntity> entities = JCasUtil.iterator(jcas, NamedEntity.class);
        while (entities.hasNext()) {
            NamedEntity entity = entities.next();
            //disregards gold-standard mentions
            if (!Constants.GOLDSTANDARD.equals(entity.getSource())) {
            	Mention mention = new Mention(entity);
            	if (ChemSpotConfiguration.isAnnotate(mention.getType())) {
            		mentions.add(mention);
            	}
            }
        }

        return mentions;
    }
    
    /**
     * Returns all goldstandard entities from a jcas object.
     * 
     * @param jcas the jcas
     * @return
     */
    public static List<Mention> getGoldstandardAnnotations(JCas jcas) {
    	List<Mention> result = new ArrayList<Mention>();
        Iterator<NamedEntity> entities = JCasUtil.iterator(jcas, NamedEntity.class);
        while (entities.hasNext()) {
            NamedEntity entity = entities.next();
            if (Constants.GOLDSTANDARD.equals(entity.getSource())) {
                result.add(new Mention(entity));
            }
        }

        return result;
    }
    
    /**
     * Reads a text from a file and puts the content into the provided jcas.
     * 
     * @param jcas the jcas
     * @param pathToFile the path to the text file
     * @throws IOException
     */
    public static void readFile(JCas jcas, String pathToFile) throws IOException {
    	FileInputStream stream = new FileInputStream(new File(pathToFile));
    	String text = null;
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			text = Charset.defaultCharset().decode(bb).toString();
		} finally {
			stream.close();
		}
		
		jcas.setDocumentText(text);
        PubmedDocument pd = new PubmedDocument(jcas);
        pd.setBegin(0);
        pd.setEnd(text.length());
        pd.setPmid("");
        pd.addToIndexes(jcas);
        
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jcas);
        srcDocInfo.setUri(new File(pathToFile).getAbsoluteFile().toURI().toString());
        srcDocInfo.setOffsetInSource(0);
        srcDocInfo.setDocumentSize((int) new File(pathToFile).length());
        srcDocInfo.setBegin(0);
        srcDocInfo.setEnd(text.length());
        srcDocInfo.addToIndexes();
    }
    
    /**
     * Reads a text from a gzipped file and puts the content into the provided jcas.
     * 
     * @param jcas the jcas
     * @param pathToFile the path to the text file
     * @throws IOException
     */
    public static void readGZFile(JCas jcas, String pathToFile) throws IOException {
        File file = new File(pathToFile);
        String text;
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(file)) ) );

        StringBuilder textBuffer = new StringBuilder();
        Integer currindex = -1;
        while(reader.ready()){
            PubmedDocument pmdoc = new PubmedDocument(jcas);
            String s = reader.readLine();
            if (s != null) {
                //split line into pmid and text
                String pmid = s.substring(0, s.indexOf("\t"));
                String annot = s.substring(s.indexOf("\t"));
                //two = splitFirst(s, "\t");
                pmdoc.setPmid(pmid);

                //append text
                textBuffer.append(annot).append("\n");
                pmdoc.setBegin(currindex + 1);
                Integer len = annot.length();
                currindex = currindex + len + 1;
                pmdoc.setEnd(currindex);
                pmdoc.addToIndexes();
            }
        }

        text = textBuffer.toString();

        //put document in CAS
        jcas.setDocumentText(text);
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jcas);
        srcDocInfo.setUri(file.getAbsoluteFile().toURI().toString());
        srcDocInfo.setOffsetInSource(0);
        srcDocInfo.setDocumentSize((int) file.length());
        srcDocInfo.setBegin(0);
        srcDocInfo.setEnd(currindex);
        srcDocInfo.addToIndexes();
    }
    
    private static long start = 0;
    public static void printTime(String action) {
    	if (ChemSpotConfiguration.useComponent(Component.PROFILER)) {
    		System.out.printf("%s: %.1f s%n", action, (System.currentTimeMillis() - start) / 1000.0);
    		start = System.currentTimeMillis();
    	}
    }
    
    private static void startTimer() {
    	start = ChemSpotConfiguration.useComponent(Component.PROFILER) ? System.currentTimeMillis() : 0;
    	if (start != 0) {
    		System.out.println("start profiling...");
    	}
    }
    
    /**
     * Finds chemical entities in the document of a {@code JCas} object and returns a list of mentions.
     * @param jcas contains the document text
     * @return a list of mentions
     */
    public List<Mention> tag(JCas jcas) {
    	List<NamedEntity> otherEntities = null;
    	startTimer();
        try {
        	if (fineTokenizer != null) {
        		fineTokenizer.process(jcas);
        		printTime("tokenization");
        	}
        	
            synchronized (this) {
            	if (sentenceDetector != null) {
            		sentenceDetector.process(jcas);
            		printTime("sentence detector");
            	}
            	if (posTagger != null) {
            		posTagger.process(jcas);
            		printTime("POS tagger");
            	}
            }
            if (tokenConverter != null) {
            	tokenConverter.process(jcas);
            	printTime("token converter");
            }
            if (sentenceConverter != null) {
            	sentenceConverter.process(jcas);
            	printTime("sentence converter");
            }
            if (crfTagger != null) {
            	crfTagger.process(jcas);
            	printTime("crf tagger");
            }
            if (dictionaryTagger != null) {
            	dictionaryTagger.process(jcas);
            	printTime("dictionary tagger");
            }
            if (chemicalFormulaTagger != null) {
            	chemicalFormulaTagger.process(jcas);
            	printTime("chemical formula tagger");
            }
            if (abbrevTagger != null) {
            	abbrevTagger.process(jcas);
            	printTime("abbreviation tagger");
            }
            if (drugTagger != null) {
            	drugTagger.process(jcas);
            	printTime("drug tagger");
            }
            if (featureGenerator != null) {
            	if (normalizer != null) {
            		normalizer.process(jcas);
            	}
            	featureGenerator.process(jcas, Feature_Phase.PHASE1);
            	printTime("feature generation phase 1 (+ preliminary normalization run)");
            }
            if (stopwordFilter != null) {
            	stopwordFilter.process(jcas);
            	printTime("stopword filter");
            }
            if (mentionExpander != null) {
            	mentionExpander.process(jcas);
            	printTime("mention expander");
            }
            if (featureGenerator != null) {
            	featureGenerator.process(jcas, Feature_Phase.PHASE2);
            	printTime("feature generator phase 2");
            }
            if (annotationMerger != null) {
            	annotationMerger.process(jcas);
            	printTime("annotation merger");
            }
            if (featureGenerator != null) {
            	featureGenerator.process(jcas, Feature_Phase.PHASE3);
            	printTime("feature generator phase 3");
            }
            if (normalizer != null) {
            	normalizer.process(jcas);
            	printTime("normalizer");
            }
            if (featureGenerator != null) {
            	featureGenerator.process(jcas, Feature_Phase.PHASE4);
            	printTime("feature generator phase 4");
            }
        } catch (AnalysisEngineProcessException e) {
            System.err.println("Failed to extract chemicals from text.");
            e.printStackTrace();
        } finally {
        	if (otherEntities != null && !otherEntities.isEmpty()) {
        		for (NamedEntity ne : otherEntities) {
        			ne.addToIndexes();
        		}
        	}
        }
        
        return getMentions(jcas);
    	
    	/*Oscar oscar = new Oscar();
    	ChemicalEntityRecogniser recogniser = new MEMMRecogniser(new PubMedModel(), OntologyTerms.getDefaultInstance(), new ChemNameDictRegistry(Locale.ENGLISH));
    	
    	List<PubmedDocument> documents = new ArrayList<PubmedDocument>();
    	for (PubmedDocument doc : JCasUtil.iterate(jcas, PubmedDocument.class)) {
    		documents.add(doc);
    	}
    	if (documents.isEmpty()) {
    		PubmedDocument doc = new PubmedDocument(jcas);
			doc.setBegin(0);
			doc.setEnd(jcas.getDocumentText().length());
			doc.setPmid("");
			doc.addToIndexes(jcas);	
    		documents.add(doc);
    	}
    	for (PubmedDocument doc : documents) {
	    	List<uk.ac.cam.ch.wwmm.oscar.document.NamedEntity> entities = recogniser.findNamedEntities(oscar.tokenise(doc.getCoveredText()), ResolutionMode.REMOVE_BLOCKED);
	    	for (uk.ac.cam.ch.wwmm.oscar.document.NamedEntity rne : entities) {
	    	    if (!rne.getType().isInstance(NamedEntityType.COMPOUND)){
					continue;
				}
	    		
	    	    NamedEntity entity = new NamedEntity(jcas);
	    	    entity.setBegin(doc.getBegin() + rne.getStart());
	    	    entity.setEnd(doc.getBegin() + rne.getEnd());
	    	    for (String id : rne.getOntIds()) {
	    	    	if (id.contains("CHEBI:")) {
	    	    		entity.setId("," + id);
	    	    	}
	    	    }
	    	    entity.setSource("OSCAR");
	    	    entity.addToIndexes();
	    	}
    	}
    	
    	return null;*/
    }

    /**
     * Finds chemical entities in a {@code text} and returns a list of mentions.
     * @param text natural language text from which ChemSpot shall extract chemical entities
     * @return a list of mentions
     * @throws UIMAException 
     */
    public List<Mention> tag(String text) {
    	// get JCas object for currently executed thread
    	long threadId = Thread.currentThread().getId();
    	
    	// create new jcas if necessary (i.e. a thread calls this method for the first time)
    	if (!jCases.containsKey(threadId)) {
    		synchronized (jCases) {
    			try {
					jCases.put(threadId, JCasFactory.createJCas(typeSystem));
				} catch (UIMAException e) {
					throw new RuntimeException(e);
				}
    		}
    	}
    	// get jcas
    	JCas jcas = jCases.get(threadId);
    	jcas.reset();    	
    	
    	// TODO: for applications that create an excessive amount of threads it would be best
    	// to release the jcas object once a calling thread dies in order to reduce memory consumption.
    	// This would probably require a new thread for each one that calls this method (to call its 
    	// thread.join() method), which seems like a bit of an overkill for applications with few threads.
        
        jcas.setDocumentText(text);
        PubmedDocument pd = new PubmedDocument(jcas);
        pd.setBegin(0);
        pd.setEnd(text.length());
        pd.setPmid("");
        pd.addToIndexes(jcas);
        return tag(jcas);
    }

    /**
     * Converts all annotations from jcas to the IOB format
     * 
     * @param jcas the jcas
     * @return
     */
    public static String convertToIOB(JCas jcas) {
    	StringBuilder sb = new StringBuilder();
        HashMap<String, ArrayList<NamedEntity>> goldAnnotations = new HashMap<String, ArrayList<NamedEntity>>();
        HashMap<String, ArrayList<NamedEntity>> pipelineAnnotations = new HashMap<String, ArrayList<NamedEntity>>();
    	
    	System.out.println("Converting annotations to IOB format...");
    	
        Iterator<PubmedDocument> abstracts = JCasUtil.iterator(jcas, PubmedDocument.class);
        while (abstracts.hasNext()) {
            PubmedDocument pubmedAbstract = abstracts.next();
            sb.append("### ").append(pubmedAbstract.getPmid()).append("\n");
            int offset = pubmedAbstract.getBegin();
            String pmid = pubmedAbstract.getPmid();

            List<Token> tokens = JCasUtil.selectCovered(Token.class, pubmedAbstract);
            for (Token token : tokens) {
                token.setLabel("O");
            }

            List<NamedEntity> entities = JCasUtil.selectCovered(NamedEntity.class, pubmedAbstract);
            for (NamedEntity entity : entities) {
                int firstTokenBegin = 0;
                int lastTokenEnd = 0;
                
                String id = "";
                Mention m = new Mention(entity);
                for (ChemicalID type : ChemicalID.values()) {
                	String tempId = m.getId(type);
                	id += (!id.isEmpty() ? "\t" : "") + (tempId != null && !tempId.isEmpty() ? tempId : "");
                }
                if (!Constants.GOLDSTANDARD.equals(entity.getSource())) {
                    if (pipelineAnnotations.containsKey(pmid)) {
                        pipelineAnnotations.get(pmid).add(entity);
                    } else {
                        ArrayList<NamedEntity> tempArray = new ArrayList<NamedEntity>();
                        tempArray.add(entity);
                        pipelineAnnotations.put(pmid, tempArray);
                    }
                    
                    String labelName = m.getType().toString();
                    List<Token> entityTokens = JCasUtil.selectCovered(Token.class, entity);
                    boolean first = true;
                    for (Token token : entityTokens) {
                        if (first) {
                            if (id.isEmpty()) token.setLabel("B-" + labelName); else token.setLabel("B-" + labelName + "\t" + id);
                            first = false;
                            firstTokenBegin = token.getBegin();
                        } else {
                            token.setLabel("I-" + labelName);
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
                    sb.append(" " + "\t" + 0 + "\t").append(token.getBegin() - offset).append("\t\t|O\n");
                }
                firstToken = false;
                sb.append(token.getCoveredText()).append("\t").append(token.getBegin() - offset).append("\t").append(token.getEnd() - offset).append("\t\t|").append(token.getLabel()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    public static String serializeAnnotations(JCas jcas) {
        int offset;
        StringBuilder sb = new StringBuilder();
        Iterator<PubmedDocument> documentIterator = JCasUtil.iterator(jcas, PubmedDocument.class);
        while (documentIterator.hasNext()) {
            PubmedDocument document = documentIterator.next();
            offset = document.getBegin();
            String pmid = document.getPmid();
            int numberOfEntities = 0;
            Iterator<NamedEntity> entityIterator = JCasUtil.iterator(document, NamedEntity.class, true, true);
            while (entityIterator.hasNext()) {
                NamedEntity entity = entityIterator.next();
                if (!Constants.GOLDSTANDARD.equals(entity.getSource())) {
                    //offset fix for GeneView
                    //int begin = entity.getBegin() - offset;
                    int begin = entity.getBegin() - offset - 1;
                    //int end = entity.getEnd() - offset - 1;
                    int end = entity.getEnd() - offset - 2;
                    String text = entity.getCoveredText();
                    
                    String id = "";
                    Mention m = new Mention(entity);
                    for (ChemicalID type : ChemicalID.values()) {
                    	String tempId = m.getId(type);
                    	id += "\t" + (tempId != null && !tempId.isEmpty() ? tempId : "");
                    }
                    
                    sb.append(pmid + "\t" + begin + "\t" + end + "\t" + text + "\t" + m.getType().toString() + id + "\n");
                }
                numberOfEntities++;
            }
            if (numberOfEntities == 0) {
            	sb.append(pmid + "\t-1\t-1\t\\N\t\\N\t\\N" + new String(new char[ChemicalID.values().length]).replace("\0", "\t") + "\n");
            }
        }
        
        return sb.toString();
    }

	public ChemicalNEREvaluator getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(ChemicalNEREvaluator evaluator) {
		this.evaluator = evaluator;
	}
	
	public FeatureTokenGenerator getFeatureTokenGenerator() {
		return featureGenerator;
	}
}
