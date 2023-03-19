package org.ioc.engine;

import java.util.Set;

public abstract class SettingComponent {
    public abstract Set<ComponentModel> mappingComponent(Set<Class<?>> allComponent);
    public abstract void init();
}
