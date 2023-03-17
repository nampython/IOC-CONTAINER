package org.ioc;

import javassist.util.proxy.MethodHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MethodInvocationHandler implements MethodHandler {

    private final ComponentModel componentModel;

    public MethodInvocationHandler(ComponentModel componentModel) {
        this.componentModel = componentModel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        try {
            if (!this.componentModel.getMethodAspectHandlers().containsKey(thisMethod)) {
                return thisMethod.invoke(this.componentModel.getActualInstance(), args);
            }

            final List<MethodAspectHandlerDto> aspectHandlers = this.componentModel.getMethodAspectHandlers().get(thisMethod);

            final AtomicReference<MethodInvocationChain> invocationChain = new AtomicReference<>(() -> thisMethod.invoke(
                    this.componentModel.getActualInstance(), args
            ));

            for (MethodAspectHandlerDto serviceAspectHandler : aspectHandlers) {
                final MethodAspectHandler<Annotation> aspectHandler = (MethodAspectHandler<Annotation>)
                        serviceAspectHandler.getComponentModel().getInstance();

                final MethodInvocationChain next = invocationChain.get();
                invocationChain.set(() -> aspectHandler.proceed(
                        thisMethod.getAnnotation(serviceAspectHandler.getAnnotation()),
                        thisMethod,
                        args,
                        next
                ));
            }

            return invocationChain.get().proceed();
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
