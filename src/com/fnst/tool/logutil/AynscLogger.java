package com.fnst.tool.logutil;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AynscLogger extends Logger{
	
	private volatile ArrayBlockingQueue<String[]> loggingQue = new ArrayBlockingQueue<String[]>(10000, true);
	
	/**
	 * Control the logging behavior
	 */
	private volatile boolean stopLogging = Boolean.FALSE.booleanValue();
	
	/**
	 * This thread retrieve data from the queue , <br/>
	 * call {@link java.util.logger.Logger#logp logp} and writes data to the log.
	 */
	public Thread logpCaller = null;   
	
	protected AynscLogger(String s, String s1) {
		super(s, s1);

		// add shutdownHook to stop the thread. this Thread is executed
		// in the following case
		// - send SIGNAL(-TERM) to JavaVM
		// - call Kernel.exit on RubyRuntime.
		Runtime.getRuntime().addShutdownHook(
				new Thread(Thread.currentThread(), "shutdownHook-"
						+ Thread.currentThread().getName()) {
					public void run() {
						flushLogging();
					}
				});
		this.logpCaller = new Thread(Thread.currentThread(), "logpcaller"
				+ Thread.currentThread().getId()) {

			public void run() {
				// loop
				while(Boolean.TRUE.booleanValue()){
					synchronized( AynscLogger.this ){
						if( isStopLogging() && AynscLogger.this.loggingQue.isEmpty()){
							break;
						}
					}
					writeLog();
				}
			}
		};
		this.logpCaller.setDaemon(Boolean.TRUE.booleanValue());
		this.logpCaller.start();
	}

	/**
	 * Indicates whether logging.
	 */
	public boolean isStopLogging() {
		return stopLogging;
	}

	/**
	 * Stop logging.
	 */
	public void stopLogging() {
		this.stopLogging = Boolean.TRUE.booleanValue();
	}
	
	/**
	 * Flush data to Log when logging is stopped.
	 */
	public void flushLogging(){
		try {
			stopLogging();
			//wait
			this.logpCaller.join(); 
		}catch(InterruptedException interrupt){
			//do nothing
		}finally{
			while(isStopLogging() && !loggingQue.isEmpty()){
				writeLog();
			}
		}
	}

	/**
	 * Adds data to the queue by calling
	 * {@link jata.util.concurrent.ArrayBlockingQueue#offer offer}.
	 * 
	 * @param offerdData
	 *            String[] elements must be below contents.
	 *            <ol>
	 *            <li> level
	 *            <li> class_name
	 *            <li> method_name
	 *            <li> message
	 *            </ol>
	 */
	public void putQueue(String[] offerdData) {
		if (offerdData == null || isStopLogging()) {
			return;
		}
		while (Boolean.TRUE.booleanValue()) {
			try {
				if (this.loggingQue
						.offer(offerdData, 5L, TimeUnit.MILLISECONDS)) {
					break;
				}
			} catch (Exception e) {
				// do nothing
			}
		}
	}

	/**
	 * Write log by calling {@link java.util.logger.Logger#logp logp}.
	 * Retreve data by calling
	 * {@link java.util.concurrent.ArrayBlockingQueue#poll poll} <br/>
	 * and call
	 * {@link com.AynscLogger.rcx.logserver.RcxLogger#writeLog writeLog}.
	 * @param polledData
	 *            String[] elements must be below contents.
	 *            <ol>
	 *            <li> level
	 *            <li> class_name
	 *            <li> method_name
	 *            <li> message
	 *            </ol>
	 */
	public void writeLog() {
		String[] polledData = null;
		try {
			polledData = this.loggingQue.poll(5L, TimeUnit.MILLISECONDS);
		} catch (InterruptedException interrupt) {
			error(interrupt.getMessage(), interrupt);
		} catch (Exception e) {
			error(e.getMessage(), e);
		}
		
		synchronized (this) {
			if(isStopLogging() || polledData == null){
				return;
			}
		}
		
		//call Logger#logp
		try {
			logp(Level.parse(polledData[0].toUpperCase()), polledData[1],
					polledData[2], polledData[3]);
		} catch (Exception e) {
			error(e.getMessage(), e);
		}
	}



	/** Error  */
	final static Level ERROR = new LevelError();

	/** Debug */
	final static Level DEBUG = new LevelDebug();

	/** Fatal */
	final static Level FATAL = new LevelFatal();
	
	/**
	 * Error level
	 */
	static class LevelError extends Level {
		static private final long serialVersionUID = 1L;

		LevelError() {
			super("ERROR", Level.WARNING.intValue() + 1);
		}
	}

	/**
	 * Debug level
	 */
	static class LevelDebug extends Level {
		static private final long serialVersionUID = 1L;

		LevelDebug() {
			super("DEBUG", Level.FINE.intValue());
		}
	}

	/**
	 * Fatal level
	 */
	static class LevelFatal extends Level {
		static private final long serialVersionUID = 1L;

		LevelFatal() {
			super("FATAL", Level.SEVERE.intValue());
		}
	}
	
	/**
	 * Debug log
	 * @param msg Message
	 * @param e Throwable
	 */
	public void debug(String msg, Throwable e) {
		this.log(DEBUG, msg, e);
	}

	/**
	 * Information log
	 * @param msg Message
	 * @param e Throwable
	 */
	public void info(String msg, Throwable e) {
		this.log(Level.INFO, msg, e);
	}
	
	/**
	 * Warning log
	 * @param msg Message
	 * @param e Throwable
	 */
	public void warning(String msg, Throwable e) {
		this.log(Level.WARNING, msg, e);
	}
	
	/**
	 * Error log
	 * @param msg Message
	 * @param e Throwable
	 */
	public void error(String msg, Throwable e) {
		this.log(ERROR, msg, e);
	}
	
	/**
	 * Fatal log
	 * @param msg Message
	 * @param e Throwable
	 */
	public void fatal(String msg, Throwable e) {
		this.log(FATAL, msg, e);
	}

	/**
	 * Debug log
	 * @param msg Message
	 */
	public void debug(String msg){
		this.log(DEBUG, msg);
	}

	/**
	 * Error log
	 * @param msg Message
	 */
	public void error(String msg){
		this.log(ERROR, msg);
	}
	
	/**
	 * Fatal log 
	 * @param msg Message
	 */
	public void fatal(String msg){
		this.log(FATAL, msg);
	}
	
	/**
	 * @see java.util.logging.Logger#log(java.util.logging.Level, java.lang.String)
	 */
	public void log(Level level, String msg) {
		if (isLoggable(level)) {
			Throwable dummyException = new Throwable();
			StackTraceElement locations[] = dummyException.getStackTrace();
			String cname = "unknown";//$NON-NLS-1$
			String method = "unknown";//$NON-NLS-1$
			if (locations != null && locations.length > 2) {
				StackTraceElement caller = locations[2];
				cname = caller.getClassName();
				method = caller.getMethodName();
			}
			logp(level, cname, method, msg);
		}
	}

	/**
	 * @see java.util.logging.Logger#log(java.util.logging.Level, java.lang.String)
	 */
	public void log(Level level, String msg, Throwable e) {
		if (isLoggable(level)) {
			Throwable dummyException = new Throwable();
			StackTraceElement locations[] = dummyException.getStackTrace();
			String cname = "unknown";//$NON-NLS-1$
			String method = "unknown";//$NON-NLS-1$
			if (locations != null && locations.length > 2) {
				StackTraceElement caller = locations[2];
				cname = caller.getClassName();
				method = caller.getMethodName();
			}
			logp(level, cname, method, msg, e);
		}
	}
}
