package org.ioc;

import java.io.File;

public interface DirectoryHandler {
     Directory resolveDirectory(Class<?> initClass);
     Directory resolveDirectory(File directory);
}
