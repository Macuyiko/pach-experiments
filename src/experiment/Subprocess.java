package experiment;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Subprocess {

	public static void startSecondJVM(final Class<?> cls,
			List<String> jvmArgs, List<String> cmdArgs) throws IOException, TimeoutException, InterruptedException {
		startSecondJVM(cls, jvmArgs, cmdArgs, 600000);
	}
	
	public static void startSecondJVM(final Class<?> cls,
			List<String> jvmArgs, List<String> cmdArgs, long millis) throws IOException, TimeoutException, InterruptedException {
		String separator = System.getProperty("file.separator");
		String classpath = System.getProperty("java.class.path");
		String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
		final List<String> cmd = new ArrayList<>();
		cmd.add(path);
		cmd.addAll(jvmArgs);
		cmd.add("-cp");
		cmd.add(classpath);
		cmd.add(cls.getName());
		cmd.addAll(cmdArgs);
		ProcessBuilder processBuilder = new ProcessBuilder(cmd);
		processBuilder.inheritIO();
		Process process = processBuilder.start();
		waitFor(process, millis);
	}

	static Method findMain(final Class<?> cls) {
		for (final Method method : cls.getMethods()) {
			int mod = method.getModifiers();
			if (method.getName().equals("main") 
					&& Modifier.isPublic(mod) 
					&& Modifier.isStatic(mod) 
					&& method.getParameterTypes().length == 1 
					&& method.getParameterTypes()[0].equals(String[].class)) {
				return method;
			}
		}
		return null;
	}
	
	private static int waitFor(Process process, long millis) throws TimeoutException, InterruptedException {
		ProcessWorker worker = new ProcessWorker(process);
		worker.start();
		try {
			worker.join(millis);
			if (worker.exit != null)
				return worker.exit;
			else
				throw new TimeoutException();
		} catch (InterruptedException ex) {
			worker.interrupt();
			Thread.currentThread().interrupt();
			throw ex;
		} finally {
			process.destroy();
		}
	}
	
	
	
}