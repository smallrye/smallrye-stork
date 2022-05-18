package examples;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import io.smallrye.stork.api.config.ServiceDiscoveryConfig;

/**
 *  Configuration for the {@code AcmeServiceDiscoveryProvider} ServiceDiscovery.
 */
 public class AcmeConfiguration implements io.smallrye.stork.api.config.ServiceDiscoveryConfig{
   private final Map<String, String> parameters;

   /**
    * Creates a new AcmeConfiguration
    *
    * @param params the parameters, must not be {@code null}
    */
   public AcmeConfiguration(Map<String, String> params) {
      parameters = Collections.unmodifiableMap(params);
   }

   /**
    * Creates a new AcmeConfiguration
    */
   public AcmeConfiguration() {
      parameters = Collections.emptyMap();
   }


  /**
   * @return the type
   */
   @Override
   public String type() {
      return "acme";
   }


   /**
    * @return the parameters
    */
   @Override
   public Map<String, String> parameters() {
      return parameters;
   }

   private AcmeConfiguration extend(String key, String value) {
      Map<String, String> copy = new HashMap<>(parameters);
      copy.put(key, value);
      return new AcmeConfiguration(copy);
   }

   /**
    * Host name of the service discovery server.
    *
    * @return the configured host, @{code null} if not set
    */
   public String getHost() {
      return parameters.get("host");
   }

   /**
    * Set the 'host' attribute.
    * 
    * @param value the value for host
    * @return the current AcmeConfiguration to chain calls
    */
   public AcmeConfiguration withHost(String value) {
      return extend("host", value);
   }

   /**
    * Hort of the service discovery server.
    *
    * @return the configured port, @{code null} if not set
    */
   public String getPort() {
      return parameters.get("port");
   }

   /**
    * Set the 'port' attribute.
    * 
    * @param value the value for port
    * @return the current AcmeConfiguration to chain calls
    */
   public AcmeConfiguration withPort(String value) {
      return extend("port", value);
   }
}
