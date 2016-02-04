package negativelogs;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlSerializer;
import org.processmining.plugins.kutoolbox.groupedlog.GroupedXLog;
import org.processmining.plugins.kutoolbox.utils.LogUtils;
import org.processmining.plugins.neconformance.negativeevents.AbstractNegativeEventInducer;
import org.processmining.plugins.neconformance.negativeevents.impl.LogTreeWeightedNegativeEventInducer;
import org.processmining.plugins.neconformance.trees.ukkonen.UkkonenLogTree;
import org.processmining.plugins.neconformance.types.WeightedEventClass;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.Globals;
import experiment.Utils;

public class MakeNegativeLogs {
	public String inputLog, outputLog;
	public int outputFormat = 2; // 0: traces with negev, 1: pos and neg traces, 2: neg traces
	public boolean onlyUnique = true;
	public boolean randomExtend = true;
	public double inclusionChance = 1.0d;
	public double ratioToPos = 1.0d;
	public XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
	public boolean useWeighted = false;
	public boolean useCut = false;
	public int winSize = 1;
	public double weightThreshold = 0.0;
	
	private Random r = new Random();
	private XLog log;
	private XEventClasses eventClasses;
	private GroupedXLog glog;
	private Set<XEventClass> startingClasses;
	private UkkonenLogTree logTree;
	private LogTreeWeightedNegativeEventInducer inducer;

	public static void main(final String... args) throws Exception {
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		
		for (File file : Utils.getDirectoryFiles(new File(Globals.poslogsdir), "\\.xes$")) {
			System.out.println(file.getName());
			MakeNegativeLogs ind = new MakeNegativeLogs();
			ind.inputLog = file.getAbsolutePath();
			ind.outputLog = Globals.neglogsdir + file.getName();
			ind.onlyUnique = true;
			ind.outputFormat = 2;
			ind.useWeighted = true;
			ind.useCut = true;
			ind.randomExtend = true;
			ind.winSize = -1;
			ind.weightThreshold = .8D;
			ind.run();
		}
	}
	
	public void run() throws Exception {
		setup();
		induce();
	}

	protected void setup() {
		System.out.println("Reading log...");
		log = Utils.readLog(inputLog);
		eventClasses = XEventClasses.deriveEventClasses(classifier, log);
		glog = new GroupedXLog(log, classifier, false);
		startingClasses = AbstractNegativeEventInducer.deriveStartingClasses(eventClasses, log);	
		
		System.out.println("Building tree...");
		logTree = new UkkonenLogTree(glog.getGroupedLog());
		inducer = new LogTreeWeightedNegativeEventInducer(eventClasses, startingClasses, logTree);
		inducer.setReturnZeroEvents(false);
		inducer.setUseWeighted(useWeighted);
		inducer.setNegWindowSize(winSize);
		inducer.setUseWindowOccurrenceCut(useCut);
	}
	
