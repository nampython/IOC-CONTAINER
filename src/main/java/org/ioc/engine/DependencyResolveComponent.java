package org.ioc.engine;

import org.ioc.*;
import org.ioc.configuration.InstantiationConfiguration;
import org.ioc.exception.CircularDependencyException;
import org.ioc.exception.ComponentInstantiationException;
import org.ioc.support.HandlerAnnotation;
import org.ioc.stereotype.Nullable;
import org.ioc.stereotype.Qualifier;

import java.util.*;
import java.util.stream.Collectors;

public class DependencyResolveComponent {
    private final InstantiationConfiguration configuration;

    public DependencyResolveComponent(InstantiationConfiguration configuration) {
        this.configuration = configuration;
    }


    /**
     * Resolve the dependencies of a component model by recursively resolving the dependencies of its dependencies
     *
     * @param componentModels The list of components that are being resolved.
     * @return A list of EnqueuedComponentDetails
     */
    public List<EnqueuedComponentDetails> resolveDependencies(Collection<ComponentModel> componentModels) {
        final List<EnqueuedComponentDetails> resolvedDependencies = new ArrayList<>();
        final List<ComponentModel> allAvailableComponents = new ArrayList<>(componentModels);
        allAvailableComponents.addAll(this.configuration.getProvidedComponentModels());
        for (ComponentModel componentModel : allAvailableComponents) {
            this.resolveDependency(componentModel, resolvedDependencies, allAvailableComponents, new LinkedList<>());
        }
        return resolvedDependencies;
    }

    private void resolveDependency(ComponentModel componentModel, List<EnqueuedComponentDetails> resolvedDependencies,
                                   List<ComponentModel> allAvailableComponents, LinkedList<ComponentModel> componentModelTrace) {

        this.checkForCyclicDependency(componentModel, componentModelTrace);
        final EnqueuedComponentDetails enqueuedComponentDetails = new EnqueuedComponentDetails(componentModel);

        if (!resolvedDependencies.contains(enqueuedComponentDetails)) {
            componentModelTrace.addFirst(componentModel);
//            final Set<MethodAspectHandlerDto> aspects = service.getMethodAspectHandlers()
//                    .values().stream()
//                    .flatMap(Collection::stream)
//                    .collect(Collectors.toSet());
//
//            for (MethodAspectHandlerDto aspect : aspects) {
//                this.resolveDependency(aspect.getServiceDetails(), resolvedDependencies, allAvailableServices, serviceTrace);
//            }
            // Get all params from a specified component. It includes params of constructor and fields
            List<DependencyParam> dependencyParams = new ArrayList<>() {{
                addAll(enqueuedComponentDetails.getConstructorParams());
                addAll(enqueuedComponentDetails.getFieldDependencies());
            }};
            // Process params
            for (DependencyParam dependencyParam : dependencyParams) {
                final List<ComponentModel> componentModelsToResolve;
                try {
                    componentModelsToResolve = this.resolveParameter(dependencyParam, allAvailableComponents);
                } catch (Exception e) {
                    throw new ComponentInstantiationException(String.format(
                            "Error while resolving dependencies for service '%s'.", componentModel.getComponentType()
                    ), e);
                }
                for (ComponentModel componentModelResolve : componentModelsToResolve) {
                    this.resolveDependency(componentModelResolve, resolvedDependencies, allAvailableComponents, componentModelTrace);
                }
            }
            componentModelTrace.removeFirst();
            resolvedDependencies.add(enqueuedComponentDetails);
        }
    }

    private List<ComponentModel> resolveParameter(DependencyParam dependencyParam, List<ComponentModel> allAvailableComponents) {
        Class<?> dependencyType = dependencyParam.getDependencyType();
        String instanceName = dependencyParam.getInstanceName();

        if (HandlerAnnotation.isAnnotationPresent(dependencyParam.getAnnotations(), Nullable.class)) {
            dependencyParam.setRequired(false);
        }
        // In case of the parameter is not equals null. That means @Qualifier exists. Check the value of @Qualifier and
        // compare to all the available component. If not exist, throw an exception.
        if (dependencyParam.getInstanceName() != null) {
            ResolvedComponentDto resolvedComponentDto = HandlerDependencyParam.getNamedInstanceService(dependencyType, instanceName, allAvailableComponents);
            if (resolvedComponentDto != null) {
                dependencyParam.setComponentModel(resolvedComponentDto.getActualComponentModel());
                return List.of(resolvedComponentDto.getProducerComponentModel());
            } else {
                if (dependencyParam.isRequired()) {
                    throw new ComponentInstantiationException(String.format(
                            "Could not create instance of '%s'. Qualifier '%s' was not found.",
                            dependencyType,
                            dependencyParam.getInstanceName()
                    ));
                }
            }
            // In case of parameter's NameInstance is  equals null. That means @Qualifier is not exist.
        } else {
            final List<ComponentModel> resolvedComponentModels;
            if (dependencyParam instanceof DependencyParamCollection) {
                resolvedComponentModels = this.loadCompatibleComponentDetails((DependencyParamCollection) dependencyParam, allAvailableComponents);
            } else {
                resolvedComponentModels = this.loadCompatibleComponentDetails(dependencyParam, allAvailableComponents);
            }
            assert resolvedComponentModels != null;
            if (!resolvedComponentModels.isEmpty()) {
                return resolvedComponentModels;
            }
//        final DependencyResolver dependencyResolver = this.getDependencyResolver(dependencyParam);
//        if (dependencyResolver != null) {
//            //TODO: do not set instance, add support for proxy and singleton dependency resolvers
//            dependencyParam.setInstance(dependencyResolver.resolve(dependencyParam));
//            dependencyParam.setDependencyResolver(dependencyResolver);
//            return List.of();
//        }
            if (dependencyParam.isRequired()) {
                throw new ComponentInstantiationException(
                        String.format("Could not create instance of '%s'. Parameter '%s' implementation was not found", dependencyType, dependencyType.getName())
                );
            }
        }
        return List.of();
    }

