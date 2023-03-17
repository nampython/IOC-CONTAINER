package org.ioc.engine;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.ioc.*;
import org.ioc.exception.ComponentInstantiationException;
import org.ioc.support.HandlerInstantiation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class InstantiationComponentBean extends InstantiateContext {
    private final DependencyResolveComponent dependencyResolveComponent;

    public InstantiationComponentBean(DependencyResolveComponent dependencyResolveComponent) {
        this.dependencyResolveComponent = dependencyResolveComponent;
    }

    @Override
    public Collection<ComponentModel> instantiateComponentAndBean(Set<ComponentModel> componentModels) throws ComponentInstantiationException {
        final List<EnqueuedComponentDetails> enqueuedComponentDetails = this.dependencyResolveComponent.resolveDependencies(componentModels);
        for (EnqueuedComponentDetails enqueuedComponentDetail : enqueuedComponentDetails) {
            this.instantiateComponent(enqueuedComponentDetail);
        }
        final List<ComponentModel> allComponentsAndBean = new ArrayList<>();
        componentModels.forEach(
                componentModel -> {
                    allComponentsAndBean.add(componentModel);
                    allComponentsAndBean.addAll(componentModel.getBeans());
                }
        );
        return allComponentsAndBean;
    }

    private void instantiateComponent(EnqueuedComponentDetails enqueuedComponentDetail) {
        final ComponentModel componentModel=  enqueuedComponentDetail.getComponentModel();
        final Object[] constructorInstances = enqueuedComponentDetail.getConstructorInstances();
        final Object[] fieldInstances = enqueuedComponentDetail.getFieldInstances();
        if (enqueuedComponentDetail.getComponentModel().getInstance() == null) {
            HandlerInstantiation.createInstance(componentModel, constructorInstances, fieldInstances);
        }
        if (componentModel.getScopeType() == ScopeType.PROXY) {
            ProxyUtils.createProxyInstance(componentModel, enqueuedComponentDetail.getConstructorInstances());
        }
        this.registerResolvedDependencies(enqueuedComponentDetail);
        this.registerBeans(componentModel);
    }

    private void registerBeans(ComponentModel componentModel) {
        for (ComponentBeanModel bean : componentModel.getBeans()) {
            HandlerInstantiation.createBeanInstance(bean);
            if (bean.getScopeType() == ScopeType.PROXY) {
//                ProxyUtils.createBeanProxyInstance(beanDetails);
            }
        }
    }

    private void registerResolvedDependencies(EnqueuedComponentDetails enqueuedComponentDetail) {
        final ComponentModel componentModel = enqueuedComponentDetail.getComponentModel();
        componentModel.setResolvedConstructorParams(enqueuedComponentDetail.getConstructorParams());
        componentModel.setResolvedFields(enqueuedComponentDetail.getFieldDependencies());
    }
    static class ProxyUtils {
        public static void createProxyInstance(ComponentModel componentModel, Object[] constructorParams) {
            final ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setSuperclass(componentModel.getComponentType());
            Object proxyInstance;
            try {
                proxyInstance = proxyFactory.create(componentModel.getTargetConstructor().getParameterTypes(), constructorParams);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            ((ProxyObject) proxyInstance).setHandler(new MethodInvocationHandler(componentModel));
            componentModel.setProxyInstance(proxyInstance);
        }

        public static void createBeanProxyInstance(ComponentModel componentModel) {
            if (!componentModel.getComponentType().isInterface()) {
                return;
            }
            final Object proxyInstance = Proxy.newProxyInstance(
                    componentModel.getComponentType().getClassLoader(),
                    new Class[]{componentModel.getComponentType()},
                    new InvocationHandlerImpl(componentModel));
            componentModel.setProxyInstance(proxyInstance);
        }
    }
}
