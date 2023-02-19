package org.ioc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface ComponentMethodAspectHandler <T extends Annotation> {
    Object proceed(T annotation, Method method, Object[] params, MethodInvocationChain invocationChain) throws Exception;
}
