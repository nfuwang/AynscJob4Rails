package com.fnst.tool.logutil;

/*
 * logger for aynsc job
 */
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.fnst.tool.SystemParams;

public class LogFactory {

	static private final String PACKAGE = LogFactory.class.getPackage()
			.getName();

	static private final String DEFAULT_CONFIG = "logserver.config";

	static Properties p = null;

	
	/**
	 * Interface called by another process
	 * @param args Arguments
	 */
	public void log(String[] args) {
		if(args.length == 5){
			this.log(args[0], args[1], args[2], args[3], args[4]);
		}
	}

	/**
	 * Logging
	 * 
	 * @param id ID for logger
	 * @param level Level
	 * @param classname Class name
	 * @param sessionID Session ID
	 * @param message Message
	 */
	public void log(String id, String level, String classname,
			String sessionID, String message) {
		AynscLogger logger = LogFactory.getPackageLogger(id);
		//To improve performance
		String[] data = {level, classname, sessionID, message};
		logger.putQueue(data);
	}

	/**
	 * Get the logger by package name.
	 * 
	 * @param id
	 *            ID for logger
	 * @return Logger
	 */
	static private synchronized AynscLogger getPackageLogger(String id) {
		return getLogger(PACKAGE + "." + id);
	}

	/**
	 * Get or create(if there's no) the logger.
	 * 
	 * @param id
	 *            ID for logger
	 * @return Logger
	 */
	static public synchronized AynscLogger getLogger(String id){
		LogManager logmanager = LogManager.getLogManager();
		Logger logger = logmanager.getLogger(id);
		if (logger == null) {
			logger = new AynscLogger(id, null);
			setProperties(logger);
			logmanager.addLogger(logger);
			logger = logmanager.getLogger(id);
		}
		return (AynscLogger)logger;
	}

	static private final Level DEFAULT_LEVEL;

	static private final int DEFAULT_LIMIT ;

	static private final int DEFAULT_COUNT;
	
	static private final String DEFAULT_PATTERN;
	
	static private final String DEFAULT_DIR;
	
	static{
		Properties prop = getProperties();
		DEFAULT_LIMIT = getPositiveIntPropery(prop, PACKAGE + ".limit", 1024 * 1000);
		DEFAULT_COUNT = getPositiveIntPropery(prop, PACKAGE + ".count", 3);
		DEFAULT_LEVEL = Level.parse(prop.getProperty(PACKAGE + ".level", "INFO").toUpperCase());
		DEFAULT_PATTERN = prop.getProperty(PACKAGE + ".pattern");
		DEFAULT_DIR = prop.getProperty(PACKAGE + ".dir", 
				System.getProperty("rcx.mgrPathName","") + "/var/log/");
	}
	
	static HashMap fileHandlerMap = new HashMap();

	/**
	 * Set the properties for logger.
	 * 
	 * @param logger Logger
	 */
	static private void setProperties(Logger logger) {
		String id = logger.getName();
		logger.setUseParentHandlers(false);
		Properties prop = getProperties();
		String logHandler = prop.getProperty(id + ".handler");
		String _loggerLevel = prop.getProperty(id + ".level",	DEFAULT_LEVEL.getName().toUpperCase());
		Level loggerLevel = Level.parse(_loggerLevel.toUpperCase());
		logger.setLevel(loggerLevel);
		if (logHandler == null) {
			ConsoleHandler handler = new ConsoleHandler();
			handler.setFormatter(new LogFormatter());
			handler.setLevel(DEFAULT_LEVEL);
			logger.addHandler(handler);
		} else {
			String[] logHandlers = logHandler.split(",");
			for(int i = 0; i < logHandlers.length; i++){
				String handlerId = id + "." + logHandlers[i];
				String pattern = prop.getProperty(handlerId + ".pattern", DEFAULT_PATTERN);
				if(pattern == null){
					continue;
				}
				int limit = getPositiveIntPropery(prop, handlerId + ".limit",
						DEFAULT_LIMIT);
				int count = getPositiveIntPropery(prop, handlerId + ".count",
						DEFAULT_COUNT);
				String _level = prop.getProperty(handlerId + ".level",
						DEFAULT_LEVEL.getName().toUpperCase());
				Level level = Level.parse(_level.toUpperCase());
				String dir = prop.getProperty(handlerId + ".dir", DEFAULT_DIR);
				if (!dir.equals("") && !dir.endsWith("/")) {
					dir = dir.concat("/");
				}
				String filepath = dir + pattern;
				FileHandler fileHandler;
				try {
					fileHandler = (FileHandler)fileHandlerMap.get(filepath);
					if(fileHandler == null){
						fileHandler = new FileHandler(filepath, limit, count, true);
						fileHandler.setLevel(level);
						fileHandler.setFormatter(new LogFormatter());
						fileHandlerMap.put(filepath, fileHandler);
					}
					logger.addHandler(fileHandler);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Get the property in positive int value.
	 * @param prop Properties
 	 * @param key Key of propery
	 * @param defaultValue Defaul value
	 * @return positive int value
	 */
	static private int getPositiveIntPropery(Properties prop, String key, int defaultValue) {
		String value = prop.getProperty(key);
		if (value != null) {
			try {
				int i = Integer.parseInt(value);
				return i > 0 ? i : defaultValue;
			} catch (NumberFormatException e) {
			}
		}
		return defaultValue;
	}

	/**
	 * Getter for properties.
	 * 
	 * @return Properties
	 */
	static private synchronized Properties getProperties() {
		if (p == null) {
			p = new Properties();
			String filename = SystemParams.LOG_CONFIG_PATH+ DEFAULT_CONFIG;

			BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(new FileInputStream(filename));
				p.load(bis);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return p;
	}
}
