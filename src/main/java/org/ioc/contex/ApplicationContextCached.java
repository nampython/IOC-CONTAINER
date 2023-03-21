package org.ioc.contex;

import org.ioc.engine.ComponentModel;
import org.ioc.contex.factory.ApplicationContextInternal;

import java.lang.annotation.Annotation;
import java.util.*;
//TODO: implement getComponentByAnnotation
public class ApplicationContextCached extends ApplicationContextInternal {
    private final Map<Class<?>, ComponentModel> cachedComponents;
    private final Map<Class<?>, Collection<ComponentModel>> cachedImplementations;
    private final Map<Class<? extends Annotation>, Collection<ComponentModel>> cachedComponentsByAnnotation;

    public ApplicationContextCached(Set<Class<?>> localClasses, List<ComponentModel> components) {
        this.cachedComponents = new HashMap<>();
        this.cachedImplementations = new HashMap<>();
        this.cachedComponentsByAnnotation = new HashMap<>();
        super.init(localClasses, components);
    }

    @Override
    public ComponentModel getDefineBean(Class<?> componentType) {
        if (this.cachedComponents.containsKey(componentType)) {
            return this.cachedComponents.get(componentType);
        } else {
            final ComponentModel componentModel = super.getDefineBean(componentType);
            if (componentModel != null) {
                this.cachedComponents.put(componentType, componentModel);
            }
            return componentModel;
        }
    }

    @Override
    public Collection<ComponentModel> getImplementations(Class<?> cls) {
        if (this.cachedImplementations.containsKey(cls)) {
            return this.cachedImplementations.get(cls);
        } else {
            final Collection<ComponentModel> implementations = super.getImplementations(cls);
            this.cachedImplementations.put(cls, implementations);
            return implementations;
        }
    }
}
