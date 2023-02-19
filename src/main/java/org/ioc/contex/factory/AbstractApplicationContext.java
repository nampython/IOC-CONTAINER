package org.ioc.contex.factory;

import org.ioc.ComponentBeanModel;
import org.ioc.ComponentModel;
import org.ioc.contex.ApplicationContext;
import org.ioc.contex.enviroment.EnvironmentSource;
import org.ioc.exception.AlreadyInitializedException;
import org.ioc.exception.BeansException;
import org.ioc.exception.NoSuchBeanDefinitionException;
import org.ioc.support.HandlerInstantiation;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractApplicationContext implements EnvironmentSource, ApplicationContext {
    private static final String ALREADY_INITIALIZED_MSG = "Dependency container already initialized.";
    private static final String COMPONENT_NOT_FOUND_FORMAT = "Component \"%s\" was not found.";
    private Collection<Class<?>> allLocatedClasses;
    private Collection<ComponentModel> componentsAndBean;
    private boolean isInit;

    public AbstractApplicationContext(Collection<Class<?>> allLocatedClasses, Collection<ComponentModel> componentsAndBean) {
        this.allLocatedClasses = allLocatedClasses;
        this.componentsAndBean = componentsAndBean;
        this.isInit = false;
    }

    protected void init(Collection<Class<?>> locatedClasses, Collection<ComponentModel> componentsAndBean) {
        if (this.isInit) {
            throw new AlreadyInitializedException(ALREADY_INITIALIZED_MSG);
        }
        this.allLocatedClasses = locatedClasses;
        this.componentsAndBean = componentsAndBean;
        this.isInit = true;
    }

    /**
     * @return
     */
    @Override
    public String[] getBeanDefinitionNames() {
        return this.componentsAndBean.stream()
                .map(ComponentModel::getComponentType)
                .map(Class::getSimpleName)
                .toArray(String[]::new);
    }

    /**
     * @return
     */
    @Override
    public int getBeanDefinitionCount() {
        return this.componentsAndBean.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName) {
        ComponentModel componentModel = this.componentsAndBean.stream()
                .filter(component -> Objects.equals(component.getInstanceName(), beanName))
                .findFirst()
                .orElse(null);
        if (componentModel == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean named '%s' available", beanName));
        }
        return (T) componentModel.getInstance();
    }


    @Override
    public <T> T getBean(Class<?> cls) {
        return this.getBean(cls, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<?> cls, String instanceName) {
        ComponentModel defineBean = this.getDefineBean(cls, instanceName);
        if (defineBean != null) {
            return (T) defineBean.getInstance();
        }
//        if (cls.isAssignableFrom(this.getClass())) {
//            return (T) this;
//        }
        return null;
    }

    @Override
    public ComponentModel getDefineBean(Class<?> cls) {
        return this.getDefineBean(cls, null);
    }

    @Override
    public ComponentModel getDefineBean(Class<?> cls, String instanceName) {
        ComponentModel component = this.componentsAndBean.stream()
                .filter(componentModel -> {
                    final boolean isRequiredTypeAssignable = cls.isAssignableFrom(componentModel.getComponentType());
                    final boolean isRequiredTypeAssignable2 = componentModel.getInstance() != null &&
                            cls.isAssignableFrom(componentModel.getInstance().getClass());
                    final boolean instanceNameMatches = instanceName == null || instanceName.equalsIgnoreCase(componentModel.getInstanceName());
                    return (isRequiredTypeAssignable || isRequiredTypeAssignable2) && instanceNameMatches;
                })
                .findFirst().orElse(null);
        if (component == null) {
            throw new BeansException(String.format("No qualifying bean of type '%s' available", cls.getName()));
        }
        return component;
    }

    @Override
    public Collection<Class<?>> getClassLoader() {
        return this.allLocatedClasses;
    }


    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation) {
        Map<String, Object> beanWithAnnotation = new HashMap<String, Object>();
        List<Object> instances = this.componentsAndBean.stream()
                .filter(component -> component.getAnnotation().annotationType() == annotation)
                .map(ComponentModel::getInstance)
                .collect(Collectors.toList());
        for (Object instance : instances) {
            beanWithAnnotation.put(instance.getClass().getSimpleName().toLowerCase(), instance);
        }
        return beanWithAnnotation;
    }

    /**
     *
     * @param annotation
     * @return
     */
    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotation) {
        return this.componentsAndBean.stream()
                .filter(component -> component.getAnnotation().annotationType() == annotation)
                .map(ComponentModel::getComponentType)
                .map(Class::getSimpleName)
                .toArray(String[]::new);
    }

    @Override
    public <T> T getNewBean(Class<?> beanClass) {
        return this.getNewBean(beanClass, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getNewBean(Class<?> beanClass, String instanceName) {
        final ComponentModel component = this.componentsAndBean.stream()
                .filter(componentModel -> {
                    final boolean isRequiredTypeAssignable = beanClass.isAssignableFrom(componentModel.getComponentType());
                    final boolean isRequiredTypeAssignable2 = componentModel.getInstance() != null &&
                            beanClass.isAssignableFrom(componentModel.getInstance().getClass());
                    final boolean instanceNameMatches = instanceName == null || instanceName.equalsIgnoreCase(componentModel.getInstanceName());
                    return (isRequiredTypeAssignable || isRequiredTypeAssignable2) && instanceNameMatches;
                })
                .findFirst().orElse(null);
        if (component == null) {
            throw new IllegalArgumentException(String.format(COMPONENT_NOT_FOUND_FORMAT, beanClass.getName()));
        }
        final Object oldInstance = component.getActualInstance();
        if (component instanceof ComponentBeanModel) {
            HandlerInstantiation.createBeanInstance((ComponentBeanModel)component);
        } else {
            HandlerInstantiation.createInstance(component);
        }
        final Object newInstance = component.getActualInstance();
        component.setInstance(oldInstance);
        return (T) newInstance;
    }

    @Override
    public Collection<ComponentModel> getImplementations(Class<?> cls) {
        return this.componentsAndBean.stream()
                .filter(sd -> cls.isAssignableFrom(sd.getComponentType()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ComponentModel> getDefineBeansWithAnnotation(Class<? extends Annotation> annotation) {
        return this.componentsAndBean.stream()
                .filter(sd -> sd.getAnnotation() != null && sd.getAnnotation().annotationType() == annotation)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ComponentModel> getAllDefineBean() {
        return this.componentsAndBean;
    }


    @Override
    public void update(Object componentInstance) {
        this.update(componentInstance.getClass(), componentInstance);
    }

    @Override
    public void update(Class<?> cls, Object componentInstance) {
        this.update(cls, componentInstance, true);
    }

    @Override
    public void update(Class<?> cls, Object componentInstance, boolean destroyOldInstance) {
        final ComponentModel componentModel = this.getDefineBean(cls, null);
        if (componentModel == null) {
            throw new IllegalArgumentException(String.format(COMPONENT_NOT_FOUND_FORMAT, cls.getName()));
        }
        if (destroyOldInstance) {
            HandlerInstantiation.destroyInstance(componentModel);
        }
        componentModel.setInstance(componentInstance);
    }

    @Override
    public void reload(ComponentModel componentModel) {
        HandlerInstantiation.destroyInstance(componentModel);
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
