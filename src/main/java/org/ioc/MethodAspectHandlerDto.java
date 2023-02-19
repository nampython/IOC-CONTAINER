package org.ioc;


import java.lang.annotation.Annotation;

public class MethodAspectHandlerDto {

    /**
     * Service details of the aspect handler.
     */
    private final ComponentModel componentModel;

    /**
     * Registered annotation
     */
    private final Class<? extends Annotation> annotation;

    public MethodAspectHandlerDto(ComponentModel componentModel, Class<? extends Annotation> annotation) {
        this.componentModel = componentModel;
        this.annotation = annotation;
    }

    public Class<? extends Annotation> getAnnotation() {
        return this.annotation;
    }

    public ComponentModel getComponentModel() {
        return this.componentModel;
    }
}
