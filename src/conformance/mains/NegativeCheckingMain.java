package conformance.mains;
import java.io.File;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.kutoolbox.logmappers.PetrinetLogMapper;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import org.processmining.plugins.neconformance.metrics.impl.BehavioralRecallMetric;
import org.processmining.plugins.neconformance.metrics.impl.BehavioralWeightedGeneralizationMetric;
import org.processmining.plugins.neconformance.metrics.impl.BehavioralWeightedPrecisionMetric;
import org.processmining.plugins.neconformance.models.impl.PetrinetReplayModel;
import org.processmining.plugins.neconformance.negativeevents.AbstractNegativeEventInducer;
import org.processmining.plugins.neconformance.negativeevents.impl.LogTreeWeightedNegativeEventInducer;
import org.processmining.plugins.neconformance.trees.LogTree;
import org.processmining.plugins.neconformance.trees.ukkonen.UkkonenLogTree;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.Utils;

public class NegativeCheckingMain {
	
	public static void main(String...args) {
		File logDirectory = new File(args[0]);
		File outputTxt = new File(args[1]);
		File pnml = new File(args[2]);
		
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		
		System.out.print(pnml.getName() + "\t");
	
		File logFile = new File(logDirectory.getAbsolutePath() + "/" +
				Utils.replaceExtension(
						pnml.getName()
						.replace(".enc", "")
						.replace("allbut_", "")
						.replace("_no_smt", "")
						.replace("_smt_iter", "")
						.replace("_smt_matrix", ""), "xes"));
		
		if (!logFile.exists()) {
			System.err.println("NOT FOUND (1): "+logFile.getName());
			logFile = new File(logFile.getAbsolutePath().replace(".xes", ".enc.xes"));
			System.err.println("CHANGED: "+logFile.getName());
		}
			
		if (!logFile.exists()) {
			System.err.println("NOT FOUND (2): "+logFile.getName());
			return;
		}
			
		XLog log = ImportUtils.openLog(logFile);
		Object[] i = ImportUtils.openPetrinet(pnml);
		Petrinet net = (Petrinet) i[0];
		Marking initialMarking = (Marking) i[1];
					
		LogTree logTree = new UkkonenLogTree(log);
		XEventClasses eventClasses = XEventClasses.deriveEventClasses(XLogInfoImpl.STANDARD_CLASSIFIER, log);
		LogTreeWeightedNegativeEventInducer inducer = new LogTreeWeightedNegativeEventInducer(
				eventClasses,
				AbstractNegativeEventInducer.deriveStartingClasses(eventClasses, log),
				logTree);
		inducer.setReturnZeroEvents(false);
		inducer.setUseBothWindowRatios(false);
		inducer.setUseWeighted(true);
		
		PetrinetLogMapper mapper = PetrinetLogMapper.getStandardMap(log, net);
		mapper.applyMappingOnTransitions();
		
		double[] result = new double[]{-1,-1,-1};
		PetrinetReplayModel replayModelN = new PetrinetReplayModel(net, initialMarking, mapper);
		//AryaPetrinetReplayModel replayModelA = new AryaPetrinetReplayModel(net, eventClasses, initialMarking, mapper);
		//RecoveringPetrinetReplayModel replayModelR = new RecoveringPetrinetReplayModel(net, initialMarking, mapper);
			
		//doChecking(replayModelN, inducer, log, true, "normal", pos);
		//doChecking(replayModelA, inducer, log, true, "arya", pos);
		//doChecking(replayModelR, inducer, log, true, "recovery", pos);
		try {
			result = doChecking(replayModelN, inducer, log, false, "normal");
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			System.exit(0);
		}
		//doChecking(replayModelA, inducer, log, false, "arya", pos);
		//doChecking(replayModelR, inducer, log, false, "recovery", pos);
		
		System.out.println(result[0]+" "+result[1]+" "+result[2]);
		Utils.writeLineToFile(outputTxt, pnml.getName()+"\t"+result[0]+"\t"+result[1]+"\t"+result[2]);
		System.exit(0);
	}
	
	public static double[] doChecking(PetrinetReplayModel replayModel, LogTreeWeightedNegativeEventInducer inducer,
			XLog log, boolean punishUnmapped, String type) {
		BehavioralRecallMetric br = new BehavioralRecallMetric(replayModel, inducer, log, punishUnmapped, true);
		BehavioralWeightedPrecisionMetric bp = new BehavioralWeightedPrecisionMetric(replayModel, inducer, log, punishUnmapped, true);
		BehavioralWeightedGeneralizationMetric bg = new BehavioralWeightedGeneralizationMetric(replayModel, inducer, log, punishUnmapped, true);
		return new double[]{br.getValue(), bp.getValue(), bg.getValue()};
	}
	
	
}
