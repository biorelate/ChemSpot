package de.berlin.hu.chemspot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import de.berlin.hu.util.Constants.ChemicalType;

public class ChemSpotConfiguration {
	// Constants
	public static enum Corpus {IOB, CRAFT, GZ, NACTEM, PATENT, DDI, XMI, CHEMDNER, TXT};
	public static enum Component {TOKENIZER, SENTENCE_DETECTOR, POS_TAGGER, CRF, DICTIONARY, SUM_TAGGER, ABBREV, EUMED_TAGGER, MENTION_EXPANDER, ANNOTATION_MERGER, STOPWORD_FILTER, NORMALIZER, OPSIN, FEATURE_GENERATOR, CHEMHITS, PROFILER};
	private static final Component[] DEFAULT_DEACTIVATED = {Component.FEATURE_GENERATOR, Component.CHEMHITS, Component.PROFILER};
	private static final ChemicalType[] DEFAULT_DEACTIVATED_ANNOTATIONS = {};
	private static final ChemicalType[] DEFAULT_DEACTIVATED_ANNOTATIONS_EUMED = {};
	
	private static final String CORPUS_PREFIX = "corpus.";
	
	private static final String OUTPUT_PATH = "output.path";
	private static final String XMI_OUTPUT_PATH = "output.path.xmi";
	private static final String CONVERT_TO_IOB = "output.convertToIOB";
	
	private static final String SENTENCE_MODEL = "sentence_model.path";
	private static final String CRF_MODEL = "crf.model.path";
	private static final String DICTIONARY = "dict.path";
	private static final String IDS = "ids.path";
	private static final String DRUG_MODEL = "drug.model.path";
	
	private static final String EVALUATION = "evaluation";
	private static final String DETAILED_EVALUATION = "evaluation.detailed";
	private static final String THREADING = "threading";
	private static final String THREAD_NR = "threading.number_of_threads";
	private static final int DEFAULT_THREAD_NR = 4;
	
	private static final String COMPONENT_PREFIX = "component.";
	private static final String DICTIONARY_INITIALIZE_FROM_NORMALIZER = COMPONENT_PREFIX + Component.DICTIONARY.toString().toLowerCase() + ".initializeFromNormalizer";
	private static final String DICTIONARY_FILTER_LENGTH = COMPONENT_PREFIX + Component.DICTIONARY.toString().toLowerCase() + ".filterLength";
	
	private static final String ANNOTATIONS_PREFIX = "annotation.";
	private static final String ANNOTATIONS_PREFIX_EUMED = "annotation.eumed.";
	
	private static final String UPDATE_PREFIX = "update.";
	private static final String UPDATE_REMOVE_TEMPORARY_FILES = UPDATE_PREFIX + "removeTemporaryFiles";
	private static final String UPDATE_CHEBI_SDF_URL = UPDATE_PREFIX + "chebi.sdf.url";
	private static final String UPDATE_CHEBI_MUST_CONTAIN_FORMULA = UPDATE_PREFIX + "chebi.mustContainFormula";
	private static final String UPDATE_PUBCHEM_SDF_URL = UPDATE_PREFIX + "pubchem.sdf.url";
	private static final String UPDATE_PUBCHEM_MAX_LENGTH = UPDATE_PREFIX + "pubchem.maxLength";
	
	// Variables
	private static Properties properties = null;
	
	static {
		properties = new Properties();
	}
	
	public static void initialize() throws FileNotFoundException, IOException {
		if (new File("conf/chemspot.cfg").exists()) {
			initialize("conf/chemspot.cfg");
		} else if (new File("chemspot.cfg").exists()) {
			initialize("chemspot.cfg");
		}
	}
	
	public static void initialize(String configFilePath) throws FileNotFoundException, IOException {
		initialize(configFilePath, true);
	}
	
	public static void initialize(String configFilePath, boolean overwrite) throws FileNotFoundException, IOException {
		initialize(new FileInputStream(configFilePath), overwrite);
	}
	
	public static void initialize(InputStream inStream, boolean overwrite) throws IOException {
		if (overwrite) {
			properties.load(inStream);
		} else {
			Properties temp = new Properties();
			temp.load(inStream);
			temp.putAll(properties);
			properties = temp;
		}
	}
	
	public static String getProperty(String property) {
		String result = properties.getProperty(property);
		if (result != null) result = result.trim();
		return result;
	}
	
	public static String getProperty(String property, String defaultValue) {
		return properties.getProperty(property, defaultValue);
	}
	
	public static String getPathToCorpus(Corpus corpus) {
		return getProperty(CORPUS_PREFIX + corpus);
	}

