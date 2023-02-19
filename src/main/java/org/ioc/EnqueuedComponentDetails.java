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

    private Object[] constructorInstances;
    private final LinkedList<DependencyParam> fieldDependencies;

    private Object[] fieldInstances;

    public EnqueuedComponentDetails(ComponentModel componentModel) {
        this.componentModel = componentModel;
        this.constructorParams = new LinkedList<>();
        this.fieldDependencies = new LinkedList<>();
        this.fillConstructorParams();
        this.fillFieldDependencyTypes();
    }

    private void fillFieldDependencyTypes() {
        for (Field autowireAnnotatedField : this.componentModel.getAutowireAnnotatedFields()) {
            Class<?> type = autowireAnnotatedField.getType();
            String instanceName = this.getInstanceName(autowireAnnotatedField.getDeclaredAnnotations());
            Annotation[] annotations = autowireAnnotatedField.getDeclaredAnnotations();
            Type genericType = autowireAnnotatedField.getGenericType();
            this.fieldDependencies.add(this.createDependencyParam(
                    type,
                    instanceName,
                    annotations,
                    genericType
            ));
        }
    }

    private void fillConstructorParams() {
        for (Parameter parameter : this.componentModel.getTargetConstructor().getParameters()) {
            Class<?> type = parameter.getType();
            String instanceName = this.getInstanceName(parameter.getDeclaredAnnotations());
            Annotation[] annotations = parameter.getDeclaredAnnotations();
            Type parameterizedType = parameter.getParameterizedType();
            this.constructorParams.add(this.createDependencyParam(
                    type,
                    instanceName,
                    annotations,
                    parameterizedType
            ));
        }
    }


    private DependencyParam createDependencyParam(Class<?> type,
                                                  String instanceName,
                                                  Annotation[] annotations,
                                                  Type parameterizedType) {
        if (Collection.class.isAssignableFrom(type)) {
            return new DependencyParamCollection((ParameterizedType) parameterizedType, type, instanceName, annotations);
        }
        return new DependencyParam(type, instanceName, annotations);
    }

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

    public Object[] getConstructorInstances() {
        if (this.constructorInstances == null) {
            this.constructorInstances = this.constructorParams.stream()
                    .map(DependencyParam::getInstance)
                    .toArray(Object[]::new);
        }
        return constructorInstances;
    }
    public Object[] getFieldInstances() {
        if (this.fieldInstances == null) {
            this.fieldInstances = this.fieldDependencies.stream()
                    .map(DependencyParam::getInstance)
                    .toArray(Object[]::new);
        }
        return this.fieldInstances;
    }
    public LinkedList<DependencyParam> getConstructorParams() {
        return constructorParams;
    }

    public LinkedList<DependencyParam> getFieldDependencies() {
        return fieldDependencies;
    }

    public ComponentModel getComponentModel() {
        return componentModel;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EnqueuedComponentDetails{");
        sb.append("componentModel=").append(componentModel);
        sb.append(", constructorParams=").append(constructorParams);
        sb.append(", constructorInstances=").append(constructorInstances == null ? "null" : Arrays.asList(constructorInstances).toString());
        sb.append(", fieldDependencies=").append(fieldDependencies);
        sb.append(", fieldInstances=").append(fieldInstances == null ? "null" : Arrays.asList(fieldInstances).toString());
        sb.append('}');
        return sb.toString();
    }
}
