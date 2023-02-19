package org.ioc.configuration;

public class Configuration {
    private final ScanningConfiguration annotations;

    private final InstantiationConfiguration instantiations;

    private final GeneralConfiguration generalConfiguration;

    public Configuration() {
        this.annotations = new ScanningConfiguration(this);
        this.instantiations = new InstantiationConfiguration(this);
        this.generalConfiguration = new GeneralConfiguration(this);
    }
    public ScanningConfiguration scanning() {
        return this.annotations;
    }

    public InstantiationConfiguration instantiations() {
        return this.instantiations;
    }

    public GeneralConfiguration general() {
        return this.generalConfiguration;
    }

    public Configuration build() {
        return this;
    }
}
