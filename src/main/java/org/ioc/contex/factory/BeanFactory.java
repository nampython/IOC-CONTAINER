package org.ioc.contex.factory;

public interface BeanFactory {
    <T> T getBean(Class<T> cls);
    <T> T getBean(String beanName);
    <T> T getBean(Class<?> cls, String instanceName);
    <T> T getNewBean(Class<?> beanClass);
    <T> T getNewBean(Class<?> beanClass, String instanceName);
}
