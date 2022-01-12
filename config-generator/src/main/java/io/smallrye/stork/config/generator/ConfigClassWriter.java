package io.smallrye.stork.config.generator;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.Constants;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerConfig;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.spi.internal.LoadBalancerLoader;
import io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;

public class ConfigClassWriter {
    private static final String SERVICES_DIR = "META-INF/services/";

    private final ProcessingEnvironment environment;

    public ConfigClassWriter(ProcessingEnvironment environment) {
        this.environment = environment;
    }

    public String createConfig(Element element, LoadBalancerAttribute[] attributes) throws IOException {
        return createConfig(element, format(" * Configuration for the {@code %s} LoadBalancer.", element.getSimpleName()),
                out -> writeLoadBalancerAttributes(attributes, out));
    }

    public String createConfig(Element element, ServiceDiscoveryAttribute[] attributes) throws IOException {
        return createConfig(element, format(" * Configuration for the {@code %s} ServiceDiscovery.", element.getSimpleName()),
                out -> writeServiceDiscoveryAttributes(attributes, out));
    }

    public String createLoadBalancerLoader(Element element, String configClassName, String type) throws IOException {
        String providerClassName = element.toString();
        String className = providerClassName + "Loader";
        String classPackage = getPackage(className);
        String simpleClassName = element.getSimpleName() + "Loader";

        JavaFileObject javaFile = environment.getFiler().createSourceFile(className);
        javaFile.delete();

        try (PrintWriter out = new PrintWriter(javaFile.openWriter())) {
            writePackageDeclaration(classPackage, out);
            out.println(format("import %s;", configClassName));
            out.println(format("import %s;", providerClassName));
            out.println(format("import %s;", LoadBalancer.class.getName()));
            out.println(format("import %s;", LoadBalancerConfig.class.getName()));
            out.println(format("import %s;", ServiceDiscovery.class.getName()));
            writeClassDeclaration(format("%s implements %s", simpleClassName, LoadBalancerLoader.class.getName()),
                    "LoadBalancerLoader for " + providerClassName, out);

            out.println(format("   private final %s provider = new %s();", providerClassName, providerClassName));
            out.println("   @Override");
            out.println(
                    "   public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {");
            out.println(format("      %s typedConfig = new %s(config.parameters());", configClassName, configClassName));
            out.println("      return provider.createLoadBalancer(typedConfig, serviceDiscovery);");
            out.println("   }");
            out.println("   @Override");
            out.println("   public String type() {");
            out.println(String.format("      return \"%s\";", type));
            out.println("   }");

            out.println("}");
        }
        return className;
    }

    public String createServiceDiscoveryLoader(Element element, String configClassName, String type) throws IOException {
        String providerClassName = element.toString();
        String className = providerClassName + "Loader";
        String classPackage = getPackage(className);
        String simpleClassName = element.getSimpleName() + "Loader";

        JavaFileObject javaFile = environment.getFiler().createSourceFile(className);
        javaFile.delete();

        try (PrintWriter out = new PrintWriter(javaFile.openWriter())) {
            writePackageDeclaration(classPackage, out);
            out.println(format("import %s;", configClassName));
            out.println(format("import %s;", providerClassName));
            out.println(format("import %s;", ServiceDiscovery.class.getName()));
            out.println(format("import %s;", ServiceDiscoveryConfig.class.getName()));
            out.println(format("import %s;", ServiceConfig.class.getName()));
            out.println(format("import %s;", StorkInfrastructure.class.getName()));

            writeClassDeclaration(format("%s implements %s", simpleClassName, ServiceDiscoveryLoader.class.getName()),
                    "ServiceDiscoveryLoader for " + providerClassName, out);

            out.println(format("   private final %s provider = new %s();", providerClassName, providerClassName));
            out.println("   @Override");
            out.println("   public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,");
            out.println("              ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {");
            out.println(format("      %s typedConfig = new %s(config.parameters());", configClassName, configClassName));
            out.println(
                    "      return provider.createServiceDiscovery(typedConfig, serviceName, serviceConfig, storkInfrastructure);");
            out.println("   }");
            out.println("   @Override");
            out.println("   public String type() {");
            out.println(String.format("      return \"%s\";", type));
            out.println("   }");

            out.println("}");
        }
        return className;
    }

