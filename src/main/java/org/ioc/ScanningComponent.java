package org.ioc;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public interface ScanningComponent {
    Set<ComponentModel> mappingComponents(Set<Class<?>> locatedClasses);
}
