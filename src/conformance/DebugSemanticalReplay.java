package conformance;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.kutoolbox.exceptions.OperationCancelledException;
import org.processmining.plugins.kutoolbox.logmappers.PetrinetLogMapper;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;

import experiment.Globals;

public class DebugSemanticalReplay {
	private final static String modelfile = Globals.modelsdir + "/hybridIlpMod_positiveonly/a32.pnml";
	private final static String logfile = Globals.poslogsdir + "/a32.xes";
	
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
		boolean isok = true;
		String feedback = "";
		PetrinetSemantics semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		feedback += "Starting to work on trace: "+XConceptExtension.instance().extractName(trace);
		semantics.initialize(net.getTransitions(), initialMarking);
		for (XEvent e : trace) {
			feedback += "\nDoing event: "+XConceptExtension.instance().extractName(e);
			Collection<Transition> pt = mapper.getTransitionsForActivity(mapper.getEventClassifier().getClassIdentity(e));
			feedback += "\nTransitions possible for this event:";
			feedback += "\n"+pt.toString();
			Transition t = (Transition) pt.toArray()[0];
			feedback += "\nMarking before fire: ";
			feedback += "\n"+semantics.getCurrentState().toString();
			try {
				semantics.executeExecutableTransition(t);
			} catch (IllegalTransitionException e1) {
				feedback += "\nI need to stop here, transition was not enabled";
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> ed : net.getInEdges(t)) {
					Arc a = (Arc) ed;
					Place ps = (Place) ed.getSource();
					feedback += "\n - Input place: "+ps+" needs to supply "+a.getWeight()+
							" tokens, I see "+semantics.getCurrentState().occurrences(ps);
					if (a.getWeight() > semantics.getCurrentState().occurrences(ps))
						feedback += " *** PLACE IS MISSING TOKENS ***";
				}
				if (printWrong) System.out.println(feedback);
				isok = false;
				break;
			}
			feedback += "\nMarking after fire: ";
			feedback += "\n"+semantics.getCurrentState().toString();
		}
		return isok;
	}
	
}
