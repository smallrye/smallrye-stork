package io.smallrye.stork.config.generator;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import io.smallrye.stork.api.config.Constants;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;

public class DocWriter {
    private static final String STORK_DOCS_DIR = "META-INF/stork-docs/";

    private final ProcessingEnvironment environment;

    public DocWriter(ProcessingEnvironment environment) {
        this.environment = environment;
    }

    public void createAttributeTable(String serviceRegistrarType, ServiceRegistrarAttribute[] attributes) throws IOException {
        String[][] rows = new String[attributes.length][];
        for (int i = 0; i < attributes.length; i++) {
            ServiceRegistrarAttribute attribute = attributes[i];
            rows[i] = new String[] { attribute.name(), attribute.required() ? "Yes" : "No", attribute.defaultValue(),
                    attribute.description() };
        }
        writeTables(serviceRegistrarType + "-sr-attributes", rows);
    }

    public void createAttributeTable(String serviceDiscoveryType, ServiceDiscoveryAttribute[] attributes) throws IOException {
        String[][] rows = new String[attributes.length][];
        for (int i = 0; i < attributes.length; i++) {
            ServiceDiscoveryAttribute attribute = attributes[i];
            rows[i] = new String[] { attribute.name(), attribute.required() ? "Yes" : "No", attribute.defaultValue(),
                    attribute.description() };
        }
        writeTables(serviceDiscoveryType + "-sd-attributes", rows);
    }

    public void createAttributeTable(String loadBalancerType, LoadBalancerAttribute[] attributes) throws IOException {
        String[][] rows = new String[attributes.length][];
        for (int i = 0; i < attributes.length; i++) {
            LoadBalancerAttribute attribute = attributes[i];
            rows[i] = new String[] { attribute.name(), attribute.required() ? "Yes" : "No", attribute.defaultValue(),
                    attribute.description() };
        }
        writeTables(loadBalancerType + "-lb-attributes", rows);
    }

    /**
     * Writes the attribute table twice: as Markdown ({@code .txt}) and as AsciiDoc ({@code .adoc}), which the
     * documentation published on smallrye.io includes.
     */
    private void writeTables(String baseName, String[][] rows) throws IOException {
        try (PrintWriter out = new PrintWriter(createResourceFile(baseName + ".txt").openWriter())) {
            out.println(" | Attribute            | Mandatory  | Default Value      | Description  |");
            out.println(" |----------------------|------------|--------------------|--------------|");
            for (String[] row : rows) {
                out.println(String.format("| `%s` | %s | %s | %s |", row[0], row[1], markdownDefault(row[2]), row[3]));
            }
        }
        try (PrintWriter out = new PrintWriter(createResourceFile(baseName + ".adoc").openWriter())) {
            out.println("[cols=\"2,1,2,5\",options=\"header\"]");
            out.println("|===");
            out.println("|Attribute |Mandatory |Default Value |Description");
            for (String[] row : rows) {
                out.println(String.format("|`%s` |%s |%s |%s", row[0], row[1], asciidocDefault(row[2]),
                        row[3] == null ? "" : row[3].replace("|", "\\|")));
            }
            out.println("|===");
        }
    }

    private static String markdownDefault(String value) {
        return Constants.DEFAULT_VALUE.equals(value) ? "" : '`' + value + '`';
    }

    private static String asciidocDefault(String value) {
        // a passthrough keeps characters such as * or { in default values literal
        return Constants.DEFAULT_VALUE.equals(value) ? "" : "`+" + value + "+`";
    }

    private FileObject createResourceFile(String fileName) throws IOException {
        FileObject loaderFile = environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                STORK_DOCS_DIR + fileName);
        loaderFile.delete();
        return loaderFile;
    }
}
