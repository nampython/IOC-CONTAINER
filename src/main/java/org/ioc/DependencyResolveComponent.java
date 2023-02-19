package org.ioc;

import java.util.Collection;
import java.util.List;

public interface DependencyResolveComponent {
    public List<EnqueuedComponentDetails> resolveDependencies(Collection<ComponentModel> componentModels);
}
