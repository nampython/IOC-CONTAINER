package org.ioc.engine.core;

import org.ioc.engine.ClassLoaderContext;
import org.ioc.exception.ClassLocationException;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ClassLocator implementation for jar files.
 * <p>
 * Creates a JarFile object from the jar file from which the application is
 * executed and filters those entries have are class files.
 */
public class ClassLoaderContextJar extends ClassLoaderContext {
    public ClassLoaderContextJar() {

    }

    /**
     * Creates JarFile from the given directory.
     * <p>
     * Iterates all entries and checks if the entry name ends with ".class".
     * If that is the case, adds the class to a set of located classes.
     *
     * @param directory the given directory to the jar file.
     * @return a set of located classes.
     */
    @Override
    public Set<Class<?>> loadClasses(String directory) {
        final Set<Class<?>> locatedClasses = new HashSet<>();
        try {
            JarFile jarFile = new JarFile(new File(directory));
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (!jarEntry.getName().endsWith(JAVA_BINARY_EXTENSION)) {
                    continue;
                }
                final String className = jarEntry.getName().replace(JAVA_BINARY_EXTENSION, "")
                        .replaceAll("\\\\", ".")
                        .replaceAll("/", ".");
                locatedClasses.add(Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new ClassLocationException(e.getMessage(), e);
        }
        return locatedClasses;
    }
}
