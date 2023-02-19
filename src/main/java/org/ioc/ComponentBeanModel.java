package org.ioc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ComponentBeanModel extends ComponentModel {
    /**
     * Reference to the method that returns instance of this type of bean.
     */
    private final Method beanMethod;
    /**
     * The component from this bean was created.
     */
    private final ComponentModel rootComponent;

    public ComponentBeanModel(Class<?> beanType, Method beanMethod, ComponentModel componentModel,
            Annotation annotation,
            ScopeType scopeType,
            String instanceName) {
        super.setComponentType(beanType);
        super.setBeans(new ArrayList<>(0));
        super.setAnnotation(annotation);
        super.setScopeType(scopeType);
        super.setInstanceName(instanceName);
        this.beanMethod = beanMethod;
        this.rootComponent = componentModel;
    }
    public Method getOriginMethod() {
        return this.beanMethod;
    }

    public ComponentModel getRootComponent() {
        return rootComponent;
    }
}
