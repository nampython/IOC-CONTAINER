package org.ioc.engine;

import org.ioc.exception.ComponentInstantiationException;
import org.ioc.exception.PostConstructException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

    public ComponentModel(Class<?> componentType, Annotation annotation, Constructor<?> targetConstructor, String instanceName, Method postConstructMethod, Method preDestroyMethod, ScopeType scopeType, Field[] autowireAnnotatedFields) {
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
            } else if (!this.instanceRequested) {
                this.instanceRequested = true;
                return this.instance;
            }
            return ObjectInstantiation.createNewInstance(this);
        } else {
            if (this.proxyInstance != null) {
                return this.proxyInstance;
            }
            return instance;
        }
    }

    public void setProxyInstance(Object proxyInstance) {
        if (this.proxyInstance != null) {
            throw new IllegalArgumentException(PROXY_ALREADY_CREATED_MSG);
        } else {
            this.proxyInstance = proxyInstance;
        }
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
    /**
     * We are using the componentType hashcode in order to make this class unique
     * when using in in sets.
     *
     * @return hashcode.
     */
    @Override
    public int hashCode() {
        if (this.componentType == null) {
            return super.hashCode();
        }

        return this.componentType.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ComponentModel)) {
            return false;
        }

        if (this.componentType == null) {
            return super.equals(other);
        }

        final ComponentModel otherService = (ComponentModel) other;
        return Objects.equals(otherService.getInstanceName(), this.getInstanceName())
                && Objects.equals(otherService.getAnnotation(), this.getAnnotation())
                && Objects.equals(otherService.getComponentType(), this.getComponentType())
                && Objects.equals(otherService.getScopeType(), this.getScopeType());
    }

    @Override
    public String toString() {
        if (this.componentType == null) {
            return super.toString();
        }

        return this.componentType.getName();
    }


    public static class ObjectInstantiation {
        private static final String INVALID_PARAMETERS_COUNT_MSG = "Invalid parameters count for '%s'.";

        public static Object createNewInstance(ComponentModel componentModel) {
            final Object[] constructorParams = componentModel.getResolvedConstructorParams().stream().map(DependencyParam::getInstance).toArray(Object[]::new);
            final Object[] fieldParams = componentModel.getResolvedFields().stream().map(DependencyParam::getInstance).toArray(Object[]::new);
            return createNewInstance(componentModel, constructorParams, fieldParams);
        }

        public static Object createNewInstance(ComponentModel componentModel, Object[] constructorParams, Object[] fieldAutowiredParams) {
            Constructor<?> constructor = componentModel.getTargetConstructor();
            if (constructor.getParameterCount() != constructorParams.length) {
                throw new ComponentInstantiationException(String.format(INVALID_PARAMETERS_COUNT_MSG, componentModel.getComponentType().getName()));
            }
            try {
                final Object instance = constructor.newInstance(constructorParams);
                componentModel.setInstance(instance);
                setAutowiredFieldInstances(componentModel, fieldAutowiredParams, instance);
                invokePostConstruct(componentModel, instance);
                return instance;
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new ComponentInstantiationException(e.getMessage(), e);
            }
        }

        private static void invokePostConstruct(ComponentModel componentModel, Object instance) {
            if (componentModel.getPostConstructMethod() == null) {
                return;
            }
            try {
                componentModel.getPostConstructMethod().invoke(instance);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new PostConstructException(e.getMessage(), e);
            }
        }

        private static void setAutowiredFieldInstances(ComponentModel componentModel, Object[] fieldAutowiredParams, Object instance) throws IllegalAccessException {
            Field[] autowireAnnotatedFields = componentModel.getAutowireAnnotatedFields();
            for (int i = 0; i < autowireAnnotatedFields.length; i++) {
                autowireAnnotatedFields[i].set(instance, autowireAnnotatedFields[i]);
            }
        }
    }
}
