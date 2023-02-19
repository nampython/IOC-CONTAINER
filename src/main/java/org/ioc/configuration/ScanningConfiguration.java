package org.ioc.configuration;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ScanningConfiguration extends CoreConfiguration{
    private final Set<Class<? extends Annotation>> componentAnnotations;
    private final Set<Class<? extends Annotation>> beanAnnotations;
    private ClassLoader classLoader;

    public ScanningConfiguration(Configuration configuration) {
        super(configuration);
        this.componentAnnotations = new HashSet<>();
        this.beanAnnotations = new HashSet<>();
        this.classLoader = Thread.currentThread().getContextClassLoader();

    }

    public ScanningConfiguration addComponentAnnotation(Class<? extends Annotation> annotation) {
        this.componentAnnotations.add(annotation);
        return this;
    }

    public ScanningConfiguration addComponentAnnotations(Collection<Class<? extends Annotation>> annotations) {
        this.componentAnnotations.addAll(Set.copyOf(annotations));
        return this;
    }

    public ScanningConfiguration addBeanAnnotation(Class<? extends Annotation> annotation) {
        this.beanAnnotations.add(annotation);
        return this;
    }

    public ScanningConfiguration addBeanAnnotations(Collection<Class<? extends Annotation>> annotations) {
        this.beanAnnotations.addAll(Set.copyOf(annotations));
        return this;
    }

    public Set<Class<? extends Annotation>> getComponentAnnotations() {
        return this.componentAnnotations;
    }

    public Set<Class<? extends Annotation>> getBeanAnnotations() {
        return this.beanAnnotations;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
