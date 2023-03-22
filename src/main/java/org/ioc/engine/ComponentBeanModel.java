package org.ioc.engine;

import org.ioc.engine.core.InstantiationComponentBean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ComponentBeanModel extends ComponentModel {
    /**
     * Reference to the method that returns instance of this type of bean.
     */
    private final Method originMethod;
    /**
     * The component from this bean was created.
     */
    private final ComponentModel rootComponent;

    public ComponentBeanModel(Class<?> beanType, Method originMethod, ComponentModel rootCompononent,
            Annotation annotation,
            ScopeType scopeType,
            String instanceName) {
        super.setComponentType(beanType);
        super.setBeans(new ArrayList<>(0));
        super.setAnnotation(annotation);
        super.setScopeType(scopeType);
        super.setInstanceName(instanceName);
        this.originMethod = originMethod;
        this.rootComponent = rootCompononent;
    }
    public Method getOriginMethod() {
        return this.originMethod;
    }

    public ComponentModel getRootComponent() {
        return rootComponent;
    }

    @Override
    public Object getInstance() {
        if (super.getScopeType() == ScopeType.PROTOTYPE) {
            if (super.getActualInstance() == null) {
                return null;
            }

            if (!super.instanceRequested) {
                super.instanceRequested = true;
                return super.getActualInstance();
            }

            return InstantiationComponentBean.HandlerInstantiation.createNewInstance(this);
        }
        return super.getInstance();
    }
}
