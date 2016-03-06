package experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import org.processmining.plugins.kutoolbox.utils.LogUtils;

public class Utils {
	public static final String KILL = "taskkill /F /IM ";

	public static void killProcess(String serviceName) {
		// No risks, no idea if really necessary
		try { Runtime.getRuntime().exec(KILL + serviceName.toLowerCase());
		} catch (IOException e) {}
		try { Runtime.getRuntime().exec(KILL + serviceName.toUpperCase());
		} catch (IOException e) {}
		try { Runtime.getRuntime().exec(KILL + serviceName.toLowerCase() + ".exe");
		} catch (IOException e) {}
		try { Runtime.getRuntime().exec(KILL + serviceName.toLowerCase() + ".EXE");
		} catch (IOException e) {}
		try { Runtime.getRuntime().exec(KILL + serviceName.toUpperCase() + ".exe");
		} catch (IOException e) {}
		try { Runtime.getRuntime().exec(KILL + serviceName.toUpperCase() + ".EXE");
		} catch (IOException e) {}
	}
	
	public static List<File> getDirectoryFiles(File dir) {
		return getDirectoryFiles(dir, null);
	}
	
	public static List<File> getDirectoryFiles(File dir, String pattern) {
		List<File> l = new ArrayList<File>();
		
		if (pattern == null) pattern = "*";
		Pattern r = Pattern.compile(pattern);
		
		for (File f : dir.listFiles()) {
			Matcher m = r.matcher(f.getName());
			if (m.find()) l.add(f);
		}
		
		return l;
	}
	
	public static XLog readLog(String inputLog) {
		File f = new File(inputLog);
		if (inputLog.toLowerCase().endsWith(".xes.gz")) {
			return ImportUtils.openXESGZ(f);
		} else if (inputLog.toLowerCase().endsWith(".xes")) {
			return ImportUtils.openXES(f);
		} else if (inputLog.toLowerCase().endsWith(".mxml.gz")) {
			return ImportUtils.openMXMLGZ(f);
		} else if (inputLog.toLowerCase().endsWith(".mxml")) {
			return ImportUtils.openMXML(f);
		}
		return null;
	}
	
	public static void makeNegative(XEvent event) {
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		event.getAttributes().put("negative:isnegative", 
				factory.createAttributeBoolean("negative:isnegative", true, null));
		String ol = XLifecycleExtension.instance().extractTransition(event);
		if (ol == null) ol = "";
		String lt = ol + "-rejected";
		event.getAttributes().put("lifecycle:transition",
				factory.createAttributeLiteral("lifecycle:transition", lt, 
						XLifecycleExtension.instance()));
	}
	
	public static String traceToString(XTrace trace, XEventClassifier cl) {
		String tt = ""; 
		for (XEvent event : trace)
			tt += cl.getClassIdentity(event)+"____";
		return tt;
	}
	
	public static XLog mergeFolds(File logsdir, File currentFold, String pattern) {
		String baseName = currentFold.getName().split("\\.")[0];
		XLog merged = LogUtils.newLog(" -- positive folds except " + currentFold.getName());
		for (File otherpos : Utils.getDirectoryFiles(logsdir, pattern)) {
			if (otherpos.getName().equals(currentFold.getName())) continue;
			if (!otherpos.getName().startsWith(baseName)) continue;
			XLog other = Utils.readLog(otherpos.getAbsolutePath());
			merged.addAll(other);
		}
		return merged;
	}
	
	public static String getExtension(String path) {
		String file = path.lastIndexOf("/") == -1 ? path : path.substring(path.lastIndexOf("/"));
		return file.substring(file.lastIndexOf("."));
	}
	
	public static String replaceExtension(String path, String replacement) {
		return path.replace(getExtension(path), "."+replacement);
	}

	public static void writeLineToFile(File file, String line) {
		try {
			PrintStream pos = new PrintStream(new FileOutputStream(file, true));
			pos.println(line);
			pos.close();
		} catch (FileNotFoundException e) {
			
		}
	}
}
