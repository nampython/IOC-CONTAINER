package org.ioc.support;

import org.ioc.engine.DependencyParam;

public interface DependencyResolver {

    boolean canResolve(DependencyParam dependencyParam);

    Object resolve(DependencyParam dependencyParam);
}
