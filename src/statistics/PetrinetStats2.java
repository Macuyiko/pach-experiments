package statistics;
import java.io.File;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.initialfinalmarking.InitialMarkingPlugin;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import experiment.Utils;

public class PetrinetStats2 {
	public static void main(final String... args) throws Exception {
		
		for (File file : Utils.getDirectoryFiles(new File("C:\\Users\\u0078555\\Desktop\\ExampleBookWil"), "\\.tpn$")) {
			System.out.println(file.getName());
			Petrinet net = ImportUtils.openTPN(file);
			Marking m = InitialMarkingPlugin.create(null, net);
			ExportUtils.exportPetriNet(net, m, new File(file.getAbsolutePath().replace(".tpn", ".pnml")));
		}
	}
	
	
	
}
