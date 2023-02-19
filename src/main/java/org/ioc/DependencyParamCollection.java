package org.ioc;

import org.ioc.support.HandlerGeneric;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DependencyParamCollection extends DependencyParam {
    private final Class<?> collectionType;
    private List<ComponentModel> componentModels;

    public DependencyParamCollection(ParameterizedType parameterizedType,
                                     Class<?> dependencyType,
                                     String instanceName,
                                     Annotation[] annotations) {
        super(HandlerGeneric.getRawType(parameterizedType), instanceName, annotations);
        this.collectionType = dependencyType;
    }

    public void setComponentModels(List<ComponentModel> componentModels) {
        this.componentModels = componentModels;
    }

    @Override
    public Object getInstance() {
        if (super.getDependencyResolver() != null) {
            return super.getInstance();
        }
        final Collection<Object> collection = CollectionUtils.createInstanceOfCollection(this.collectionType);
        collection.addAll(
                this.componentModels.stream().map(ComponentModel::getInstance).collect(Collectors.toList()));
        return collection;
    }
}
