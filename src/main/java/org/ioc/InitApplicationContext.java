package org.ioc;

import org.ioc.configuration.Configuration;
import org.ioc.contex.AnnotationConfigApplicationContext;
import org.ioc.contex.ApplicationContext;
import org.ioc.stereotype.StartUp;
import org.ioc.type.DirectoryType;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class InitApplicationContext {
    public static void main(String[] args) {
        run(InitApplicationContext.class);
    }

    private static ApplicationContext run(Class<?> initApplicationContextClass) {
        return run(initApplicationContextClass, new Configuration());
    }

    private static ApplicationContext run(Class<?> initApplicationContextClass, Configuration configuration) {
        final Directory directory = new DirectoryHandlerImpl().resolveDirectory(initApplicationContextClass);
        final File file = new File(directory.getDirectory());
        final ApplicationContext applicationContext = run(
                new File[] {file}, configuration
        );
        runStartUpMethod(initApplicationContextClass, applicationContext);
        return applicationContext;
    }

    private static ApplicationContext run(File[] files, Configuration configuration) {
        ScanningComponent scanningComponent = new ScanningComponentImpl(configuration.scanning());
        InstantiationComponent instantiationComponent = new InstantiationComponentImpl(
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
        DirectoryHandler directoryHandler = new DirectoryHandlerImpl();
        AccessingAllClasses accessingAllClasses = new AccessingAllClassesFromDirImpl();

        for (File file : files) {
            final Directory directory = directoryHandler.resolveDirectory(file);
            //TODO: Implement ClassLocatorForJarFile class.
            if (directory.getDirectoryType() == DirectoryType.JAR_FILE) {
//                classLocator = new ClassLocatorForJarFile();
            }
            allActiveClass.addAll(accessingAllClasses.accessAllClasses(directory.getDirectory()));
        }
        return allActiveClass;
    }

    /**
     * This method calls executes when all services are loaded.
     * <p>
     * Looks for instantiated service from the given type.
     * <p>
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
