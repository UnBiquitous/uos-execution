package org.unbiquitous.driver.spike;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

import org.unbiquitous.driver.execution.executeAgent.ClassToolbox;

public class FSpike {

		public static void main(String[] args) throws Exception {
			ClassToolbox box = new ClassToolbox();
			ClassLoader loader = box.load(new FileInputStream("auos.exe_spiker.jar"));
			
			Class<?> clazz = loader.loadClass("org.unbiquitous.driver.execution.spike.HelloFromAndroidAgent");
			Object o = clazz.newInstance();
			Method setText = clazz.getMethod("setText", String.class);
			setText.invoke(o, "será?");
			toFile(o, "HelloFromAndroidAgent");
		}

		@SuppressWarnings("resource")
		private static void toFile(Object obj, String file_prefix) throws IOException, FileNotFoundException,
				Exception {
			ObjectOutputStream writer_agent = new ObjectOutputStream(
												new FileOutputStream(file_prefix+"_obj"));
			writer_agent.writeObject(obj);
			writer_agent.close();
			
			ClassToolbox toolbox = createToolBox();
			File dalvik = toolbox.packageDalvikFor(obj.getClass());
			File clazz = new File(file_prefix+"_class");
			clazz.createNewFile();
			
			FileChannel source = null;
		    FileChannel destination = null;
		    try {
		        source = new FileInputStream(dalvik).getChannel();
		        destination = new FileOutputStream(clazz).getChannel();

		        // previous code: destination.transferFrom(source, 0, source.size());
		        // to avoid infinite loops, should be:
		        long count = 0;
		        long size = source.size();              
		        while((count += destination.transferFrom(source, count, size-count))<size);
		    }
		    finally {
		        if(source != null) {
		            source.close();
		        }
		        if(destination != null) {
		            destination.close();
		        }
		    }
		}

		private static ClassToolbox createToolBox() {
			ClassToolbox toolbox = new ClassToolbox();
			toolbox.add2BlackList("junit");
			toolbox.add2BlackList("log4j");
			toolbox.add2BlackList("hsqldb");
			toolbox.add2BlackList("owlapi");
			toolbox.add2BlackList("jcl-core");
			toolbox.add2BlackList("objenesis");
			toolbox.add2BlackList("cglib-nodep");
			toolbox.add2BlackList("HermiT");
			toolbox.add2BlackList("hamcrest-core");
			toolbox.add2BlackList("mockito-all");
			toolbox.add2BlackList("uos-core");
			toolbox.add2BlackList("uos.tcp_udp.plugin");
			toolbox.add2BlackList("uos_tcp_udp_plugin");
			toolbox.add2BlackList("uos_core");
//			toolbox.add2BlackList("execution");
			toolbox.add2BlackList("ExecutionDriver");
			return toolbox;
		}
}
