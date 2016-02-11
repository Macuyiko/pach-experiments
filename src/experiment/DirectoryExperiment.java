package experiment;

import java.io.File;

public abstract class DirectoryExperiment {
	private File directory;
	private String pattern;
	private File outputDirectory;
	private boolean skipIfExists;

	public DirectoryExperiment(File directory, String pattern, 
			File outputDirectory, boolean skipIfExists) {
		this.directory = directory;
		this.pattern = pattern;
		this.outputDirectory = outputDirectory;
		this.skipIfExists = skipIfExists;
	}
	
	public void go() {
		for (File file : Utils.getDirectoryFiles(directory, pattern)) {
			System.out.println(file.getName());
			String outputTxt = outputDirectory.getAbsolutePath() + "/" + Utils.replaceExtension(file.getName(), "txt");
			File outputTxtFile = new File(outputTxt);
			
			if (skipIfExists && outputTxtFile.exists()) {
				System.out.println("  SKIPPED: "+file.getAbsolutePath());
				continue;
			}
			outputTxtFile.delete();
			run(file, outputDirectory, outputTxtFile);
		}
	}
	
	public abstract void run(File file, File outputDirectory, File outputTxt);
}
