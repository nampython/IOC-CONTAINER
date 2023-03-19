package org.ioc.contex.factory;

import org.ioc.engine.ComponentBeanModel;
import org.ioc.engine.ComponentModel;
import org.ioc.contex.ApplicationContext;
import org.ioc.contex.enviroment.EnvironmentSource;
import org.ioc.engine.InstantiationComponentBean;
import org.ioc.exception.AlreadyInitializedException;
import org.ioc.exception.BeansException;
import org.ioc.exception.NoSuchBeanDefinitionException;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationContextInternal implements EnvironmentSource, ApplicationContext {
    private static final String ALREADY_INITIALIZED_MSG = "Dependency container already initialized.";
    private static final String COMPONENT_NOT_FOUND_FORMAT = "Component \"%s\" was not found.";
    private Set<Class<?>> allLocatedClasses;
    private List<ComponentModel> componentsAndBean;
    private boolean isInit;

    // This is the constructor for the ApplicationContextInternal class. It sets the `isInit` flag to false.
    public ApplicationContextInternal() {
        this.isInit = false;
    }

    /**
     * > If the `isInit` flag is set to true, that mean container initialize, throw an exception.
     * Otherwise, set the `allLocatedClasses` and `componentsAndBean`
     * fields to the values passed in, and set the `isInit` flag to true
     *
     * @param locatedClasses    - A collection of all the classes that were found in the classpath.
     * @param componentsAndBean - This is a collection of ComponentModel objects.
     */
    protected void init(Set<Class<?>> locatedClasses, List<ComponentModel> componentsAndBean) {
        if (this.isInit) {
            throw new AlreadyInitializedException(ALREADY_INITIALIZED_MSG);
        } else {
            this.allLocatedClasses = locatedClasses;
            this.componentsAndBean = componentsAndBean;
            this.isInit = true;
        }
    }


    /**
     * > Get the names of all the beans in the container
     *
     * @return - The names of the bean definitions.
     */
    @Override
    public String[] getBeanDefinitionNames() {
        String[] beanDefinitionNames = new String[this.getBeanDefinitionCount()];
        for (int i = 0; i < this.getBeanDefinitionCount(); i++) {
            beanDefinitionNames[i] = this.componentsAndBean.get(i).getComponentType().getSimpleName();
        }
        return beanDefinitionNames;
//        return this.componentsAndBean.stream()
//                .map(ComponentModel::getComponentType)
//                .map(Class::getSimpleName)
//                .toArray(String[]::new);
    }

    /**
     * > This function returns the number of beans in the application context
     *
     * @return - The number of components and beans in the application context.
     */
    @Override
    public int getBeanDefinitionCount() {
        return this.componentsAndBean.size();
    }

    /**
     * > It returns the instance of the bean with the given name
     *
     * @param beanName - The name of the bean to be obtained.
     * @return - The instance of the bean.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName) {
        ComponentModel requiredComponentModel = null;
        for (ComponentModel componentModel : this.componentsAndBean) {
            if (Objects.equals(componentModel.getInstanceName(), beanName)) {
                requiredComponentModel = componentModel;
                break;
            }
        }
        if (requiredComponentModel == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean named '%s' available", beanName));
        } else {
            return (T) requiredComponentModel.getInstance();
        }
//        ComponentModel componentModel = this.componentsAndBean.stream()
//                .filter(component -> Objects.equals(component.getInstanceName(), beanName))
//                .findFirst()
//                .orElse(null);
//        if (componentModel == null) {
//            throw new NoSuchBeanDefinitionException(String.format("No bean named '%s' available", beanName));
//        }
//        return (T) componentModel.getInstance();
    }


    /**
     * Get bean from the given class
     *
     * @param cls - The given class that gets Bean
     * @return - Bean instance
     */
    @Override
    public <T> T getBean(Class<?> cls) {
        return this.getBean(cls, null);
    }

    /**
     * Get bean from the type and name of the specified bean
     *
     * @param cls          - type of bean
     * @param instanceName - name of bean
     * @return - Instance Bean
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<?> cls, String instanceName) {
        ComponentModel defineBean = this.getDefineBean(cls, instanceName);
        if (defineBean != null) {
            return (T) defineBean.getInstance();
        } else if (cls.isAssignableFrom(this.getClass())) {
            return (T) this;
        } else {
            return null;
        }
    }

    /**
     * Get a component model from type
     *
     * @param cls - type of component
     * @return - Component model
     */
    @Override
    public ComponentModel getDefineBean(Class<?> cls) {
        return this.getDefineBean(cls, null);
    }

    /**
     * > If the component type is assignable from the component model's component type or the component model's instance,
     * and the instance name matches, then return the component model
     *
     * @param componentType - The type of the bean to retrieve.
     * @param instanceName  - The name of the bean to retrieve.
     * @return - A component model
     */
    @Override
    public ComponentModel getDefineBean(Class<?> componentType, String instanceName) {
        ComponentModel component = null;
        for (ComponentModel componentModel : this.componentsAndBean) {
            if (this.checkBeanType(componentType, instanceName, componentModel)) {
                component = componentModel;
                break;
            }
        }
        if (component == null) {
            throw new BeansException(String.format("No qualifying bean of type '%s' available", componentType.getName()));
        } else {
            return component;
        }
    }

    /**
     * This function returns a collection of all the classes that were found in the classpath.
     *
     * @return A collection of all the classes that have been located.
     */
    @Override
    public Collection<Class<?>> getClassLoader() {
        return this.allLocatedClasses;
    }


    /**
     * > This function returns a map of beans with the specified annotation
     *
     * @param annotation - The annotation class that you want to find.
     * @return - A map of beans with the annotation.
     */
    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation) {
        Map<String, Object> beanWithAnnotation = new HashMap<String, Object>();
        List<Object> instanceBeans = new ArrayList<>();
        for (ComponentModel componentModel : this.componentsAndBean) {
            if (componentModel.getAnnotation().annotationType() == annotation) {
                instanceBeans.add(componentModel.getInstance());
            }
        }
        for (Object instanceBean : instanceBeans) {
            beanWithAnnotation.put(instanceBean.getClass().getSimpleName().toLowerCase(), instanceBean);
        }
        return beanWithAnnotation;
    }

    /**
     * > We are filtering the list of components and bean by the annotation type and then mapping the component type to the
     * simple name of the class
     *
     * @param annotation - The annotation class to look for.
     * @return - An array of strings.
     */
    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotation) {
        List<String> beanNameForAnnotation = new ArrayList<>();
        for (ComponentModel componentModel : this.componentsAndBean) {
            if (componentModel.getAnnotation().annotationType() == annotation) {
                beanNameForAnnotation.add(componentModel.getComponentType().getSimpleName());
            }
        }
        return beanNameForAnnotation.toArray(String[]::new);
//        return this.componentsAndBean.stream()
//                .filter(component -> component.getAnnotation().annotationType() == annotation)
//                .map(ComponentModel::getComponentType)
//                .map(Class::getSimpleName)
//                .toArray(String[]::new);
    }

    /**
     * Gt a new bean with the given type
     * @param beanClass - Type of bean
     * @return - New bean
     */
    @Override
    public <T> T getNewBean(Class<?> beanClass) {
        return this.getNewBean(beanClass, null);
    }

    /**
     * It creates a new instance of the bean and returns it
     *
     * @param beanClass - The class of the bean you want to get a new instance of.
     * @param instanceName - The name of the bean instance.
     * @return - The new instance of the bean.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getNewBean(Class<?> beanClass, String instanceName) {
        ComponentModel componentModel = null;
        for (ComponentModel component : this.componentsAndBean) {
            if (checkBeanType(beanClass, instanceName, component)) {
                componentModel = component;
                break;
            }
        }
        if (componentModel == null) {
            throw new IllegalArgumentException(String.format(COMPONENT_NOT_FOUND_FORMAT, beanClass.getName()));
        } else {
            final Object oldInstance = componentModel.getActualInstance();
            componentModel.setInstance(oldInstance);
            if (componentModel instanceof ComponentBeanModel) {
                InstantiationComponentBean.HandlerInstantiation.createBeanInstance((ComponentBeanModel) componentModel);
            } else {
                InstantiationComponentBean.HandlerInstantiation.createInstance(componentModel);
            }
            final Object newInstance = componentModel.getActualInstance();
            return (T) newInstance;
        }
    }

    private boolean checkBeanType(Class<?> beanClass, String instanceName, ComponentModel componentModel) {
        final boolean isRequiredTypeAssignable = beanClass.isAssignableFrom(componentModel.getComponentType());
        final boolean isRequiredTypeAssignable2 = componentModel.getInstance() != null &&
                beanClass.isAssignableFrom(componentModel.getInstance().getClass());
        final boolean instanceNameMatches = instanceName == null || instanceName.equalsIgnoreCase(componentModel.getInstanceName());
        return (isRequiredTypeAssignable || isRequiredTypeAssignable2) && instanceNameMatches;
    }

    /**
     * > Return a list of all the components that implement the given class
     *
     * @param cls The class to search for implementations of.
     * @return A list of ComponentModel objects that are assignable from the class passed in.
     */
    @Override
    public Collection<ComponentModel> getImplementations(Class<?> cls) {
        return this.componentsAndBean.stream()
                .filter(sd -> cls.isAssignableFrom(sd.getComponentType()))
                .collect(Collectors.toList());
    }

    /**
     * > Get all the components that have the specified annotation
     *
     * @param annotation The annotation class to be searched
     * @return A collection of ComponentModel objects that have the annotation specified.
     */
    @Override
    public Collection<ComponentModel> getDefineBeansWithAnnotation(Class<? extends Annotation> annotation) {
        return this.componentsAndBean.stream()
                .filter(sd -> sd.getAnnotation() != null && sd.getAnnotation().annotationType() == annotation)
                .collect(Collectors.toList());
    }

    /**
     * > This function returns a collection of all the components and beans that are defined in the application
     *
     * @return - A collection of ComponentModel objects.
     */
    @Override
    public Collection<ComponentModel> getAllDefineBean() {
        return this.componentsAndBean;
    }

    /**
     * Update the bean instance for the given class.
     *
     * @param componentInstance The instance of the component to be updated.
     */
    @Override
    public void updateBeanInstance(Object componentInstance) {
        this.updateBeanInstance(componentInstance.getClass(), componentInstance);
    }

    /**
     * If the component instance is not null, then update the bean instance.
     *
     * @param cls The class of the bean to be updated.
     * @param componentInstance The instance of the component to be updated.
     */
    @Override
    public void updateBeanInstance(Class<?> cls, Object componentInstance) {
        this.updateBeanInstance(cls, componentInstance, true);
    }

    /**
     * If the component is found, destroy the old instance and set the new instance.
     *
     * @param cls The class of the component to be updated
     * @param componentInstance The new instance of the component
     * @param destroyOldInstance Whether to destroy the old instance.
     */
    @Override
    public void updateBeanInstance(Class<?> cls, Object componentInstance, boolean destroyOldInstance) {
        final ComponentModel componentModel = this.getDefineBean(cls, null);
        if (componentModel == null) {
            throw new IllegalArgumentException(String.format(COMPONENT_NOT_FOUND_FORMAT, cls.getName()));
        } else {
            if (destroyOldInstance) {
                InstantiationComponentBean.HandlerInstantiation.destroyInstance(componentModel);
            }
            componentModel.setInstance(componentInstance);
        }
    }

    @Override
    public void reload(ComponentModel componentModel) {
        InstantiationComponentBean.HandlerInstantiation.destroyInstance(componentModel);
        final Object newInstance = this.getNewBean(componentModel.getComponentType(), componentModel.getInstanceName());
        componentModel.setInstance(newInstance);
    }

    @Override
    public void reload(Class<?> cls) {
        ComponentModel defineBean = this.getDefineBean(cls, null);
        if (defineBean == null) {
            throw new IllegalArgumentException(String.format(COMPONENT_NOT_FOUND_FORMAT, cls));
        }
        this.reload(defineBean);
    }
}
