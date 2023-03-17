package org.ioc.support;

import org.ioc.ComponentBeanModel;
import org.ioc.ComponentModel;
import org.ioc.DependencyParam;
import org.ioc.exception.BeanInstantiationException;
import org.ioc.exception.ComponentInstantiationException;
import org.ioc.exception.PostConstructException;
import org.ioc.exception.PreDestroyExecutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class HandlerInstantiation {
    private static final String INVALID_PARAMETERS_COUNT_MSG = "Invalid parameters count for '%s'.";

    public static void createInstance(ComponentModel componentModel) {
        componentModel.setInstance(createNewInstance(componentModel));
    }

    private static Object createNewInstance(ComponentModel componentModel) {
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

    private static Object createNewInstance(ComponentBeanModel bean) {
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
