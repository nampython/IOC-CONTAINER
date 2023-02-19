package org.ioc;

import org.ioc.type.DirectoryType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

public class DirectoryHandlerImpl implements DirectoryHandler {
    private static final String JAR_FILE_EXTENSION = ".jar";

    /**
     *
     * @param initClass
     */
    @Override
    public Directory resolveDirectory(Class<?> initClass) {
        String pathDir = this.getDir(initClass);
        DirectoryType dirType = this.getDirType(pathDir);
        return new Directory(pathDir, dirType);
    }

    @Override
    public Directory resolveDirectory(File directory) {
        try {
            return new Directory(
                    directory.getCanonicalPath(), this.getDirType(directory.getCanonicalPath())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param pathDir
     * @return
     */
    private DirectoryType getDirType(String pathDir) {
        final File file = new File(pathDir);
        boolean isJarFIle = this.checkJarFile(file, pathDir);
        return isJarFIle ? DirectoryType.JAR_FILE : DirectoryType.DIRECTORY;
    }

    private boolean checkJarFile(File file, String pathDir) {
        return !file.isDirectory() && pathDir.endsWith(JAR_FILE_EXTENSION);
    }

    private String getDir(Class<?> initClass) {
        return URLDecoder.decode(initClass
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getFile(), StandardCharsets.UTF_8);
    }
}
