package org.ioc.engine;

import org.ioc.exception.ComponentInstantiationException;

import java.util.Collection;
import java.util.Set;

public abstract class InstantiateContext {
    public abstract Collection<ComponentModel> instantiateComponentAndBean(Set<ComponentModel> componentModels) throws ComponentInstantiationException;
}
