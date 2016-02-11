package negativelogs;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Utils;

public class MakeNegativeLogs extends DirectoryExperiment {
	public MakeNegativeLogs(File directory, String pattern, File outputDirectory, boolean skipIfExists) {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
	}

	@Override
	public void run(File file, File outputDirectory, File outputTxt) {
		String outputLogPath = outputDirectory.getAbsolutePath() + "/" + Utils.replaceExtension(file.getName(), "xes");
		
		int outputFormat = 2;
		boolean onlyUnique = true;
		boolean randomExtend = true;
		double inclusionChance = 1.0d;
		double ratioToPos = 1.0d;
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		boolean useWeighted = true;
		boolean useCut = true;
		int winSize = -1;
		double weightThreshold = 0.8;

		
		Random r = new Random();
		
		System.out.println("Reading log...");
		XLog log = Utils.readLog(file.getAbsolutePath());
		XEventClasses eventClasses = XEventClasses.deriveEventClasses(classifier, log);
		GroupedXLog glog = new GroupedXLog(log, classifier, false);
		Set<XEventClass> startingClasses = AbstractNegativeEventInducer.deriveStartingClasses(eventClasses, log);	
		
		System.out.println("Building tree...");
		UkkonenLogTree logTree = new UkkonenLogTree(glog.getGroupedLog());
		LogTreeWeightedNegativeEventInducer inducer = new LogTreeWeightedNegativeEventInducer(eventClasses, startingClasses, logTree);
		inducer.setReturnZeroEvents(false);
		inducer.setUseWeighted(useWeighted);
		inducer.setNegWindowSize(winSize);
		inducer.setUseWindowOccurrenceCut(useCut);
	
		
		System.out.println("Inducing negatives...");
		XLog outputLog = XFactoryRegistry.instance().currentDefault().createLog();
		outputLog.clear();
		
		for (int tn = 0; tn < log.size(); tn++) {
			if (tn % 100 == 0)
				System.out.println("Trace nr. "+(tn+1)+" / "+log.size());
			XTrace trace = log.get(tn);
			List<Set<WeightedEventClass>> negatives = new ArrayList<>();
			for (int position = 0; position < trace.size(); position++) {
				Set<WeightedEventClass> negativesAtPos = getNegativeEvents(trace, position, inducer, weightThreshold);
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
		XSerializer ser = new XesXmlSerializer();
		try {
			ser.serialize(outputLog, new FileOutputStream(outputLogPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}

	private Set<WeightedEventClass> getNegativeEvents(XTrace trace, int position,
			LogTreeWeightedNegativeEventInducer inducer,
			double weightThreshold) {
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
	
	public static void main(final String... args) throws Exception {
		DirectoryExperiment miner = new MakeNegativeLogs(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.neglogsdir), 
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
}
