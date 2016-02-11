package loggenerator;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlSerializer;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.kutoolbox.groupedlog.GroupedXLog;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.Globals;
import experiment.Utils;

public class MakeLogs {
	public String inputNet, outputLog;
	public int nrTraces;
	private Petrinet net;
	private Marking marking;
	private boolean onlyUnique = false;
	private Random r = new Random();
	
	public static void main(final String... args) throws Exception {
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		
		for (File file : Utils.getDirectoryFiles(new File(Globals.basedir+"runningManufacturingExample\\nets\\"), "\\.pnml$")) {
			System.out.println(file.getName());	
			for (int nrTraces : new int[]{10, 100, 500, 1000, 2000, 5000, 10000}) {
				MakeLogs ind = new MakeLogs();
				ind.inputNet = file.getAbsolutePath();
				ind.outputLog = Globals.basedir+"runningManufacturingExample\\logs\\" + 
						file.getName().replace(".pnml", "-"+nrTraces+".xes");
				ind.nrTraces = nrTraces;
				ind.run();
			}
		}
	}
	
	public void run() throws Exception {
		setup();
		generate();
	}

	protected void setup() {
		System.out.println("Reading net...");
		Object[] petrinet = ImportUtils.openPetrinet(new File(inputNet));
		net = (Petrinet) petrinet[0];
		marking = (Marking) petrinet[1];
	}
	
	private void generate() throws Exception {
		XLog outputLog = XFactoryRegistry.instance().currentDefault().createLog();
		outputLog.clear();
		
		System.out.println("Generating positives...");
		
		for (int tn = 0; tn < nrTraces; tn++) {
			XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
			XConceptExtension.instance().assignName(trace, "trace_"+tn);
			PetrinetSemantics semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
			semantics.initialize(net.getTransitions(), marking);
			while (true) {
				List<Transition> possible = new ArrayList<Transition>(semantics.getExecutableTransitions());
				if (possible.size() == 0) break;
				Transition t = possible.get(r.nextInt(possible.size()));
				semantics.executeExecutableTransition(t);
				XEvent event = XFactoryRegistry.instance().currentDefault().createEvent();
				XConceptExtension.instance().assignName(event, t.getLabel());
				trace.add(event);
				if (trace.size() > 10 && r.nextFloat() > .95F) break;
			}
			if (tn % 1000 == 0)
				System.out.println("Trace nr. "+(tn+1)+" / "+nrTraces+" -- "+trace.size());
			outputLog.add(trace);
		}
		
		System.out.println("Saving file...");
		
		if (onlyUnique) {
			System.out.println("Making uniques...");
			outputLog = new GroupedXLog(outputLog).getGroupedLog();
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
	
	
}
