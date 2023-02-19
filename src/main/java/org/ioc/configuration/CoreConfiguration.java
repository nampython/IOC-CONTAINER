package org.ioc.configuration;

public abstract class CoreConfiguration {
    private final Configuration configuration;

    protected CoreConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration and() {
        return this.configuration;
    }
}
