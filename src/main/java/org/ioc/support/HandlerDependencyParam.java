package org.ioc.support;

import org.ioc.ComponentBeanModel;
import org.ioc.ComponentModel;
import org.ioc.DependencyParam;
import org.ioc.ResolvedComponentDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HandlerDependencyParam {
    public static ResolvedComponentDto getNamedInstanceService(Class<?> cls, String nameOfInstance, Collection<ComponentModel> componentModels) {
        for (ComponentModel componentModel : componentModels) {
            boolean b = nameOfInstance.equalsIgnoreCase(componentModel.getInstanceName()) && cls.isAssignableFrom(componentModel.getComponentType());
            if (b) {
                return new ResolvedComponentDto(componentModel, componentModel);
            }
            for (ComponentBeanModel bean : componentModel.getBeans()) {
                boolean c = nameOfInstance.equalsIgnoreCase(bean.getInstanceName()) && cls.isAssignableFrom(bean.getComponentType());
                if (c) {
                    return new ResolvedComponentDto(componentModel, bean);
                }
            }
        }
        return null;
    }


    public static List<ResolvedComponentDto> findAllCompatibleComponents(DependencyParam dependencyParam, Collection<ComponentModel> allComponents) {
        final List<ResolvedComponentDto> resolvedComponents = new ArrayList<>();

        for (ComponentModel component : allComponents) {
            if (dependencyParam.isCompatible(component)) {
                resolvedComponents.add(new ResolvedComponentDto(component, component));
            }

            for (ComponentBeanModel bean : component.getBeans()) {
                if (dependencyParam.isCompatible(bean)) {
                    resolvedComponents.add(new ResolvedComponentDto(component, bean));
                }
            }
        }
        return resolvedComponents;
    }

    public static boolean isComponentCompatible(ComponentModel componentModel, Class<?> requiredType, String instanceName) {
        //service type is assignable from (concrete instance or proxy)
        // and (instanceName if not null equals service's instance name)
        final boolean isRequiredTypeAssignable = requiredType.isAssignableFrom(componentModel.getComponentType());
        final boolean isRequiredTypeAssignable2 = componentModel.getInstance() != null &&
                requiredType.isAssignableFrom(componentModel.getInstance().getClass());
        final boolean instanceNameMatches = instanceName == null ||
                instanceName.equalsIgnoreCase(componentModel.getInstanceName());
        return (isRequiredTypeAssignable || isRequiredTypeAssignable2) && instanceNameMatches;
    }
}
