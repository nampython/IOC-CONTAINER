package org.ioc.contex.factory;

public interface UpdatableBeanFactory {
    void update(Object componentInstance);
    void update(Class<?> cls, Object componentInstance);
    void update(Class<?> cls, Object componentInstance, boolean destroyOldInstance);
}
