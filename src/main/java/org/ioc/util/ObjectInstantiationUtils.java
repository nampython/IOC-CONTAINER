package org.ioc.util;

import org.ioc.ComponentModel;
import org.ioc.DependencyParam;
import org.ioc.exception.ComponentInstantiationException;
import org.ioc.exception.PostConstructException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ObjectInstantiationUtils {

    private static final String INVALID_PARAMETERS_COUNT_MSG = "Invalid parameters count for '%s'.";

    public static Object createNewInstance(ComponentModel componentModel) {
        final Object[] constructorParams = componentModel.getResolvedConstructorParams().stream()
                .map(DependencyParam::getInstance)
                .toArray(Object[]::new);

        final Object[] fieldParams = componentModel.getResolvedFields().stream()
                .map(DependencyParam::getInstance)
                .toArray(Object[]::new);

        return createNewInstance(componentModel, constructorParams, fieldParams);
    }

    public static Object createNewInstance(ComponentModel componentModel, Object[] constructorParams, Object[] fieldAutowiredParams) {
        Constructor<?> constructor = componentModel.getTargetConstructor();
        if(constructor.getParameterCount() != constructorParams.length) {
            throw new ComponentInstantiationException(String.format(
                    INVALID_PARAMETERS_COUNT_MSG,
                    componentModel.getComponentType().getName()
            ));
        }
        try {
            final Object instance = constructor.newInstance(constructorParams);
            componentModel.setInstance(instance);
            setAutowiredFieldInstances(componentModel, fieldAutowiredParams, instance);
            invokePostConstruct(componentModel, instance);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new ComponentInstantiationException(e.getMessage(), e);
        }
        return null;
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