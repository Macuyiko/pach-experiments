package statistics;
import java.io.File;
import java.io.IOException;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.kutoolbox.exceptions.OperationCancelledException;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;

import experiment.Globals;
import experiment.Utils;

public class LogStats {
	public static void main(final String... args) throws OperationCancelledException, IOException {
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryNaiveImpl());
		
		for (File file : Utils.getDirectoryFiles(new File(Globals.neglogsdir), "\\.xes$")) {
			
			System.out.println(file.getName());
			XLog log = ImportUtils.openLog(file);
			XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
			System.out.println(" - traces : "+logInfo.getNumberOfTraces());
			System.out.println(" - events : "+logInfo.getNumberOfEvents());
			System.out.println(" - classes: "+logInfo.getEventClasses().size());
		}
	}
}
