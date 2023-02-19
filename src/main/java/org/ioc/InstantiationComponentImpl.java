package org.ioc;

import org.ioc.exception.ComponentInstantiationException;
import org.ioc.support.HandlerInstantiation;

import java.util.*;

public class InstantiationComponentImpl implements InstantiationComponent {

    private final DependencyResolveComponent dependencyResolveComponent;

    public InstantiationComponentImpl(DependencyResolveComponent dependencyResolveComponent) {
        this.dependencyResolveComponent = dependencyResolveComponent;
    }

    @Override
    public Collection<ComponentModel> instantiateComponentAndBean(Set<ComponentModel> componentModels) throws ComponentInstantiationException {
        final List<EnqueuedComponentDetails> enqueuedComponentDetails = this.dependencyResolveComponent.resolveDependencies(componentModels);
        for (EnqueuedComponentDetails enqueuedComponentDetail : enqueuedComponentDetails) {
            this.instantiateService(enqueuedComponentDetail);
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

    private void instantiateService(EnqueuedComponentDetails enqueuedComponentDetail) {
        final ComponentModel componentModel=  enqueuedComponentDetail.getComponentModel();
        final Object[] constructorInstances = enqueuedComponentDetail.getConstructorInstances();
        final Object[] fieldInstances = enqueuedComponentDetail.getFieldInstances();

        if (enqueuedComponentDetail.getComponentModel().getInstance() == null) {
            HandlerInstantiation.createInstance(componentModel, constructorInstances, fieldInstances);
        }


        if (componentModel.getScopeType() == ScopeType.PROTOTYPE) {
            //???
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
}
