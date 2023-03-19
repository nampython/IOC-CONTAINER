package org.ioc.engine;

import org.ioc.engine.ComponentModel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InvocationHandlerImpl implements InvocationHandler {

    private final ComponentModel componentModel;

    public InvocationHandlerImpl(ComponentModel componentModel) {
        this.componentModel = componentModel;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(this.componentModel.getActualInstance(), args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
