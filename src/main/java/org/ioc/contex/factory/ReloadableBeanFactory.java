package org.ioc.contex.factory;

import org.ioc.ComponentModel;

public interface ReloadableBeanFactory {
    void reload(ComponentModel componentModel);
    void reload(Class<?> cls);
}
