package com.ibm.drl.hbcp.util;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.ibm.drl.hbcp.api.ExtractorController;

import java.text.ParseException;

/**
 * Methods to work with environment variables.
 * @author ptommasi
 */
public class Environment {
	
	private static Logger logger = LoggerFactory.getLogger(ExtractorController.class);
	
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
		return env_value != null && Boolean.parseBoolean(env_value);
	}

}
