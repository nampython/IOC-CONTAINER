package org.ioc;

import org.ioc.configuration.Configuration;
import org.ioc.contex.AnnotationConfigApplicationContext;
import org.ioc.contex.ApplicationContext;
import org.ioc.engine.*;
import org.ioc.engine.ClassLoaderContext;
import org.ioc.stereotype.StartUp;
import org.ioc.type.DirectoryType;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class InitApplicationContext {
    public static ApplicationContext run(Class<?> initApplicationContextClass) {
        return run(initApplicationContextClass, new Configuration());
    }

    /**
     *  In case that want to custom component annotation. Use class {@link Configuration} to custom annotation
     * @param initApplicationContextClass {@link Class}
     * @param configuration {@link Configuration}
     * @return ApplicationContext
     */
    public static ApplicationContext run(Class<?> initApplicationContextClass, Configuration configuration) {
        final Directory directory = new DirectoryHandler().resolveDirectory(initApplicationContextClass);
        final File file = new File(directory.getDirectory());
        final ApplicationContext applicationContext = run(new File[] {file}, configuration);
        runStartUpMethod(initApplicationContextClass, applicationContext);
        return applicationContext;
    }

    public static ApplicationContext run(File[] files, Configuration configuration) {
        LoaderComponent scanningComponent = new LoaderComponent(configuration.scanning());
        InstantiationComponent instantiationComponent = new  InstantiationComponentImpl(
                new DependencyResolveComponentImpl(configuration.instantiations())
        );

        final Set<Class<?>> locatedClasses = new HashSet<>();
        final List<ComponentModel> instantiatedComponents = new ArrayList<>();

        final Runnable runnable = () -> {
            locatedClasses.addAll(getActiveClass(files));
            final Set<ComponentModel> allActiveComponents = new HashSet<>(scanningComponent.mappingComponents(locatedClasses));
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
        return new AnnotationConfigApplicationContext(locatedClasses, instantiatedComponents);
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
     * This method calls executes when all services are loaded.
     * <p>
     * Looks for instantiated service from the given type.
     * <p>public
     * If instance is found, looks for void method with 0 params
     * and with with @StartUp annotation and executes it.
     *
     * @param startupClass any class from the client side.
     */
    private static void runStartUpMethod(Class<?> startupClass, ApplicationContext applicationContext) {
        final ComponentModel componentModel = applicationContext.getDefineBean(startupClass, null);

        if (componentModel == null) {
            return;
        }

        for (Method declaredMethod : componentModel.getComponentType().getDeclaredMethods()) {
            if ((declaredMethod.getReturnType() != void.class &&
                    declaredMethod.getReturnType() != Void.class)
                    || !declaredMethod.isAnnotationPresent(StartUp.class)) {
                continue;
            }

            declaredMethod.setAccessible(true);
            final Object[] params = Arrays.stream(declaredMethod.getParameterTypes())
                    .map(applicationContext::getDefineBean)
                    .toArray(Object[]::new);

            try {
                declaredMethod.invoke(componentModel.getActualInstance(), params);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return;
        }
    }
}