    private void writeServiceDiscoveryAttributes(ServiceDiscoveryAttribute[] attributes, PrintWriter out) {
        for (ServiceDiscoveryAttribute attribute : attributes) {
            writeAttribute(out, attribute.name(), attribute.description(), attribute.defaultValue());
        }
    }

    private void writeLoadBalancerAttributes(LoadBalancerAttribute[] attributes, PrintWriter out) {
        for (LoadBalancerAttribute attribute : attributes) {
            writeAttribute(out, attribute.name(), attribute.description(), attribute.defaultValue());
        }
    }

    public String createConfig(Element element, String comment, Consumer<PrintWriter> attributesWriter) throws IOException {
        String className = element.toString() + "Configuration";
        String classPackage = getPackage(className);
        String simpleClassName = element.getSimpleName() + "Configuration";

        JavaFileObject file = environment.getFiler().createSourceFile(className);
        file.delete();
        try (PrintWriter out = new PrintWriter(file.openWriter())) {
            writePackageDeclaration(classPackage, out);

            out.println("import " + Map.class.getName() + ";");
            writeClassDeclaration(simpleClassName, comment, out);

            writeConfigMapRelatedStuff(simpleClassName, out);
            attributesWriter.accept(out);

            out.println('}');
        }
        return className;
    }

    private void writeAttribute(PrintWriter out, String name, String description, String defaultValue) {
        if (defaultValue.equals(Constants.DEFAULT_VALUE)) {
            defaultValue = null;
        }
        out.println("/**");
        if (defaultValue != null) {
            out.println(format(" * %s By default: %s", description, defaultValue));
        } else {
            out.println(format(" * %s", description));
        }
        out.println(" */");
        out.println(format("   public String get%s() {", toCamelCase(name)));
        if (defaultValue != null) {
            out.println(format("      String result = parameters.get(\"%s\");", name));
            out.println(format("      return result == null ? \"%s\" : result;", defaultValue));
        } else {
            out.println(format("      return parameters.get(\"%s\");", name));
        }
        out.println("   }");
    }

    private void writeConfigMapRelatedStuff(String simpleClassName, PrintWriter out) {
        out.println("   private final Map<String, String> parameters;");
        out.println(format("   public %s(Map<String, String> params) {", simpleClassName));
        out.println("      parameters = params;");
        out.println("   }");
    }

    private String toCamelCase(String attribute) {
        StringBuilder result = new StringBuilder();
        boolean capitize = true;
        for (char c : attribute.toCharArray()) {
            if (!Character.isJavaIdentifierPart(c)) {
                capitize = true;
            } else {
                result.append(capitize ? Character.toUpperCase(c) : c);
                capitize = false;
            }
        }

        return result.toString();
    }

    static void writePackageDeclaration(String packageName, PrintWriter out) {
        if (packageName != null) {
            out.print("package ");
            out.print(packageName);
            out.println(";");
            out.println();
        }
    }

    private void writeClassDeclaration(String simpleName, String comment, PrintWriter out) {
        out.println();
        out.println("/**");
        out.println(comment);
        out.println(" */");
        out.println(format(" public class %s {", simpleName));
    }

    private static String getPackage(String className) {
        int indexOfLastDot = className.lastIndexOf('.');
        if (indexOfLastDot > 0) {
            return className.substring(0, indexOfLastDot);
        } else {
            throw new IllegalArgumentException(
                    "Invalid class name, load balancer and service provider classes cannot be in the default package");
        }
    }

    public void createServiceLoaderFile(String interfaceName, Set<String> loaders) throws IOException {
        FileObject loaderFile = environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                SERVICES_DIR + interfaceName);
        loaderFile.delete();

        try (PrintWriter out = new PrintWriter(loaderFile.openWriter())) {
            for (String loader : loaders) {
                out.println(loader);
            }
        }
    }
}
