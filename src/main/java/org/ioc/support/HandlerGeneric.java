package org.ioc.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Optional;

public class HandlerGeneric {
    public static Type[] getGenericTypeArguments(Class<?> cls, Class<?> genericClass) {
        final Optional<ParameterizedType> genericClsType = Arrays.stream(cls.getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType)
                .filter(type -> ((ParameterizedType) type).getRawType() == genericClass)
                .map(type -> (ParameterizedType) type)
                .findFirst();

        if (genericClsType.isPresent()) {
            return genericClsType.get().getActualTypeArguments();
        }

        if (cls.getGenericSuperclass() != Object.class) {
            return getGenericTypeArguments((Class<?>) cls.getGenericSuperclass(), genericClass);
        }

        return null;
    }

    public static Class<?> getRawType(ParameterizedType type) {
        return getRawType(type, null);
    }

    private static Class<?> getRawType(ParameterizedType type, Class<?> lastType) {
        final Type actualTypeArgument = type.getActualTypeArguments()[0];

        if (actualTypeArgument instanceof Class) {
            return (Class<?>) actualTypeArgument;
        }
        if (actualTypeArgument instanceof WildcardType) {
            return lastType;
        }
        return getRawType(
                (ParameterizedType) actualTypeArgument,
                (Class<?>) ((ParameterizedType) actualTypeArgument).getRawType()
        );
    }
}
