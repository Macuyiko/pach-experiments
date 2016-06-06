package conformance;
import java.io.File;
import java.io.IOException;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.kutoolbox.exceptions.OperationCancelledException;
import org.processmining.plugins.kutoolbox.logmappers.PetrinetLogMapper;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import org.processmining.plugins.yapetrinetreplayer.replayers.CompleteTraceReplayer;
import org.processmining.plugins.yapetrinetreplayer.types.ReplayStateChain;

import experiment.Globals;

public class DebugExhaustiveReplay {
	private final static String modelfile = Globals.modelsdir + "/with_negatives/ilp_uma-matrix/confdimblocking.pnml";
	private final static String logfile = Globals.poslogsdir + "/confdimblocking.xes";
	
	public static void main(final String... args) throws OperationCancelledException, IOException {
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryNaiveImpl());
		
		XLog log = ImportUtils.openLog(new File(logfile));
		Object[] i = ImportUtils.openPetrinet(new File(modelfile));
		Petrinet net = (Petrinet) i[0];
		Marking initialMarking = (Marking) i[1];
		PetrinetLogMapper mapper = PetrinetLogMapper.getStandardMap(log, net);
		mapper.applyMappingOnTransitions();
		
		System.out.println(modelfile);
		System.out.println(logfile);
		System.out.println(mapper.toString());
		
		boolean isok = true;
		for (XTrace trace : log) {
			isok = replayTrace(trace, net, initialMarking, mapper, true);
			if (!isok) break;
		}
		if (isok) System.out.println("Perfect log.");
	}
	
	public static boolean replayTrace(XTrace trace, Petrinet net, Marking initialMarking, 
			PetrinetLogMapper mapper, boolean printWrong) {
		CompleteTraceReplayer replayer;
		try {
			replayer = new CompleteTraceReplayer(net, mapper);
			replayer.resetReplayer(trace, initialMarking, false, false, false, true, -1, false, false);
			ReplayStateChain path;
			while ((path = replayer.getNextStatePath()) != null) {
				if (path.getCountForced() == 0) break;
			}
			boolean tracePossible = path != null;
			if (!tracePossible) {
				if (printWrong) 
					System.out.println(" Found not fitting trace: " +
							XConceptExtension.instance().extractName(trace));
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
