//package org.ioc.contex;
//
//import org.ioc.ComponentModel;
//
//import java.lang.annotation.Annotation;
//import java.util.*;
//
//public class ApplicationContextCached extends AnnotationConfigApplicationContext {
//
//    private final Map<Class<?>, ComponentModel> cachedComponents;
//    private final Map<Class<?>, Collection<ComponentModel>> cachedImplementations;
//    private final Map<Class<? extends Annotation>, Collection<ComponentModel>> cachedComponentsByAnnotation;
//
//    public ApplicationContextCached(Set<Class<?>> localClasses, List<ComponentModel> components) {
//        this.cachedComponents = new HashMap<>();
//        this.cachedImplementations = new HashMap<>();
//        this.cachedComponentsByAnnotation = new HashMap<>();
//        super.init(localClasses, components);
//    }
//}
