package org.ioc.engine;

import org.ioc.exception.ClassLocationException;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ClassLoaderContextDir extends ClassLoaderContext {
    private final Set<Class<?>> locatedClasses;
    public ClassLoaderContextDir() {
        this.locatedClasses = new HashSet<>();
    }

    @Override
    public Set<Class<?>> loadClasses(String dir) {
        this.init();
        File file = new File(dir);
        File[] listFiles = Objects.requireNonNull(file.listFiles());
        String initialPackage = "";

        if (!file.isDirectory()) {
            throw new ClassLocationException(String.format(INVALID_DIRECTORY_MSG, dir));
        }
        else {
            this.processInnerFiles(listFiles, initialPackage);
        }
        return this.locatedClasses;
    }

    private void processInnerFiles(File[] listFiles, String initialPackage) {
        try {
            for (File innerFile : listFiles) {
                this.scanDir(innerFile, initialPackage);
            }
        } catch (ClassNotFoundException e) {
            throw new ClassLocationException(e.getMessage(), e);
        }
    }


    private void scanDir(File file, String packageName) throws ClassNotFoundException {
        if (file.isDirectory()) {
            packageName += file.getName() + ".";
            File[] listFiles = Objects.requireNonNull(file.listFiles());
            for (File innerFile : listFiles) {
                this.scanDir(innerFile, packageName);
            }
        } else {
            if (!file.getName().endsWith(JAVA_BINARY_EXTENSION)) {
                return;
            }
            final String className = packageName + file
                    .getName()
                    .replace(JAVA_BINARY_EXTENSION, "");
            this.locatedClasses.add(Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
        }
    }
    private void init() {
        this.locatedClasses.clear();
    }
}
