package org.ioc.contex.factory;

public interface UpdatableBeanFactory {
    void updateBeanInstance(Object componentInstance);
    void updateBeanInstance(Class<?> cls, Object componentInstance);
    void updateBeanInstance(Class<?> cls, Object componentInstance, boolean destroyOldInstance);
}
