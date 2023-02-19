package org.ioc.contex.factory;

import java.lang.annotation.Annotation;
import java.util.Map;

public interface ListableBeanFactory extends BeanFactory, UpdatableBeanFactory, ReloadableBeanFactory{
    String[] getBeanDefinitionNames();
    int getBeanDefinitionCount();
    Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation);
    String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotation);
}