	private void induce() throws Exception {
		XLog outputLog = XFactoryRegistry.instance().currentDefault().createLog();
		outputLog.clear();
		
		System.out.println("Inducing negatives...");
		
		for (int tn = 0; tn < log.size(); tn++) {
			if (tn % 100 == 0)
				System.out.println("Trace nr. "+(tn+1)+" / "+log.size());
			XTrace trace = log.get(tn);
			List<Set<WeightedEventClass>> negatives = new ArrayList<>();
			for (int position = 0; position < trace.size(); position++) {
				Set<WeightedEventClass> negativesAtPos = getNegativeEvents(trace, position);
				negatives.add(negativesAtPos);
				if (outputFormat > 0) {
					for (WeightedEventClass negative : negativesAtPos) {
						XTrace t = constructSingleNegativeTrace(trace, 0, position, negative, classifier);
						if (randomExtend) {
							int i = t.size();
							for (int j = 0; j < i; j++) {
								List<XEventClass> list = Arrays.asList(eventClasses.getClasses().toArray(new XEventClass[0]));
								XEventClass result = list.get(r.nextInt(list.size()));
								t.add(LogUtils.deriveEventFromClassIdentity(result.getId(), classifier, "\\+"));
							}
						}
						if (r.nextDouble() <= inclusionChance) outputLog.add(t);
					}
				}
			}
			if (outputFormat == 0) {
				XTrace t = constructFullNegativeTrace(trace, negatives, classifier);
				if (r.nextDouble() <= inclusionChance) outputLog.add(t);
			} else if (outputFormat == 1) {
				if (r.nextDouble() <= inclusionChance) outputLog.add(tn, trace);
			}
		}
		
		System.out.println("Saving file...");
		
		if (onlyUnique) {
			System.out.println("Making uniques...");
			outputLog = new GroupedXLog(outputLog).getGroupedLog();
		}
		
		if (ratioToPos >= 0d) {
			double desired = (double) log.size() * ratioToPos;
			System.out.println("Shrinking from " + outputLog.size() + " to "+desired);
			while (outputLog.size() > desired) {
				outputLog.remove(r.nextInt(outputLog.size()));
			}
		}
		
		System.out.println("Saving " + outputLog.size() + " traces");
		
		if (this.outputLog.toLowerCase().endsWith(".sim")) {
			Files.write(Paths.get(this.outputLog), new byte[]{}, 
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			for (XTrace trace : outputLog) {
				String line = "";
				for (XEvent event : trace) {
					line += " "+XConceptExtension.instance().extractName(event);
					if (event.getAttributes().containsKey("negative:isnegative"))
						line += "-";
				}
				line = line.substring(1)+"\n";
				Files.write(Paths.get(this.outputLog), line.getBytes(), StandardOpenOption.APPEND);
			}
			
		} else if (this.outputLog.toLowerCase().endsWith(".xes")) {
			XSerializer ser = new XesXmlSerializer();
			ser.serialize(outputLog, new FileOutputStream(this.outputLog));
		}
			
		System.out.println("Done!");
	}

	private Set<WeightedEventClass> getNegativeEvents(XTrace trace, int position) {
		Set<WeightedEventClass> negativesAtPos = inducer.getNegativeEvents(trace, position);
		Iterator<WeightedEventClass> it = negativesAtPos.iterator();
		while (it.hasNext()) {
			WeightedEventClass nec = it.next();
			if (nec.weight < weightThreshold)
				it.remove();
		}
		return negativesAtPos;
	}
	
	public static XTrace constructSingleNegativeTrace(XTrace trace, int start, int end, WeightedEventClass negative, XEventClassifier cl) {
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XTrace newTrace = factory.createTrace((XAttributeMap) trace.getAttributes().clone());
		String ol = XConceptExtension.instance().extractName(newTrace);
		if (ol == null) ol = "";
		String lt = ol + "-"+end+"-"+negative.eventClass.getId();
		XConceptExtension.instance().assignName(newTrace, lt);
		for (int realPosition = start; realPosition < end; realPosition++) {
			newTrace.add(factory.createEvent(trace.get(realPosition).getAttributes()));
		}
		XEvent ne = LogUtils.deriveEventFromClassIdentity(negative.eventClass.getId(), cl, "\\+");
		Utils.makeNegative(ne);
		newTrace.add(ne);
		return newTrace;
	}
	
	public static XTrace constructFullNegativeTrace(XTrace trace, List<Set<WeightedEventClass>> negatives, XEventClassifier cl) {
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XTrace newTrace = factory.createTrace(trace.getAttributes());
		for (int realPosition = 0; realPosition < trace.size(); realPosition++) {
			for (WeightedEventClass neg : negatives.get(realPosition)) {
				XEvent ne = LogUtils.deriveEventFromClassIdentity(neg.eventClass.getId(), cl, "\\+");
				Utils.makeNegative(ne);
				newTrace.add(ne);
			}
			newTrace.add(factory.createEvent(trace.get(realPosition).getAttributes()));
		}
		return newTrace;
	}
	
	
}
