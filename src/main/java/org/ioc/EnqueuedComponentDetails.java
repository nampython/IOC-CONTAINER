package org.ioc;

import org.ioc.support.HandlerAnnotation;
import org.ioc.stereotype.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class EnqueuedComponentDetails {
    private final ComponentModel componentModel;
    private final LinkedList<DependencyParam> constructorParams;
    private final LinkedList<DependencyParam> fieldDependencies;
    private Object[] constructorInstances;
    private Object[] fieldInstances;

    public EnqueuedComponentDetails(ComponentModel componentModel) {
        this.componentModel = componentModel;
        this.constructorParams = new LinkedList<>();
        this.fieldDependencies = new LinkedList<>();
        this.fillConstructorParams();
        this.fillFieldDependencyTypes();
    }

    /**
     * This function iterates through the constructor parameters of the component and creates a dependency parameter for
     * each one
     */
    private void fillConstructorParams() {
        for (Parameter parameter : this.componentModel.getTargetConstructor().getParameters()) {
            Class<?> type = parameter.getType();
            String instanceName = this.getInstanceName(parameter.getDeclaredAnnotations());
            Annotation[] annotations = parameter.getDeclaredAnnotations();
            Type parameterizedType = parameter.getParameterizedType();
            this.constructorParams.add(this.createDependencyParam(type, instanceName, annotations, parameterizedType));
        }
    }

    /**
     * This function is used to fill the fieldDependencies list with the dependencies of the fields of the component
     */
    private void fillFieldDependencyTypes() {
        for (Field autowireAnnotatedField : this.componentModel.getAutowireAnnotatedFields()) {
            Class<?> type = autowireAnnotatedField.getType();
            String instanceName = this.getInstanceName(autowireAnnotatedField.getDeclaredAnnotations());
            Annotation[] annotations = autowireAnnotatedField.getDeclaredAnnotations();
            Type genericType = autowireAnnotatedField.getGenericType();
            this.fieldDependencies.add(this.createDependencyParam(type, instanceName, annotations, genericType));
        }
    }

    /**
     * It gets the value of the @Qualifier annotation
     *
     * @param annotations - The annotations of the field to be injected.
     * @return - The name of the instance.
     */
    private String getInstanceName(Annotation[] annotations) {
        final Annotation annotation = HandlerAnnotation.getAnnotation(annotations, Qualifier.class);
        if (annotation != null) {
            try {
                Method method = annotation.annotationType().getMethod("value");
                Object invoke = method.invoke(annotation);
                return invoke.toString();
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * If the type is a collection, create a DependencyParamCollection, otherwise create a DependencyParam
     *
     * @param type              - The type of the parameter.
     * @param instanceName      - The name of the instance to be injected.
     * @param annotations       - The annotations on the parameter.
     * @param parameterizedType - The type of the parameter.
     * @return - A DependencyParam object.
     */
    private DependencyParam createDependencyParam(Class<?> type, String instanceName, Annotation[] annotations, Type parameterizedType) {
        return Collection.class.isAssignableFrom(type) ? new DependencyParamCollection((ParameterizedType) parameterizedType, type, instanceName, annotations) : new DependencyParam(type, instanceName, annotations);
    }

    /**
     * > If the constructorInstances array is null, then create a new array of objects by mapping the constructorParams
     * stream to the instance of each dependency param
     *
     * @return - An array of objects that are the instances of the constructor parameters.
     */
    public Object[] getConstructorInstances() {
        if (this.constructorInstances == null) {
            this.constructorInstances = this.constructorParams.stream().map(DependencyParam::getInstance).toArray(Object[]::new);
        }
        return constructorInstances;
    }

    /**
     * > If the fieldInstances array is null, then create a new array of objects by mapping the fieldDependencies stream to
     * the instance of each dependency parameter, and return the array
     *
     * @return - An array of objects that are the instances of the field dependencies.
     */
    public Object[] getFieldInstances() {
        if (this.fieldInstances == null) {
            this.fieldInstances = this.fieldDependencies.stream().map(DependencyParam::getInstance).toArray(Object[]::new);
        }
        return this.fieldInstances;
    }

    /**
     * > This function returns the constructor parameters of the class
     *
     * @return - A LinkedList of DependencyParam objects.
     */
    public LinkedList<DependencyParam> getConstructorParams() {
        return constructorParams;
    }

    /**
     * > This function returns a list of dependencies for the field
     *
     * @return - A list of dependencies.
     */
    public LinkedList<DependencyParam> getFieldDependencies() {
        return fieldDependencies;
    }

    /**
     * This function returns the component model.
     *
     * @return - The componentModel
     */
    public ComponentModel getComponentModel() {
        return componentModel;
    }
}
