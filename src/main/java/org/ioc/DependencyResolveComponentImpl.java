package org.ioc;

import org.ioc.configuration.InstantiationConfiguration;
import org.ioc.exception.CircularDependencyException;
import org.ioc.exception.ComponentInstantiationException;
import org.ioc.support.HandlerAnnotation;
import org.ioc.support.HandlerDependencyParam;
import org.ioc.stereotype.Nullable;
import org.ioc.stereotype.Qualifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DependencyResolveComponentImpl implements DependencyResolveComponent {
    private final InstantiationConfiguration configuration;

    public DependencyResolveComponentImpl(InstantiationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
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

        if (resolvedDependencies.contains(enqueuedComponentDetails)) {
            return;
        }
        componentModelTrace.addFirst(componentModel);

        List<DependencyParam> dependencyParams = new ArrayList<>() {{
            addAll(enqueuedComponentDetails.getConstructorParams());
            addAll(enqueuedComponentDetails.getFieldDependencies());
        }};


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

    private  List<ComponentModel> resolveParameter(DependencyParam dependencyParam, List<ComponentModel> allAvailableComponents) {
        Class<?> dependencyType = dependencyParam.getDependencyType();
        String instanceName = dependencyParam.getInstanceName();

        if (HandlerAnnotation.isAnnotationPresent(dependencyParam.getAnnotations(), Nullable.class)) {
            dependencyParam.setRequired(false);
        }

        // In case of the parameter is not equals null.
        if (dependencyParam.getInstanceName() != null) {
            ResolvedComponentDto resolvedComponentDto = HandlerDependencyParam.getNamedInstanceService(
                    dependencyType,
                    instanceName,
                    allAvailableComponents
            );
            if (resolvedComponentDto != null) {
                dependencyParam.setComponentModel(resolvedComponentDto.getActualComponentModel());
                return List.of(resolvedComponentDto.getProducerComponentModel());
            }
            if (dependencyParam.isRequired()) {
                throw new ComponentInstantiationException(String.format(
                        "Could not create instance of '%s'. Qualifier '%s' was not found.",
                        dependencyType,
                        dependencyParam.getInstanceName()
                ));
            }
        }

        final List<ComponentModel> resolvedComponentModels;
        if (dependencyParam instanceof DependencyParamCollection) {
            resolvedComponentModels = this.loadCompatibleComponentDetails(
                    (DependencyParamCollection) dependencyParam,
                    allAvailableComponents
            );
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
                    String.format("Could not create instance of '%s'. Parameter '%s' implementation was not found",
                            dependencyType,
                            dependencyType.getName()
                    )
            );
        }
        return List.of();
    }

    private List<ComponentModel> loadCompatibleComponentDetails(DependencyParamCollection dependencyParam, List<ComponentModel> allAvailableComponents) {
        return null;
    }

    private List<ComponentModel> loadCompatibleComponentDetails(DependencyParam dependencyParam, List<ComponentModel> allAvailableComponents) {
        final List<ResolvedComponentDto>  compatibleComponents = HandlerDependencyParam.findAllCompatibleComponents(dependencyParam, allAvailableComponents);
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
        if (componentTrace.isEmpty()) {
            return;
        }

        if (!componentTrace.contains(componentModel)) {
            return;
        }

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