	public static String getOutputPath() {
		return getProperty(OUTPUT_PATH);
	}

	public static String getXMIOutputPath() {
		return getProperty(XMI_OUTPUT_PATH);
	}
	
	public static String getUpdateOutputPath() {
		return getProperty(UPDATE_PREFIX + OUTPUT_PATH);
	}

	public static boolean isConvertToIob() {
		return "true".equals(getProperty(CONVERT_TO_IOB));
	}

	public static String getSentenceModelPath() {
		return getProperty(SENTENCE_MODEL);
	}

	public static String getCRFModelPath() {
		return getProperty(CRF_MODEL);
	}

	public static String getDictionaryPath() {
		return getProperty(DICTIONARY);
	}

	public static String getDictionaryUpdatePath() {
		return getProperty(UPDATE_PREFIX + DICTIONARY);
	}

	public static String getIdsFilePath() {
		return getProperty(IDS);
	}
	
	public static String getIdsFileUpdatePath() {
		return getProperty(UPDATE_PREFIX + IDS);
	}
	
	public static String getDrugModelPath() {
		return getProperty(DRUG_MODEL);
	}

	public static boolean isEvaluate() {
		return "true".equals(getProperty(EVALUATION));
	}

	public static boolean isDetailedEvaluation() {
		return "true".equals(getProperty(DETAILED_EVALUATION));
	}

	public static boolean useComponent(Component component) {
		String defaultValue = Arrays.asList(DEFAULT_DEACTIVATED).contains(component) ? "false" : "true";
		return "true".equals(getProperty(COMPONENT_PREFIX + component.toString().toLowerCase(), defaultValue).toLowerCase().trim());
	}
	
	public static boolean isAnnotate(ChemicalType type) {
		String defaultValue = Arrays.asList(DEFAULT_DEACTIVATED_ANNOTATIONS).contains(type) ? "false" : "true";
		return "true".equals(getProperty(ANNOTATIONS_PREFIX + type.toString().toLowerCase(), defaultValue).toLowerCase().trim());
	}
	
	public static boolean isAnnotateEumed(ChemicalType type) {
		String defaultValue = Arrays.asList(DEFAULT_DEACTIVATED_ANNOTATIONS_EUMED).contains(type) ? "false" : "true";
		return isAnnotate(type) && "true".equals(getProperty(ANNOTATIONS_PREFIX_EUMED + type.toString().toLowerCase(), defaultValue).toLowerCase().trim());
	}
	
	public static boolean initializeDictionaryFromNormalizer() {
		return "true".equals(getProperty(DICTIONARY_INITIALIZE_FROM_NORMALIZER, "false").toLowerCase());
	}
	
	public static int getDictionaryFilterLength() {
		return Integer.parseInt(getProperty(DICTIONARY_FILTER_LENGTH, "-1").toLowerCase());
	}

	public static boolean isThreading() {
		return "true".equals(getProperty(THREADING));
	}

	public static int getNumberOfThreads() {
		try {
			return Integer.valueOf(getProperty(THREAD_NR, DEFAULT_THREAD_NR + ""));
		} catch (NumberFormatException e) {
			System.out.println("ERROR: value of property '" + THREAD_NR + "' is not a number. Using defalut value " + DEFAULT_THREAD_NR);
			return DEFAULT_THREAD_NR;
		}
	}
	
	public static boolean isUpdate(String s) {
		return "true".equals(getProperty(UPDATE_PREFIX + s.toLowerCase()));
	}
	
	public static URL getChEBISDFUpdateURL() {
		String urlString = getProperty(UPDATE_CHEBI_SDF_URL);
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			System.err.println("The ChEBI update URL '" + urlString + "' in your configuration file is not a valid url");
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean isChEBIUpdateMustContainFormula() {
		return "true".equals(getProperty(UPDATE_CHEBI_MUST_CONTAIN_FORMULA));
	}
	
	public static URL getPubChemSDFUpdateURL() {
		String urlString = getProperty(UPDATE_PUBCHEM_SDF_URL);
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			System.err.println("The PubChem update URL '" + urlString + "' in your configuration file is not a valid url");
			e.printStackTrace();
			return null;
		}
	}
	
	public static int getPubChemMaxLength() {
		return Integer.parseInt(getProperty(UPDATE_PUBCHEM_MAX_LENGTH, "40"));
	}
	
	public static boolean isRemoveTemporaryUpdateFiles() {
		return "true".equals(getProperty(UPDATE_REMOVE_TEMPORARY_FILES, "false"));
	}
}
