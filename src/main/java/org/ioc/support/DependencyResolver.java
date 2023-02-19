package org.ioc.support;

import org.ioc.DependencyParam;

public interface DependencyResolver {

    boolean canResolve(DependencyParam dependencyParam);

    Object resolve(DependencyParam dependencyParam);
}
