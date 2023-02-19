package org.ioc;

import org.ioc.support.DependencyResolver;

import java.lang.annotation.Annotation;
import java.util.Arrays;

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

    public boolean isCompatible(ComponentModel componentModel) {
        final boolean isRequiredTypeAssignable = this.dependencyType.isAssignableFrom(componentModel.getComponentType());
        final boolean isRequiredTypeAssignable2 = componentModel.getInstance() != null &&
                this.dependencyType.isAssignableFrom(componentModel.getInstance().getClass());
        final boolean instanceNameMatches = this.instanceName  == null || this.instanceName.equalsIgnoreCase(componentModel.getInstanceName());

        return (isRequiredTypeAssignable || isRequiredTypeAssignable2) && instanceNameMatches;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DependencyParam{");
        sb.append("dependencyType=").append(dependencyType);
        sb.append(", instanceName='").append(instanceName).append('\'');
        sb.append(", annotations=").append(annotations == null ? "null" : Arrays.asList(annotations).toString());
        sb.append(", isRequired=").append(isRequired);
        sb.append(", dependencyResolver=").append(dependencyResolver);
        sb.append(", componentModel=").append(componentModel);
        sb.append(", instance=").append(instance);
        sb.append('}');
        return sb.toString();
    }
}

