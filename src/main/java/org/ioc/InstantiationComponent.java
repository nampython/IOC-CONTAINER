package org.ioc;

import org.ioc.exception.ComponentInstantiationException;

import java.util.Collection;
import java.util.Set;

public interface InstantiationComponent {
    Collection<ComponentModel> instantiateComponentAndBean(Set<ComponentModel>  componentModels) throws ComponentInstantiationException;
}
