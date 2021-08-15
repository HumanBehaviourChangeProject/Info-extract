package com.ibm.drl.hbcp.util;

import com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.ibm.drl.hbcp.api.ExtractorController;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Methods to work with environment variables.
 * @author ptommasi
 */
public class Environment {

	private static final Logger logger = LoggerFactory.getLogger(ExtractorController.class);

	private static String __DEFAULT_FLAIR_URL__  = "127.0.0.1";
	private static int    __DEFAULT_FLAIR_PORT__ = 5000;
	private static String __DEFAULT_PREDICTION_URL__ = "127.0.0.1";
	private static int __DEFAULT_PREDICTION_PORT__ = 5001;


	public static String getFlairURL() {
		String env_value = System.getenv("FLAIR_URL");
		return env_value != null ? env_value : __DEFAULT_FLAIR_URL__;
	}

	public static int getFlairPort() {

		String env_value = System.getenv("FLAIR_PORT");

		if (env_value != null) {
			try {
				return Integer.parseInt(env_value);
			} catch (Exception e) {
				logger.warn("FLAIR_PORT environment variable found, but impossible to parse it, falling back on default port.", e);
			}
		}

		return __DEFAULT_FLAIR_PORT__;
	}

	public static String getPredictionURL() {
		String env_value = System.getenv("PREDICTION_URL");
		return env_value != null ? env_value : __DEFAULT_PREDICTION_URL__;
	}

	public static int getPredictionPort() {
		String env_value = System.getenv("PREDICTION_PORT");
		if (env_value != null) {
			try {
				return Integer.parseInt(env_value);
			} catch (Exception e) {
				logger.warn("PREDICTION_PORT environment variable found, but impossible to parse it, falling back on default port.", e);
			}
		}
		return __DEFAULT_PREDICTION_PORT__;
	}

	/** To set to true in a docker-compose setting, this will ignore paper indexing/extraction for the prediction endpoints */
	public static boolean isPredictionApiOnly() {
		String env_value = System.getenv("PREDICTION_API_ONLY");
		return Boolean.parseBoolean(env_value);
	}

	/** Returns the list of JSON annotation files used to build and train the prediction system */
	public static File getAnnotationFileForPrediction() {
		String env_value = System.getenv("ENTITIES_FOR_PREDICTION");
		logger.debug("$ENTITIES_FOR_PREDICTION is: " + env_value);
		if (env_value != null) {
			// the file has been copied to the resources by the docker-compose
			File resourceFile = FileUtils.potentiallyGetAsResource(new File("data/jsons", new File(env_value).getName()));
			// as a fallback, use the raw path
			if (resourceFile.exists()) {
				return resourceFile;
			} else {
				return new File(env_value);
			}
		} else {
			// default to the smoking cessation file defined in the init.properties
			try {
				return FileUtils.potentiallyGetAsResource(new File(Props.loadProperties().getProperty("ref.json")));
			} catch (IOException e) {
				logger.error("Couldn't load main property file to read the annotation JSON's path: ", e);
				logger.error("HBCP-core will now crash.");
				throw new RuntimeException(e);
			}
		}
	}

	/** Returns the single JSON file used to initialize the singleton class Attributes, which represents the ontology
	 * and codesets used everywhere in the API endpoints */
	public static File getAnnotationFileForAttributeDefinition() {
		String env_value = System.getenv("CODESET_JSON");
		if (env_value != null) {
			return new File(env_value);
		} else {
			// if a value is not specified through the bespoke property, use the annotation file defined for prediction
			// this at least maintains coherency between the API and the prediction training as a default
			return getAnnotationFileForPrediction();
		}
	}

}
