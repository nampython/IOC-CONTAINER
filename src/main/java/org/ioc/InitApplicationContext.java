package org.ioc;

import org.ioc.configuration.Configuration;
import org.ioc.contex.ApplicationContext;
import org.ioc.contex.ApplicationContextCached;
import org.ioc.engine.*;
import org.ioc.engine.ClassLoaderContext;
import org.ioc.engine.core.*;
import org.ioc.stereotype.StartUp;
import org.ioc.type.DirectoryType;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Entry point for every application. Return an applicationContext to retrieve the{@link org.ioc.stereotype.Component}
 * Contain multiple starting point method
 */
public class InitApplicationContext {
    /**
     * > This function creates a new instance of the class passed in as the first parameter, and then calls the `run`
     * function with the new instance and the second parameter
     *
     * @param initApplicationContextClass The class that will be used to initialize the application context.
     * @return An ApplicationContext object.
     */
    public static ApplicationContext run(Class<?> initApplicationContextClass) {
        return run(initApplicationContextClass, new Configuration());
    }

    /**
     * In case that want to custom component annotation. Use class {@link Configuration} to custom annotation
     *
     * @param initApplicationContextClass {@link Class}
     * @param configuration               {@link Configuration}
     * @return ApplicationContext
     */
    public static ApplicationContext run(Class<?> initApplicationContextClass, Configuration configuration) {
        final Directory directory = new DirectoryHandler().resolveDirectory(initApplicationContextClass);
        final File file = new File(directory.getDirectory());
        final ApplicationContext applicationContext = run(new File[]{file}, configuration);
        runStartUpMethod(initApplicationContextClass, applicationContext);
        return applicationContext;
    }

    public static ApplicationContext run(File[] files, Configuration configuration) {
        SettingComponent scanningComponent = new LoaderComponent(configuration.scanning());
        InstantiateContext instantiationComponent = new InstantiationComponentBean(
                new DependencyResolveComponent(configuration.instantiations())
        );
        final Set<Class<?>> locatedClasses = new HashSet<>();
        final List<ComponentModel> instantiatedComponents = new ArrayList<>();

        final Runnable runnable = () -> {
            locatedClasses.addAll(getActiveClass(files));
            final Set<ComponentModel> allActiveComponents = new HashSet<>(scanningComponent.mappingComponent(locatedClasses));
            instantiatedComponents.addAll(new ArrayList<>(
                    instantiationComponent.instantiateComponentAndBean(allActiveComponents)
            ));
        };
        if (configuration.general().isRunInNewThread()) {
            final Thread runner = new Thread(runnable);
            runner.setContextClassLoader(configuration.scanning().getClassLoader());
            runner.start();
            try {
                runner.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(configuration.scanning().getClassLoader());
                runnable.run();
            } finally {
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        }
        return new ApplicationContextCached(locatedClasses, instantiatedComponents);
    }

    private static Set<Class<?>> getActiveClass(File[] files) {
        final Set<Class<?>> allActiveClass = new HashSet<>();
        DirectoryHandler directoryHandler = new DirectoryHandler();
        ClassLoaderContext classLoaderContext = new ClassLoaderContextDir();
        for (File file : files) {
            final Directory directory = directoryHandler.resolveDirectory(file);
            //TODO: Implement ClassLocatorForJarFile class.
            if (directory.getDirectoryType() == DirectoryType.JAR_FILE) {
//                classLocator = new ClassLocatorForJarFile();
            }
            allActiveClass.addAll(classLoaderContext.loadClasses(directory.getDirectory()));
        }
        return allActiveClass;
    }

    /**
     * This method calls executes when all components are loaded.
     * <p>
     * Looks for instantiated component from the given type.
     * <p>public
     * If instance is found, looks for void method with 0 params
     * and with @StartUp annotation and executes it.
     *
     * @param startupClass any class from the client side.
     */
    private static void runStartUpMethod(Class<?> startupClass, ApplicationContext applicationContext) {
        final ComponentModel componentModel = applicationContext.getDefineBean(startupClass, null);
        if (componentModel != null) {
            for (Method declaredMethod : componentModel.getComponentType().getDeclaredMethods()) {
                boolean checkStartupMethod = (declaredMethod.getReturnType() != void.class &&
                        declaredMethod.getReturnType() != Void.class) ||
                        !declaredMethod.isAnnotationPresent(StartUp.class);

                if (checkStartupMethod) {
                } else {
                    declaredMethod.setAccessible(true);
                    final Object[] params = Arrays.stream(declaredMethod.getParameterTypes())
                            .map(applicationContext::getBean)
                            .toArray(Object[]::new);
                    try {
                        declaredMethod.invoke(componentModel.getActualInstance(), params);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
