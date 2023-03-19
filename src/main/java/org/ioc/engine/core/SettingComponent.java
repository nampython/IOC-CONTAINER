package org.ioc.engine.core;

import org.ioc.engine.ComponentModel;

import java.util.Set;

public abstract class SettingComponent {
    public abstract Set<ComponentModel> mappingComponent(Set<Class<?>> allComponent);
    public abstract void init();
}
