package io.smallrye.stork;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.ConfigProvider;

public class TestEnv {
    public static final File SPI_ROOT = new File("target/test-classes/META-INF/services");
    public static final List<ServiceConfig> configurations = new ArrayList<>();
    public static Set<Path> createdSpis = new HashSet<>();

    @SuppressWarnings("unchecked")
    public static <T> void install(Class<T> itf, Class<? extends T>... impls) {
        File out = new File(SPI_ROOT, itf.getName());
        if (out.isFile()) {
            throw new IllegalArgumentException(out.getAbsolutePath() + " does already exist");
        }
        if (impls == null || impls.length == 0) {
            throw new IllegalArgumentException("The list of providers must not be `null` or empty");
        }

        List<String> list = Arrays.stream(impls).map(Class::getName).collect(Collectors.toList());
        try {
            Files.write(out.toPath(), list);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        createdSpis.add(out.toPath());
    }

    public static void clearSPIs() throws IOException {
        for (Path createdSpi : createdSpis) {
            Files.delete(createdSpi);
        }
        createdSpis.clear();
    }

    public static class AnchoredConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            return new ArrayList<>(configurations);
        }

        @Override
        public int priority() {
            return 5;
        }
    }
}
