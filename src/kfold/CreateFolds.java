package kfold;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.kutoolbox.utils.ExportUtils;
import org.processmining.plugins.kutoolbox.utils.LogUtils;
import org.processmining.xeslite.external.XFactoryExternalStore;

import experiment.DirectoryExperiment;
import experiment.Globals;
import experiment.Utils;

public class CreateFolds extends DirectoryExperiment {
	public CreateFolds(File directory, String pattern, File outputDirectory, boolean skipIfExists) {
		super(directory, pattern, outputDirectory, skipIfExists);
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryExternalStore.MapDBDiskImpl());
		outputDirectory.mkdirs();
	}

	@Override
	public void run(File file, File outputDirectory, File outputTxt) {
		int folds = Globals.NRFOLDS;
		
		
		Random r = new Random();
		String poslog = file.getAbsolutePath();
		String neglog = Globals.neglogsdir + file.getName();
		XLog posxlog = Utils.readLog(poslog);
		XLog negxlog = Utils.readLog(neglog);
		int posperfold = (int) Math.floor((double) posxlog.size() / (double) folds);
		int negperfold = (int) Math.floor((double) negxlog.size() / (double) folds);
		System.out.format(" Positive log size: %d -> size per fold: %d\n", posxlog.size(), posperfold);
		System.out.format(" Negative log size: %d -> size per fold: %d\n", negxlog.size(), negperfold);
		
		for (int fnr = 0; fnr < folds; fnr++) {
			System.out.format("  Doing fold 1+%d / %d\n", fnr, folds);
			XLog posxlogf = LogUtils.newLog(file.getName() + " -- positive fold " + fnr);
			XLog negxlogf = LogUtils.newLog(file.getName() + " -- negative fold " + fnr);
			for (int tnr = 0; tnr < posperfold; tnr++) {
				XTrace postrace = posxlog.remove(r.nextInt(posxlog.size()));
				posxlogf.add(postrace);
			}
			for (int tnr = 0; tnr < negperfold; tnr++) {
				XTrace negtrace = negxlog.remove(r.nextInt(negxlog.size()));
				negxlogf.add(negtrace);
			}
			if (fnr == folds-1) {
				posxlogf.addAll(posxlog);
				negxlogf.addAll(negxlog);
			}
			
			try {
				ExportUtils.exportLog(posxlogf, new File(outputDirectory.getAbsolutePath() + "/" +
						Utils.replaceExtension(file.getName(), "pos."+fnr+".xes")));
				ExportUtils.exportLog(negxlogf, new File(outputDirectory.getAbsolutePath() + "/" +
						Utils.replaceExtension(file.getName(), "neg."+fnr+".xes")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

	public static void main(final String... args) throws Exception {
		DirectoryExperiment miner = new CreateFolds(
				new File(Globals.poslogsdir),
				"\\.xes$", 
				new File(Globals.basedir + "kfold/logs/"), 
				true);
		miner.go();
		System.out.println("Experiment finished ---------------");
	}
}
