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
        final Object[] constructorParams = componentModel.getResolvedConstructorParams().stream()
                .map(DependencyParam::getInstance)
                .toArray(Object[]::new);
        final Object[] fieldParams = componentModel.getResolvedFields().stream()
                .map(DependencyParam::getInstance)
                .toArray(Object[]::new);
        return createNewInstance(componentModel, constructorParams, fieldParams);
    }


    public static void createInstance(ComponentModel componentModel, Object[] constructorParams, Object[] autowiredFieldInstances) {
        componentModel.setInstance(createNewInstance(componentModel, constructorParams, autowiredFieldInstances));
    }

    private static Object createNewInstance(ComponentModel componentModel, Object[] constructorParams, Object[] autowiredFieldInstances) {
        final Constructor<?> constructor = componentModel.getTargetConstructor();
        if (constructor.getParameterCount() != constructorParams.length) {
            throw new ComponentInstantiationException(String.format(
                    INVALID_PARAMETERS_COUNT_MSG,
                    componentModel.getComponentType().getName()
            ));
        }
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

    private static void setAutowiredFieldInstances(ComponentModel componentModel, Object[] autowiredFieldInstances, Object instance) throws IllegalAccessException {
        final Field[] autowireAnnotatedFields = componentModel.getAutowireAnnotatedFields();
        for (int i = 0; i < autowireAnnotatedFields.length; i++) {
            autowireAnnotatedFields[i].set(instance, autowireAnnotatedFields[i]);
        }
    }

    private static void invokePostConstruct(ComponentModel componentModel, Object instance) {
        if (componentModel.getPostConstructMethod() == null) {
            return;
        }
        try {
            componentModel.getPostConstructMethod().invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PostConstructException(e.getMessage(), e);
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