    private List<ComponentModel> loadCompatibleComponentDetails(DependencyParamCollection dependencyParam, List<ComponentModel> allAvailableComponents) {
        return null;
    }

    private List<ComponentModel> loadCompatibleComponentDetails(DependencyParam dependencyParam, List<ComponentModel> allAvailableComponents) {
        final List<ResolvedComponentDto> compatibleComponents = HandlerDependencyParam.findAllCompatibleComponents(dependencyParam, allAvailableComponents);
        if (compatibleComponents.size() > 1) {
            throw new ComponentInstantiationException(String.format(
                    "Could not create instance of '%s'. "
                            + "There are more than one compatible services: (%s)."
                            + "Please consider using '%s' annotation.",
                    dependencyParam.getDependencyType(),
                    compatibleComponents.stream().map(ResolvedComponentDto::getActualComponentModel).collect(Collectors.toList()),
                    Qualifier.class.getName()
            ));
        }
        if (compatibleComponents.isEmpty()) {
            return List.of();
        }

        dependencyParam.setComponentModel(compatibleComponents.get(0).getActualComponentModel());
        return compatibleComponents.stream()
                .map(ResolvedComponentDto::getProducerComponentModel)
                .collect(Collectors.toList());
    }

    private void checkForCyclicDependency(ComponentModel componentModel, LinkedList<ComponentModel> componentTrace) {
        if (!componentTrace.isEmpty()) {
            if (componentTrace.contains(componentModel)) {
                char arrowDown = '\u2193';
                char arrowUp = '\u2191';
                final StringBuilder sb = new StringBuilder();
                sb.append("Circular dependency found!");
                sb.append(String.format("\n%s<----%s", arrowDown, arrowUp));
                sb.append(String.format("\n%s     %s %s", arrowDown, arrowUp, componentModel.getComponentType()));
                for (ComponentModel trace : componentTrace) {
                    if (componentModel.equals(trace)) {
                        break;
                    }
                    sb.append(String.format("\n%s     %s %s", arrowDown, arrowUp, trace.getComponentType()));
                }
                sb.append(String.format("\n%s---->%s", arrowDown, arrowUp));
                throw new CircularDependencyException(sb.toString());
            }
        }
    }

    static class HandlerDependencyParam {
        /**
         * > It returns a component model and a bean model if the component model or the bean model has the same name as
         * the nameOfInstance parameter and the component model or the bean model is assignable from the cls parameter
         *
         * @param cls                    - The class of the component you want to get.
         * @param nameOfInstance         - The name of the instance you want to get.
         * @param allAvailableComponents - All the components that are available in the application.
         * @return A ResolvedComponentDto object.
         */
        public static ResolvedComponentDto getNamedInstanceService(Class<?> cls, String nameOfInstance, Collection<ComponentModel> allAvailableComponents) {
            for (ComponentModel componentModel : allAvailableComponents) {
                boolean isExists = nameOfInstance.equalsIgnoreCase(componentModel.getInstanceName()) && cls.isAssignableFrom(componentModel.getComponentType());
                if (isExists) {
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

        /**
         * > It takes a list of all available components and returns a list of all components that are compatible with the
         * given dependency
         *
         * @param dependencyParam        - This is the parameter that contains the information about the dependency that we are
         *                               trying to resolve.
         * @param allAvailableComponents - All the components that are available in the current project.
         * @return A list of ResolvedComponentDto objects.
         */
        public static List<ResolvedComponentDto> findAllCompatibleComponents(DependencyParam dependencyParam, Collection<ComponentModel> allAvailableComponents) {
            final List<ResolvedComponentDto> resolvedComponents = new ArrayList<>();
            for (ComponentModel component : allAvailableComponents) {
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
}