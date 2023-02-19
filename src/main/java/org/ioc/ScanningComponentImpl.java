package org.ioc;

import org.ioc.configuration.ScanningConfiguration;
import org.ioc.exception.ClassLocationException;
import org.ioc.support.HandlerAnnotation;
import org.ioc.support.HandlerGeneric;
import org.ioc.stereotype.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class ScanningComponentImpl implements ScanningComponent {
    private final ScanningConfiguration scanningConfiguration;

    public ScanningComponentImpl(ScanningConfiguration scanningConfiguration) {
        this.scanningConfiguration = scanningConfiguration;
        this.init();
    }

    @Override
    public Set<ComponentModel> mappingComponents(Set<Class<?>> locatedClass) {
        final Map<Class<?>, Annotation> componentClass = this.filterServiceClasses(locatedClass);
        final Set<ComponentModel> componentStorage = new HashSet<>();
        final Map<Class<? extends Annotation>, ComponentModel> aspectHandlerServices = new HashMap<>();

        for (Map.Entry<Class<?>, Annotation> component : componentClass.entrySet()) {
            final Class<?> clsComponent = component.getKey();
            final Annotation annotationComponent = component.getValue();
            Constructor<?> constructor = this.handlerConstructor(clsComponent);
            String nameInstance = this.handlerNameInstance(clsComponent.getAnnotations());
            Method postConstructMethod = this.handlerMethodPostAndPre(clsComponent, PostConstruct.class);
            Method preDestroyMethod = this.handlerMethodPostAndPre(clsComponent, PreDestroy.class);
            ScopeType scopeType = this.handlerScopeType(clsComponent);
            List<Field> fieldWithAutowired = this.handlerFieldWithAutowired(clsComponent, new ArrayList<>());
            final ComponentModel componentModel = new ComponentModel(
                    clsComponent,
                    annotationComponent,
                    constructor,
                    nameInstance,
                    postConstructMethod,
                    preDestroyMethod,
                    scopeType,
                    fieldWithAutowired.toArray(new Field[0]));
            this.maybeAddAspectHandlerService(componentModel, aspectHandlerServices);
            componentModel.setBeans(this.handlerBeans(componentModel));
            // ???
            this.notifyServiceDetailsCreated(componentModel);
            componentStorage.add(componentModel);
        }
        this.applyAspectHandlerComponents(aspectHandlerServices, componentStorage);
        return componentStorage;
    }


    private String handlerNameInstance(Annotation[] annotations) {
        if (!HandlerAnnotation.isAnnotationPresent(annotations, NamedInstance.class)) {
            return null;
        }
        final Annotation annotation = HandlerAnnotation.getAnnotation(annotations, NamedInstance.class);
        assert annotation != null;
        final Method method;
        try {
            method = annotation.annotationType().getMethod("value");
            return method.invoke(annotation).toString();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyAspectHandlerComponents(Map<Class<? extends Annotation>, ComponentModel> aspectHandlerServices, Set<ComponentModel> componentStorage) {
        if (aspectHandlerServices.isEmpty()) {
            return;
        }
        for (ComponentModel componentModel : componentStorage) {
            final Map<Method, List<MethodAspectHandlerDto>> aspectsPerMethod = new HashMap<>();
            for (Method method : componentModel.getComponentType().getDeclaredMethods()) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (aspectHandlerServices.containsKey(annotation.annotationType())) {
                        aspectsPerMethod.putIfAbsent(method, new ArrayList<>());
                        aspectsPerMethod.get(method).add(new MethodAspectHandlerDto(
                                aspectHandlerServices.get(annotation.annotationType()),
                                annotation.annotationType()
                        ));
                    }
                }
            }
            if (aspectsPerMethod.isEmpty()) {
                continue;
            }
            componentModel.setScopeType(ScopeType.PROXY);
            componentModel.setMethodAspectHandlers(aspectsPerMethod);
        }
    }

    private void notifyServiceDetailsCreated(ComponentModel componentModel) {

    }


    private void maybeAddAspectHandlerService(ComponentModel componentModel, Map<Class<? extends Annotation>, ComponentModel> aspectHandlerServices) {
        if (!ComponentMethodAspectHandler.class.isAssignableFrom(componentModel.getComponentType())) {
            return;
        }

        final Type[] genericTypeArguments = HandlerGeneric.getGenericTypeArguments(
                componentModel.getComponentType(),
                ComponentMethodAspectHandler.class
        );

        if (genericTypeArguments == null || genericTypeArguments.length != 1) {
            throw new ClassLocationException(String.format(
                    "Error while loading Aspect Handler class '%s'.", componentModel.getComponentType()
            ));
        }

        aspectHandlerServices.put((Class<? extends Annotation>) genericTypeArguments[0], componentModel);
    }

    private Collection<ComponentBeanModel> handlerBeans(ComponentModel componentModel) {
        final Set<Class<? extends Annotation>> beanAnnotations = this.scanningConfiguration.getBeanAnnotations();
        final Set<ComponentBeanModel> beans = new HashSet<>();
        for (Method method : componentModel.getComponentType().getDeclaredMethods()) {

            boolean isNotBean = method.getParameterCount() != 0 || method.getReturnType() == void.class || method.getReturnType() == Void.class;
            if (isNotBean) {
                continue;
            }

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
        return beans;
    }

    private List<Field> handlerFieldWithAutowired(Class<?> clsComponent, List<Field> fields) {
        for (Field field : clsComponent.getDeclaredFields()) {
            if (HandlerAnnotation.isAnnotationPresent(field.getDeclaredAnnotations(), Autowired.class)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
//        if (cls.getSuperclass() != null) {
//            return this.findAutowireAnnotatedFields(cls.getSuperclass(), fields);
//        }
        return fields;
    }

    private ScopeType handlerScopeType(Class<?> clsComponent) {
        if(clsComponent.isAnnotationPresent(Scope.class)) {
            return clsComponent.getDeclaredAnnotation(Scope.class).value();
        }
        return ScopeType.DEFAULT_SCOPE;
    }


    private ScopeType handlerScopeType(Method method) {
        if (method.isAnnotationPresent(Scope.class)) {
            return method.getDeclaredAnnotation(Scope.class).value();
        }
        return ScopeType.DEFAULT_SCOPE;
    }
    private Method handlerMethodPostAndPre(Class<?> clsComponent, Class<? extends Annotation> requiredAnnotation) {
        for (Method declaredMethod : clsComponent.getDeclaredMethods()) {
            boolean isRequiredMethod = declaredMethod.getParameterCount() != 0 || (declaredMethod.getReturnType() != void.class
            && declaredMethod.getReturnType() != Void.class);

            if (isRequiredMethod) {
                continue;
            }
            if (HandlerAnnotation.isAnnotationPresent(declaredMethod.getDeclaredAnnotations(), requiredAnnotation)) {
                declaredMethod.setAccessible(true);
                return declaredMethod;
            }
        }
//        if (cls.getSuperclass() != null) {
//            return this.findVoidMethodWithZeroParamsAndAnnotations(annotation, cls.getSuperclass());
//        }
        return null;
    }

    /**
     * @param clsComponent
     * @return
     */
    private Constructor<?> handlerConstructor(Class<?> clsComponent) {
        for (Constructor<?> declaredConstructor : clsComponent.getDeclaredConstructors()) {
            if (HandlerAnnotation.isAnnotationPresent(declaredConstructor.getAnnotations(), Autowired.class)) {
                declaredConstructor.setAccessible(true);
                return declaredConstructor;
            }
        }
        return clsComponent.getDeclaredConstructors()[0];
    }

    private Map<Class<?>, Annotation> filterServiceClasses(Set<Class<?>> scannedClasses) {
        final Set<Class<? extends Annotation>> componentsAnnotations = this.scanningConfiguration.getComponentAnnotations();
        final Map<Class<?>, Annotation> locatedClass = new HashMap<>();


        for (Class<?> cls : scannedClasses) {
            if (cls.isInterface() || cls.isEnum() || cls.isAnnotation()) {
                continue;
            }
            for (Annotation annotation : cls.getAnnotations()) {
                if (componentsAnnotations.contains(annotation.annotationType())) {
                    locatedClass.put(cls, annotation);
                    break;
                }
            }
        }

//        Map<Class<?>, Class<? extends Annotation>> additionalClasses = this.configuration.getAdditionalClasses();
//        additionalClasses.forEach(
//                (cls, a) -> {
//                    Annotation annotation = null;
//                    if (a != null && cls.isAnnotationPresent(a)) {
//                        annotation = cls.getAnnotation(a);
//                    }
//
//                    locatedClasses.put(cls, annotation);
//                });
        return locatedClass;
    }

    public void init() {
        this.scanningConfiguration.getComponentAnnotations().add(Component.class);
        this.scanningConfiguration.getBeanAnnotations().add(Bean.class);
    }
}
