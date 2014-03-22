package org.vaadin.leif.servletcontainer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

public class MyExplodedWarServletContext extends MyServletContext {

    private final ClassLoader classLoader;

    public MyExplodedWarServletContext(String contextPath, File explodedWarDir)
            throws Exception {
        super(contextPath);

        File webInf = new File(explodedWarDir, "WEB-INF");

        List<File> classloaderFiles = new ArrayList<File>();
        findClassloaderFiles(classloaderFiles, webInf);

        URL[] classloaderUrls = new URL[classloaderFiles.size()];
        for (int i = 0; i < classloaderUrls.length; i++) {
            File file = classloaderFiles.get(i);
            URL url = file.toURI().toURL();
            if (file.isDirectory()) {
                url = new URL(url.toString() + "/");
            }
            classloaderUrls[i] = url;
        }
        this.classLoader = new URLClassLoader(classloaderUrls, getClass()
                .getClassLoader());

        findWebServletInClasses(new File(webInf, "classes"), "");
    }

    private static void findClassloaderFiles(List<File> classloaderFiles,
            File webInf) {
        /*
         * Step 8
         * 
         * Add all the resources that should be used by the context class loader
         * to the provided list. Supported resources include Jar files and
         * directory structures containing class files.
         */
        throw new UnsupportedOperationException("Implement in step 8");
    }

    private void findWebServletInClasses(File dir, String packageName)
            throws Exception {
        for (File child : dir.listFiles()) {
            String name = packageName + child.getName();
            if (child.isDirectory()) {
                findWebServletInClasses(child, name + ".");
            } else if (child.getName().endsWith(".class")) {
                // Strip ".class" from the end
                String className = name.substring(0, name.length() - 6);

                HttpServlet servlet = scanClass(className);

                if (servlet != null) {
                    addServlet(servlet);
                }
            }
        }
    }

    private HttpServlet scanClass(String className) throws Exception {
        /*
         * Step 9
         * 
         * Test whether the passed class name is a servlet class with a valid
         * WebServlet annotation. If it is, return a newly created instance of
         * that servlet class, otherwise return null.
         * 
         * To do this, you need to load the class using getClassLoader() and
         * investigate it using reflection.
         */

        throw new UnsupportedOperationException("Implement in step 9");
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private static final void specialMethod() {
        /*
         * A special method that is here just to keep some classes that might be
         * needed later on imported even when there's a save action that removes
         * unused imports.
         */

        Class<?>[] classes = { WebServlet.class };
    }

}
