package org.ioc;

import org.ioc.util.ObjectInstantiationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ComponentModel {
    private static final String PROXY_ALREADY_CREATED_MSG = "Proxy instance already created.";
    /**
     * The type of the component.
     */
    private Class<?> componentType;
    /**
     * The annotation used to map the component (@Component or a custom one).
     */
    private Annotation annotation;

    /**
     * The constructor that will be used to create an instance of the service.
     */
    private Constructor<?> targetConstructor;

    /**
     * The name of the instance or null if no name has been given. If @NameInstance exist.
     */
    private String instanceName;
    /**
     * Component instance.
     */
    private Object instance;
    /**
     * Reference to the post construct method if any.
     */
    private Method postConstructMethod;
    /**
     * Reference to the pre destroy method if any.
     */
    private Method preDestroyMethod;
    /**
     * Holds information for service's scope.
     */
    private ScopeType scopeType;
    /**
     * The reference to all @Bean (or a custom one) annotated methods.
     */
    private Collection<ComponentBeanModel> beans;

    private Field[] autowireAnnotatedFields;

    protected boolean instanceRequested;

    private Object proxyInstance;

    private LinkedList<DependencyParam> resolvedConstructorParams;
    private LinkedList<DependencyParam> resolvedFields;

    private final Map<Method, List<MethodAspectHandlerDto>> methodAspectHandlers = new HashMap<>();


    public ComponentModel() {
    }

    public ComponentModel(Class<?> componentType,
                          Annotation annotation,
                          Constructor<?> targetConstructor,
                          String instanceName,
                          Method postConstructMethod, Method preDestroyMethod,
                          ScopeType scopeType,
                          Field[] autowireAnnotatedFields) {
        this();
        this.setComponentType(componentType);
        this.setAnnotation(annotation);
        this.setTargetConstructor(targetConstructor);
        this.setInstanceName(instanceName);
        this.setPostConstructMethod(postConstructMethod);
        this.setPreDestroyMethod(preDestroyMethod);
        this.setScopeType(scopeType);
        this.setAutowireAnnotatedFields(autowireAnnotatedFields);
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setResolvedConstructorParams(LinkedList<DependencyParam> resolvedConstructorParams) {
        this.resolvedConstructorParams = resolvedConstructorParams;
    }

    public void setResolvedFields(LinkedList<DependencyParam> resolvedFields) {
        this.resolvedFields = resolvedFields;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Object getActualInstance() {
        return this.instance;
    }

    public Object getInstance() {
        if (this.getScopeType() == ScopeType.PROTOTYPE) {
            if (this.instance == null) {
                return null;
            }
            if (!this.instanceRequested) {
                this.instanceRequested = true;
                return this.instance;
            }
            return ObjectInstantiationUtils.createNewInstance(this);
        }
        if (this.proxyInstance != null) {
            return this.proxyInstance;
        }
        return instance;
    }

    public void setProxyInstance(Object proxyInstance) {
        if (this.proxyInstance != null) {
            throw new IllegalArgumentException(PROXY_ALREADY_CREATED_MSG);
        }

        this.proxyInstance = proxyInstance;
    }

    public Method getPostConstructMethod() {
        return postConstructMethod;
    }

    public Method getPreDestroyMethod() {
        return preDestroyMethod;
    }

    public Field[] getAutowireAnnotatedFields() {
        return autowireAnnotatedFields;
    }

    public LinkedList<DependencyParam> getResolvedFields() {
        return resolvedFields;
    }

    public LinkedList<DependencyParam> getResolvedConstructorParams() {
        return resolvedConstructorParams;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public Class<?> getComponentType() {
        return componentType;
    }

    public Collection<ComponentBeanModel> getBeans() {
        return beans;
    }

    public void setAutowireAnnotatedFields(Field[] autowireAnnotatedFields) {
        this.autowireAnnotatedFields = autowireAnnotatedFields;
    }

    public void setComponentType(Class<?> componentType) {
        this.componentType = componentType;
    }

    public void setBeans(Collection<ComponentBeanModel> beans) {
        this.beans = beans;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    public void setScopeType(ScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setTargetConstructor(Constructor<?> targetConstructor) {
        this.targetConstructor = targetConstructor;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public void setPostConstructMethod(Method postConstructMethod) {
        this.postConstructMethod = postConstructMethod;
    }

    public void setPreDestroyMethod(Method preDestroyMethod) {
        this.preDestroyMethod = preDestroyMethod;
    }

    public void setMethodAspectHandlers(Map<Method, List<MethodAspectHandlerDto>> methodAspectHandlers) {
        this.methodAspectHandlers.putAll(methodAspectHandlers);
    }

    public Map<Method, List<MethodAspectHandlerDto>> getMethodAspectHandlers() {
        return this.methodAspectHandlers;
    }


    public Constructor<?> getTargetConstructor() {
        return targetConstructor;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ComponentModel{");
        sb.append("componentType=").append(componentType);
        sb.append(", annotation=").append(annotation);
        sb.append(", targetConstructor=").append(targetConstructor);
        sb.append(", instanceName='").append(instanceName).append('\'');
        sb.append(", instance=").append(instance);
        sb.append(", postConstructMethod=").append(postConstructMethod);
        sb.append(", preDestroyMethod=").append(preDestroyMethod);
        sb.append(", scopeType=").append(scopeType);
        sb.append(", beans=").append(this.getBeans());
        sb.append('}');
        return sb.toString();
    }
}
