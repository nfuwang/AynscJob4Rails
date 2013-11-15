package com.fnst.tool.logutil;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 */
public class LogFormatter extends SimpleFormatter {
	/** Date format */
	static private String date_format = "yyyy-MM-dd HH:mm:ss.SSS ";//$NON-NLS-1$

	/** Line separator */
	static private final String line_separator = System.getProperty("line.separator");//$NON-NLS-1$

	/**
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public synchronized String format(LogRecord rec) {
		StringBuffer sb = new StringBuffer(100);
		sb.append(new SimpleDateFormat(date_format).format(new Date(rec.getMillis())));
		
		sb.append(rec.getSourceClassName());
		sb.append("[");
		// Use mehhod name for session id.
		sb.append(rec.getSourceMethodName());
		sb.append("]");
		
		sb.append(":");
		sb.append(rec.getLevel());
		sb.append(":");
		
		sb.append(rec.getMessage());
		sb.append(line_separator);
		Throwable t;
		if ((t = rec.getThrown()) != null) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
		}
		return new String(sb);
	}
}
