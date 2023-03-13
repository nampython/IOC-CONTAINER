package org.ioc.engine;

import java.util.Collection;
import java.util.Set;

public abstract class ClassLoaderContext {
    public  static final String INVALID_DIRECTORY_MSG = "Invalid directory '%s'.";
    public static final String JAVA_BINARY_EXTENSION = ".class";
    public abstract Set<Class<?>> loadClasses(String directory);;
}
