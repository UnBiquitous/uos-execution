package org.unbiquitous.driver.spike;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mockito.Mockito;
import org.unbiquitous.driver.execution.ExecutionDriver;
import org.unbiquitous.driver.execution.executeAgent.Agent;
import org.unbiquitous.uos.core.UOS;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Call.ServiceType;
import org.unbiquitous.uos.core.messageEngine.messages.Response;


public class FindClassSpike {
	private static final Logger logger = UOSLogging.getLogger();
	
	public static void main(String[] args) throws Exception {

		HelloAgent hello = new HelloAgent();
		hello.getClass().getResource(null);
		System.out.println(hello.getClass() + "\t>>\t" + findClass(hello));
		System.out.println(new ExecutionDriver().getClass() + "\t>>\t"
				+ findClass(new ExecutionDriver()));
		System.out.println(new UOS().getClass() + "\t>>\t"
				+ findClass(new UOS()));
		System.out.println(new Mockito().getClass() + "\t>>\t"
				+ findClass(new Mockito()));
		System.out.println(Class.class.getResourceAsStream(new Mockito()
				.getClass().getName().replace('.', File.separatorChar)
				+ ".class"));
		System.out.println(new StringBuffer().getClass() + "\t>>\t"
				+ findClass(new StringBuffer()));
	}

	private static String findClass(Object a) throws FileNotFoundException,
			IOException {
		for (String classpathEntry : System.getProperty("java.class.path")
				.split(System.getProperty("path.separator"))) {
			File entry = new File(classpathEntry);
			if (entry.isDirectory()) {
				String found = findFile(a.getClass().getSimpleName(), entry);
				if (found != null)
					return found;
			} else if (classpathEntry.endsWith(".jar")) {
				final int BUFFER = 2048;
				try {
					FileInputStream fis = new FileInputStream(entry);
					ZipInputStream zis = new ZipInputStream(
							new BufferedInputStream(fis));
					ZipEntry j;
					while ((j = zis.getNextEntry()) != null) {
						System.out.println("Extracting: " + j);
						StringBuilder b = new StringBuilder();
						byte data[] = new byte[BUFFER];
						int count = 0;
						while ((count = zis.read(data, 0, BUFFER)) != -1) {
							if (j.getName().endsWith(".html")) {
								for (int i = 0; i < count; i++)
									b.append((char) data[i]);
							}
						}
						if (j.getName().endsWith(".html")) {
							System.out.println(entry);
							System.out.println(b.toString());
							return null;
						}
					}
					zis.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (classpathEntry.endsWith(".class")
					&& classpathEntry.startsWith(a.getClass().getSimpleName())) {
				return classpathEntry;
			}
		}
		return null;
	}

	private static String findFile(String classname, File entry) {
		if (entry.isDirectory()) {
			for (File child : entry.listFiles()) {
				String found = findFile(classname, child);
				if (found != null)
					return found;
			}
		} else if (entry.getName().endsWith(".class")
				&& entry.getName().startsWith(classname)) {
			return entry.getPath();
		}
		return null;
	}
	

	protected static void moveTo(Agent a, UpDevice to, Gateway g){
		
		Call move = new Call("uos.ExecutionDriver", "executeAgent");
		move.setChannels(2);
		move.setServiceType(ServiceType.STREAM);
		move.addParameter("class", a.getClass().getName());
		
		try {
			Response r = g.callService(to, move);
			logger.fine("Opening agent stream.");
			ObjectOutputStream writer_agent = new ObjectOutputStream(r.getMessageContext().getDataOutputStream(0));
			logger.fine("Sending agent.");
			writer_agent.writeObject(a);
			writer_agent.close();
			logger.fine("Opening class stream.");
			OutputStream writer_class = r.getMessageContext().getDataOutputStream(1);
			final String classFile = findClass(a);
			logger.fine("Sending class "+classFile);
			InputStream clazz= new FileInputStream(classFile);
			logger.fine("Uffa.");
			int b;
			while((b = clazz.read())!= -1)
				writer_class.write(b);
			writer_class.close();
//			logger.fine("Waiting to finish.");
//			while (writer_agent.)
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
