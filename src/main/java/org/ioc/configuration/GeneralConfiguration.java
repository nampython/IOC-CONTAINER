package org.ioc.configuration;

public class GeneralConfiguration extends CoreConfiguration{

    private boolean runInNewThread;

    public GeneralConfiguration(Configuration parentConfig) {
        super(parentConfig);
    }

    public GeneralConfiguration runInNewThread(boolean runInNewThread) {
        this.runInNewThread = runInNewThread;
        return this;
    }

    public boolean isRunInNewThread() {
        return this.runInNewThread;
    }
}
