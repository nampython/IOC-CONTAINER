package org.ioc.contex;

import org.ioc.ComponentModel;
import org.ioc.contex.enviroment.EnvironmentSource;
import org.ioc.contex.factory.BeanFactory;
import org.ioc.contex.factory.ListableBeanFactory;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface ApplicationContext extends EnvironmentSource , ListableBeanFactory {
    ComponentModel getDefineBean(Class<?> cls);
    ComponentModel getDefineBean(Class<?> cls, String beanName);
    Collection<ComponentModel> getImplementations(Class<?> cls);
    Collection<ComponentModel> getDefineBeansWithAnnotation(Class<? extends Annotation> annotation);
    Collection<ComponentModel> getAllDefineBean();
}
