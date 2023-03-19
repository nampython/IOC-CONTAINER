package org.ioc.engine;

import org.ioc.support.DependencyResolver;

import java.lang.annotation.Annotation;

public class DependencyParam {
    private final Class<?> dependencyType;
    private final String instanceName;
    private final Annotation[] annotations;
    private boolean isRequired;
    private DependencyResolver dependencyResolver;
    private ComponentModel componentModel;
    private Object instance;

    public DependencyParam(Class<?> dependencyType, String instanceName, Annotation[] annotations) {
        this.dependencyType = dependencyType;
        this.instanceName = instanceName;
        this.annotations = annotations;
        this.setRequired(true);
    }

    public Class<?> getDependencyType() {
        return dependencyType;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public ComponentModel getComponentModel() {
        return componentModel;
    }
    // ???
    public Object getInstance() {
        final Object instance;
        if (this.dependencyResolver != null) {
            instance = this.instance;
        } else if (this.componentModel != null) {
            instance = this.componentModel.getInstance();
        } else {
            instance = null;
        }
        if (instance == null && this.isRequired) {
            throw new IllegalStateException(String.format(
                    "Trying to get instance for dependency '%s' but there is none",
                    this.dependencyType
            ));
        }

        return instance;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    public void setComponentModel(ComponentModel componentModel) {
        this.componentModel = componentModel;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    /**
     * > If the component model's component type is assignable to the dependency type, or if the component model's instance
     * is not null and the instance's class is assignable to the dependency type, and if the instance name is null or
     * matches the dependency's instance name, then return true
     *
     * @param componentModel - The component model that is being checked for compatibility.
     */
    public boolean isCompatible(ComponentModel componentModel) {
        final boolean isRequiredTypeAssignable = this.dependencyType.isAssignableFrom(componentModel.getComponentType());
        final boolean isRequiredTypeAssignable2 = componentModel.getInstance() != null &&
                this.dependencyType.isAssignableFrom(componentModel.getInstance().getClass());
        final boolean instanceNameMatches = this.instanceName  == null || this.instanceName.equalsIgnoreCase(componentModel.getInstanceName());
        return (isRequiredTypeAssignable || isRequiredTypeAssignable2) && instanceNameMatches;
    }
}

