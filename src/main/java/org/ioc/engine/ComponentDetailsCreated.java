package org.ioc.engine;

import org.ioc.engine.ComponentModel;

@FunctionalInterface
public interface ComponentDetailsCreated {
    void componentDetailsCreated(ComponentModel componentModel);
}
