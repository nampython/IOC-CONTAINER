package org.ioc.engine;

import org.ioc.*;
import org.ioc.configuration.ScanningConfiguration;
import org.ioc.exception.ClassLocationException;
import org.ioc.support.HandlerAnnotation;
import org.ioc.support.HandlerGeneric;
import org.ioc.stereotype.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * * Iterates all located classes and looks for classes with @{@link Component}
 * * annotation or one provided by the client and then collects data for that class.
 */
public class LoaderComponent extends SettingComponent {
    private final ScanningConfiguration scanningConfiguration;

    public LoaderComponent(ScanningConfiguration scanningConfiguration) {
        this.scanningConfiguration = scanningConfiguration;
        this.init();
    }

    /**
     * Set all attributes into a model called Component. These attributes include
     * <ul>
     *     <li>{@link Class} - specified class</li>
     *     <li>{@link Annotation} -  @{@link Component} or Custom Component</li>
     *     <li>{@link Constructor} - Constructor that have @{@link Autowired} -> Constructor injection</li>
     *     <li>{@link Method} - Method that having @{@link PostConstruct} or @{@link PreDestroy}</li>
     *     <li>{@link String} - Name of specified instance</li>
     *     <li>{@link ScopeType} - Singleton or Prototype</li>
     *     <li>{@link Field} - List of all field that having @{@link Autowired} -> Field injection</li>
     * </ul>
     *
     * @param locatedClass - All classes that located in project
     * @return - Set of Component Model
     */
    @Override
    public Set<ComponentModel> mappingComponent(Set<Class<?>> locatedClass) {
        final Map<Class<?>, Annotation> onlyForComponentClass = this.filterComponentClasses(locatedClass);
        final Set<ComponentModel> componentStorage = new HashSet<>();
        final Map<Class<? extends Annotation>, ComponentModel> aspectHandlerServices = new HashMap<>();

        for (Map.Entry<Class<?>, Annotation> component : onlyForComponentClass.entrySet()) {
            final ComponentModel componentModel = getComponentModel(component);
            this.maybeAddAspectHandlerService(componentModel, aspectHandlerServices);
            componentModel.setBeans(this.handlerBeans(componentModel));
            this.notifyComponentDetailsCreated(componentModel);
            componentStorage.add(componentModel);
        }
        this.applyAspectHandlerComponents(aspectHandlerServices, componentStorage);
        return componentStorage;
    }

    /**
     * It takes a class and an annotation and returns a ComponentModel object
     *
     * @param component the class and annotation of the component
     * @return ComponentModel
     */
    @NotNull
    private ComponentModel getComponentModel(Map.Entry<Class<?>, Annotation> component) {
        final Class<?> clsComponent = component.getKey();
        final Annotation annotationComponent = component.getValue();
        Constructor<?> constructor = this.handlerConstructor(clsComponent);
        String nameInstance = this.handlerNameInstance(clsComponent.getAnnotations());
        Method postConstructMethod = this.handlerVoidMethodWithZeroParamsAndAnnotations(clsComponent, PostConstruct.class);
        Method preDestroyMethod = this.handlerVoidMethodWithZeroParamsAndAnnotations(clsComponent, PreDestroy.class);
        ScopeType scopeType = this.handlerScopeType(clsComponent);
        List<Field> fieldWithAutowired = this.handlerFieldWithAutowired(clsComponent, new ArrayList<>());
        return new ComponentModel(
                clsComponent, annotationComponent,
                constructor, nameInstance,
                postConstructMethod, preDestroyMethod,
                scopeType, fieldWithAutowired.toArray(new Field[0]));
    }

    /**
     * Filter all classes that located in the project. Convert them to {@link Map} with the key is a class and value is the annotation @{@link Component}
     * for this class.
     *
     * @param scannedClasses All classes scanned
     * @return Classes with annotation
     */
    private Map<Class<?>, Annotation> filterComponentClasses(Set<Class<?>> scannedClasses) {
        final Set<Class<? extends Annotation>> componentsAnnotations = this.scanningConfiguration.getComponentAnnotations();
        final Map<Class<?>, Annotation> classWithComponent = new HashMap<>();
        for (Class<?> cls : scannedClasses) {
            if (!cls.isInterface() && !cls.isEnum() && !cls.isAnnotation()) {
                for (Annotation annotation : cls.getAnnotations()) {
                    if (componentsAnnotations.contains(annotation.annotationType())) {
                        classWithComponent.put(cls, annotation);
                        break;
                    }
                }
            }
        }
        Map<Class<?>, Class<? extends Annotation>> additionalClasses = this.scanningConfiguration.getAdditionalClasses();
        for (Map.Entry<Class<?>, Class<? extends Annotation>> entry : additionalClasses.entrySet()) {
            Annotation annotation = null;
            Class<?> cls = entry.getKey();
            Class<? extends Annotation> annotationCls = entry.getValue();
            if (annotationCls != null && cls.isAnnotationPresent(annotationCls)) {
                annotation = cls.getAnnotation(annotationCls);
            }
            classWithComponent.put(cls, annotation);
        }
//        additionalClasses.forEach(
//                (cls, a) -> {
//                    Annotation annotation = null;
//                    if (a != null && cls.isAnnotationPresent(a)) {
//                        annotation = cls.getAnnotation(a);
//                    }
//                    classWithComponent.put(cls, annotation);
//                });
        return classWithComponent;
    }

