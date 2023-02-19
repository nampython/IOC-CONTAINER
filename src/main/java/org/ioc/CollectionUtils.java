package org.ioc;

import org.ioc.exception.ComponentInstantiationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class CollectionUtils {
    public static <T> Collection<T> createInstanceOfCollection(Class<?> cls) {
        if (cls.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<>();
        }
        if (cls.isAssignableFrom(HashSet.class)) {
            return new HashSet<>();
        }
        throw new ComponentInstantiationException("Cannot autowire collection of type %s.");
    }
}
