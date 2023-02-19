package org.ioc.contex.enviroment;

import java.util.Collection;
import java.util.List;

public interface EnvironmentSource {
    Collection<Class<?>> getClassLoader();
}
