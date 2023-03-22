package org.ioc.engine.core;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.ioc.engine.*;
import org.ioc.exception.BeanInstantiationException;
import org.ioc.exception.ComponentInstantiationException;
import org.ioc.exception.PostConstructException;
import org.ioc.exception.PreDestroyExecutionException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class InstantiationComponentBean extends InstantiateContext {
    private final DependencyResolveComponent dependencyResolveComponent;

    public InstantiationComponentBean(DependencyResolveComponent dependencyResolveComponent) {
        this.dependencyResolveComponent = dependencyResolveComponent;
    }

    /**
     * > This function takes a set of component models, resolves their dependencies, instantiates the components, and
     * returns a collection of all the components and their beans
     *
     * @param componentModels The set of components that need to be instantiated.
     * @return A collection of ComponentModels
     */
    @Override
    public Collection<ComponentModel> instantiateComponentAndBean(Set<ComponentModel> componentModels) throws ComponentInstantiationException {
        final List<EnqueuedComponentDetails> enqueuedComponentDetails = this.dependencyResolveComponent.resolveDependencies(componentModels);
        for (EnqueuedComponentDetails enqueuedComponentDetail : enqueuedComponentDetails) {
            this.instantiateComponent(enqueuedComponentDetail);
        }
        final List<ComponentModel> allComponentsAndBean = new ArrayList<>();
        componentModels.forEach(
                componentModel -> {
                    allComponentsAndBean.add(componentModel);
                    allComponentsAndBean.addAll(componentModel.getBeans());
                }
        );
        return allComponentsAndBean;
    }

    /**
     * > It creates an instance of the component and registers it in the container.
     *
     * @param enqueuedComponentDetail - This is the object that contains all the information about the component that is
     * being instantiated.
     */
    private void instantiateComponent(EnqueuedComponentDetails enqueuedComponentDetail) {
        final ComponentModel componentModel=  enqueuedComponentDetail.getComponentModel();
        final Object[] constructorInstances = enqueuedComponentDetail.getConstructorInstances();
        final Object[] fieldInstances = enqueuedComponentDetail.getFieldInstances();
        if (enqueuedComponentDetail.getComponentModel().getInstance() == null) {
            HandlerInstantiation.createInstance(componentModel, constructorInstances, fieldInstances);
        }
        if (componentModel.getScopeType() == ScopeType.PROXY) {
            ProxyUtils.createProxyInstance(componentModel, enqueuedComponentDetail.getConstructorInstances());
        }
        this.registerResolvedDependencies(enqueuedComponentDetail);
        this.registerBeans(componentModel);
    }

    /**
     * > For each bean in the component model, create an instance of the bean and if the bean is a proxy bean, create a
     * proxy instance of the bean
     *
     * @param componentModel The component model that contains the bean model.
     */
    private void registerBeans(ComponentModel componentModel) {
        for (ComponentBeanModel bean : componentModel.getBeans()) {
            HandlerInstantiation.createBeanInstance(bean);
            if (bean.getScopeType() == ScopeType.PROXY) {
                ProxyUtils.createBeanProxyInstance(bean);
            }
        }
    }

    /**
     * > It sets the resolved constructor parameters and resolved fields of the component model
     *
     * @param enqueuedComponentDetail This is the component that we are currently resolving.
     */
    private void registerResolvedDependencies(EnqueuedComponentDetails enqueuedComponentDetail) {
        final ComponentModel componentModel = enqueuedComponentDetail.getComponentModel();
        componentModel.setResolvedConstructorParams(enqueuedComponentDetail.getConstructorParams());
        componentModel.setResolvedFields(enqueuedComponentDetail.getFieldDependencies());
    }
    /**
     * It creates a proxy instance for a component model
     */
    static class ProxyUtils {
        public static void createProxyInstance(ComponentModel componentModel, Object[] constructorParams) {
            final ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setSuperclass(componentModel.getComponentType());
            Object proxyInstance;
            try {
                proxyInstance = proxyFactory.create(componentModel.getTargetConstructor().getParameterTypes(), constructorParams);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            ((ProxyObject) proxyInstance).setHandler(new MethodInvocationHandler(componentModel));
            componentModel.setProxyInstance(proxyInstance);
        }

        public static void createBeanProxyInstance(ComponentModel componentModel) {
            if (!componentModel.getComponentType().isInterface()) {
                return;
            }
            final Object proxyInstance = Proxy.newProxyInstance(
                    componentModel.getComponentType().getClassLoader(),
                    new Class[]{componentModel.getComponentType()},
                    new InvocationHandlerImpl(componentModel));
            componentModel.setProxyInstance(proxyInstance);
        }
    }

    /**
     * Responsible for create new instance for Component or Bean. It can be used by {@link org.ioc.contex.factory.ApplicationContextInternal}
     */
    public static class HandlerInstantiation {
        private static final String INVALID_PARAMETERS_COUNT_MSG = "Invalid parameters count for '%s'.";

        public static void createInstance(ComponentModel componentModel) {
            componentModel.setInstance(createNewInstance(componentModel));
        }

        public static Object createNewInstance(ComponentModel componentModel) {
            final Object[] constructorParams = componentModel.getResolvedConstructorParams().stream().map(DependencyParam::getInstance).toArray(Object[]::new);
            final Object[] fieldParams = componentModel.getResolvedFields().stream().map(DependencyParam::getInstance).toArray(Object[]::new);
            return createNewInstance(componentModel, constructorParams, fieldParams);
        }

        /**
         * > It creates a new instance of the component model's class, and sets the instance of the component model to the
         * newly created instance
         *
         * @param componentModel           - The component model that is being instantiated.
         * @param constructorInstances     - The instances of the constructor parameters.
         * @param autowiredFieldInstances- This is a list of instances that are to be autowired into the fields of the
         *                                 component.
         */
        public static void createInstance(ComponentModel componentModel, Object[] constructorInstances, Object[] autowiredFieldInstances) {
            componentModel.setInstance(createNewInstance(componentModel, constructorInstances, autowiredFieldInstances));
        }

        /**
         * It creates a new instance of the component using the constructor with the given parameters
         *
         * @param componentModel          - The component model of the component to be instantiated.
         * @param constructorParams       - the parameters that will be passed to the constructor of the component
         * @param autowiredFieldInstances - This is an array of instances of the fields that are annotated with @Autowired.
         * @return - An instance of the class that is being created.
         */
        private static Object createNewInstance(ComponentModel componentModel, Object[] constructorParams, Object[] autowiredFieldInstances) {
            final Constructor<?> constructor = componentModel.getTargetConstructor();
            if (constructor.getParameterCount() != constructorParams.length) {
                throw new ComponentInstantiationException(String.format(INVALID_PARAMETERS_COUNT_MSG, componentModel.getComponentType().getName()));
            } else {
                try {
                    final Object instance = constructor.newInstance(constructorParams);
                    componentModel.setInstance(instance);
                    setAutowiredFieldInstances(componentModel, autowiredFieldInstances, instance);
                    invokePostConstruct(componentModel, instance);
                    return instance;
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }

        /**
         * Set the autowired field instances on the instance.
         *
         * @param componentModel          - The component model for the class we're instantiating.
         * @param autowiredFieldInstances - The instances of the fields that are annotated with @Autowired.
         * @param instance                - The instance of the class that we are trying to autowire.
         */
        private static void setAutowiredFieldInstances(ComponentModel componentModel, Object[] autowiredFieldInstances, Object instance) throws IllegalAccessException {
            final Field[] autowireAnnotatedFields = componentModel.getAutowireAnnotatedFields();
            for (int i = 0; i < autowireAnnotatedFields.length; i++) {
                autowireAnnotatedFields[i].set(instance, autowiredFieldInstances[i]);
            }
        }

        /**
         * If the component has a post construct method, invoke it.
         *
         * @param componentModel - This is the component model that we created in the previous step.
         * @param instance       -  The instance of the component that is being created.
         */
        private static void invokePostConstruct(ComponentModel componentModel, Object instance) {
            if (componentModel.getPostConstructMethod() != null) {
                try {
                    componentModel.getPostConstructMethod().invoke(instance);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new PostConstructException(e.getMessage(), e);
                }
            }
        }

        public static void createBeanInstance(ComponentBeanModel bean) {
            bean.setInstance(createNewInstance(bean));
        }

        public static Object createNewInstance(ComponentBeanModel bean) {
            final Method originMethod = bean.getOriginMethod();
            final Object rootInstance = bean.getRootComponent().getInstance();
            try {
                return originMethod.invoke(rootInstance);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new BeanInstantiationException(e.getMessage(), e);
            }
        }

        /**
         * If the component has a pre-destroy method, invoke it. After invoking pre-destroy method, set instance of the component is null
         *
         * @param component The component model that contains the instance to be destroyed.
         */
        public static void destroyInstance(ComponentModel component) throws PreDestroyExecutionException {
            if (component.getPreDestroyMethod() != null) {
                try {
                    component.getPreDestroyMethod().invoke(component.getActualInstance());
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new PreDestroyExecutionException(e.getMessage(), e);
                }
            }
            component.setInstance(null);
        }
    }
}
