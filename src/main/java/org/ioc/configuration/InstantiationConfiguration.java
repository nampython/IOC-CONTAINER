package org.ioc.configuration;


import org.ioc.engine.ComponentModel;
import org.ioc.support.DependencyResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class InstantiationConfiguration extends CoreConfiguration {
    private final Collection<ComponentModel> providedComponentModels;
    private final Set<DependencyResolver> dependencyResolvers;

    public InstantiationConfiguration(Configuration parentConfig) {
        super(parentConfig);
        this.providedComponentModels = new ArrayList<>();
        this.dependencyResolvers = new HashSet<>();
    }

    public InstantiationConfiguration addProvidedComponents(Collection<ComponentModel> serviceDetails) {
        this.providedComponentModels.addAll(serviceDetails);
        return this;
    }

    public InstantiationConfiguration addDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolvers.add(dependencyResolver);
        return this;
    }

    public Collection<ComponentModel> getProvidedComponentModels() {
        return this.providedComponentModels;
    }

    public Set<DependencyResolver> getDependencyResolvers() {
        return this.dependencyResolvers;
    }
}
