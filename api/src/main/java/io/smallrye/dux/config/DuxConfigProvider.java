package io.smallrye.dux.config;

import java.util.List;

public interface DuxConfigProvider {
    List<ServiceConfig> getDuxConfigs();

    int priority();
}
