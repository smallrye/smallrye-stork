package io.smallrye.stork;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class WeldTestBase {

    protected Weld weld;
    protected WeldContainer container;

    public WeldTestBase() {
        weld = new Weld();
        weld.addBeanClass(MyDataBean.class);
        TestEnv.configurations.clear();
    }

    public void run() {
        container = weld.initialize();
    }

    public void close() {
        if (container != null) {
            container.close();
        } else {
            weld.shutdown();
        }
        TestEnv.configurations.clear();
    }

    public <T> T get(Class<T> clazz) {
        return container.select(clazz).get();
    }

}
