package org.ioc.contex;

import org.ioc.ComponentModel;
import org.ioc.contex.factory.AbstractApplicationContext;
import java.util.Collection;

public class AnnotationConfigApplicationContext extends AbstractApplicationContext {
    public AnnotationConfigApplicationContext(Collection<Class<?>> allLocatedClasses, Collection<ComponentModel> componentsAndBean) {
        super(allLocatedClasses, componentsAndBean);
    }
}