    /**
     * Find a suitable constructor with annotation {@link Autowired}. There are one special case that need to check.
     * In case is not used @{@link Autowired}, there are {@link AliasFor} annotation instead.
     *
     * @param classComponent Class of component
     * @return java.lang.reflect.Constructor
     */
    private Constructor<?> handlerConstructor(Class<?> classComponent) {
        for (Constructor<?> declaredConstructor : classComponent.getDeclaredConstructors()) {
            Annotation[] annotations = declaredConstructor.getAnnotations();
            if (HandlerAnnotation.isAnnotationPresent(annotations, Autowired.class)) {
                declaredConstructor.setAccessible(true);
                return declaredConstructor;
            }
        }
        return classComponent.getDeclaredConstructors()[0];
    }

    /**
     * For each Component class, instead of using default name we use {@link NamedInstance} to define a name for Component.
     *
     * @param annotations - All annotations of component class
     * @return - Name of instance component
     */
    private String handlerNameInstance(Annotation[] annotations) {
        if (HandlerAnnotation.isAnnotationPresent(annotations, NamedInstance.class)) {
            final Annotation annotation = HandlerAnnotation.getAnnotation(annotations, NamedInstance.class);
            assert annotation != null;
            final Method method;
            try {
                method = annotation.annotationType().getMethod("value");
                return method.invoke(annotation).toString();
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Handler methods with @{@link PostConstruct} or @{@link PreDestroy}
     *
     * @param clsComponent       - Class wants to find the zero method
     * @param requiredAnnotation - {@link PostConstruct} or {@link PreDestroy}
     * @return - Method
     */
    private Method handlerVoidMethodWithZeroParamsAndAnnotations(Class<?> clsComponent, Class<? extends Annotation> requiredAnnotation) {
        for (Method declaredMethod : clsComponent.getDeclaredMethods()) {
            boolean isRequiredMethod = declaredMethod.getParameterCount() == 0 ||
                    (declaredMethod.getReturnType() == Void.class && declaredMethod.getReturnType() == Void.TYPE);
            boolean annotationPresent = HandlerAnnotation.isAnnotationPresent(declaredMethod.getDeclaredAnnotations(), requiredAnnotation);
            if (isRequiredMethod && annotationPresent) {
                declaredMethod.setAccessible(true);
                return declaredMethod;
            }
        }
        if (clsComponent.getSuperclass() != null) {
            return this.handlerVoidMethodWithZeroParamsAndAnnotations(clsComponent.getSuperclass(), requiredAnnotation);
        }
        return null;
    }

    /**
     * Find scope type of specified class
     *
     * @param clsComponent - Class that want to find ScopeType
     * @return - {@link ScopeType}
     */
    private ScopeType handlerScopeType(Class<?> clsComponent) {
        return clsComponent.isAnnotationPresent(Scope.class) ? clsComponent.getDeclaredAnnotation(Scope.class).value() : ScopeType.DEFAULT_SCOPE;
    }

    /**
     * Find scope type of specified method
     *
     * @param method - Method that want to find ScopeType
     * @return - {@link ScopeType}
     */
    private ScopeType handlerScopeType(Method method) {
        if (method.isAnnotationPresent(Scope.class)) {
            return method.getDeclaredAnnotation(Scope.class).value();
        }
        return ScopeType.DEFAULT_SCOPE;
    }

    /**
     * Handler all field of a specified class with @{@link Autowired}
     *
     * @param clsComponent - Class that want to find field with @{@link Autowired}
     * @param fields       - Store all fields
     * @return - List of field
     */
    private List<Field> handlerFieldWithAutowired(Class<?> clsComponent, List<Field> fields) {
        for (Field field : clsComponent.getDeclaredFields()) {
            if (HandlerAnnotation.isAnnotationPresent(field.getDeclaredAnnotations(), Autowired.class)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        if (clsComponent.getSuperclass() != null) {
            return this.handlerFieldWithAutowired(clsComponent.getSuperclass(), fields);
        } else {
            return fields;
        }
    }

    /**
     * It takes a map of aspect handler components and a set of component models, and for each component model, it checks
     * if any of its methods have any annotations that are also present in the map of aspect handler components. If so, it
     * adds the component model to the set of component models that will be proxied, and it adds the aspect handler
     * component to the set of aspect handler components that will be applied to the component model
     *
     * @param aspectHandlerComponents - a map of all the aspect handler components, where the key is the annotation type and
     *                                the value is the component model.
     * @param componentStorage        -  a set of all components that have been found in the application.
     */
    private void applyAspectHandlerComponents(Map<Class<? extends Annotation>, ComponentModel> aspectHandlerComponents, Set<ComponentModel> componentStorage) {
        if (!aspectHandlerComponents.isEmpty()) {
            for (ComponentModel componentModel : componentStorage) {
                final Map<Method, List<MethodAspectHandlerDto>> aspectsPerMethod = new HashMap<>();
                for (Method method : componentModel.getComponentType().getDeclaredMethods()) {
                    for (Annotation annotation : method.getDeclaredAnnotations()) {
                        if (aspectHandlerComponents.containsKey(annotation.annotationType())) {
                            aspectsPerMethod.putIfAbsent(method, new ArrayList<>());
                            aspectsPerMethod.get(method).add(new MethodAspectHandlerDto(
                                    aspectHandlerComponents.get(annotation.annotationType()),
                                    annotation.annotationType()
                            ));
                        }
                    }
                }
                if (!aspectsPerMethod.isEmpty()) {
                    componentModel.setScopeType(ScopeType.PROXY);
                    componentModel.setMethodAspectHandlers(aspectsPerMethod);
                }
            }
        }
    }

    /**
     * Iterates all events provided by the user and calls them with the newly mapped service and beans.
     *
     * @param componentModel - newly mapped service.
     */
    private void notifyComponentDetailsCreated(ComponentModel componentModel) {
        for (ComponentDetailsCreated callback : this.scanningConfiguration.getServiceDetailsCreatedCallbacks()) {
            callback.componentDetailsCreated(componentModel);
            for (ComponentBeanModel bean : componentModel.getBeans()) {
                callback.componentDetailsCreated(bean);
            }
        }
    }


    /**
     * If the component is a subclass of `ComponentMethodAspectHandler`, then add it to the `aspectHandlerServices` map
     *
     * @param componentModel        - The component model of the class being loaded.
     * @param aspectHandlerServices - a map of aspect handler services, keyed by the annotation type
     */
    @SuppressWarnings("unchecked")
    private void maybeAddAspectHandlerService(ComponentModel componentModel, Map<Class<? extends Annotation>, ComponentModel> aspectHandlerServices) {
        if (ComponentMethodAspectHandler.class.isAssignableFrom(componentModel.getComponentType())) {
            final Type[] genericTypeArguments = HandlerGeneric.getGenericTypeArguments(componentModel.getComponentType(), ComponentMethodAspectHandler.class);
            if (genericTypeArguments != null && genericTypeArguments.length == 1) {
                aspectHandlerServices.put((Class<? extends Annotation>) genericTypeArguments[0], componentModel);
            } else {
                throw new ClassLocationException(String.format(
                        "Error while loading Aspect Handler class '%s'.", componentModel.getComponentType()
                ));
            }
        }
    }


    /**
     * It takes a component model and returns a collection of bean models
     *
     * @param componentModel - The component model that is being scanned.
     * @return - A collection of ComponentBeanModel objects.
     */
    private Collection<ComponentBeanModel> handlerBeans(ComponentModel componentModel) {
        final Set<Class<? extends Annotation>> beanAnnotations = this.scanningConfiguration.getBeanAnnotations();
        final Set<ComponentBeanModel> beans = new HashSet<>();

        for (Method method : componentModel.getComponentType().getDeclaredMethods()) {
            boolean isBean = method.getParameterCount() == 0 && method.getReturnType() != void.class && method.getReturnType() != Void.TYPE;
            if (isBean) {
                Annotation[] annotations = method.getDeclaredAnnotations();
                for (Class<? extends Annotation> beanAnnotation : beanAnnotations) {
                    if (HandlerAnnotation.isAnnotationPresent(annotations, beanAnnotation)) {
                        method.setAccessible(true);
                        beans.add(new ComponentBeanModel(
                                method.getReturnType(),
                                method,
                                componentModel,
                                HandlerAnnotation.getAnnotation(annotations, beanAnnotation),
                                this.handlerScopeType(method),
                                this.handlerNameInstance(method.getDeclaredAnnotations())
                        ));
                        break;
                    }
                }
            }
        }
        return beans;
    }

    /**
     * > The function adds the `@Component` and `@Bean` annotations to the list of annotations that are scanned for by the
     * `SpringComponentScanner` class
     */
    @Override
    public void init() {
        this.scanningConfiguration.getComponentAnnotations().add(Component.class);
        this.scanningConfiguration.getBeanAnnotations().add(Bean.class);
    }
}
