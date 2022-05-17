package examples;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import io.smallrye.stork.api.config.LoadBalancerConfig;

/**
 *  Configuration for the {@code AcmeLoadBalancerProvider} LoadBalancer.
 */
 public class AcmeLoadBalancerConfiguration implements io.smallrye.stork.api.config.LoadBalancerConfig{
   private final Map<String, String> parameters;

   /**
    * Creates a new AcmeLoadBalancerConfiguration
    *
    * @param params the parameters, must not be {@code null}
    */
   public AcmeLoadBalancerConfiguration(Map<String, String> params) {
      parameters = Collections.unmodifiableMap(params);
   }

   /**
    * Creates a new AcmeLoadBalancerConfiguration
    */
   public AcmeLoadBalancerConfiguration() {
      parameters = Collections.emptyMap();
   }


  /**
   * @return the type
   */
   @Override
   public String type() {
      return "acme-load-balancer";
   }


   /**
    * @return the parameters
    */
   @Override
   public Map<String, String> parameters() {
      return parameters;
   }

   private AcmeLoadBalancerConfiguration extend(String key, String value) {
      Map<String, String> copy = new HashMap<>(parameters);
      copy.put(key, value);
      return new AcmeLoadBalancerConfiguration(copy);
   }

   /**
    * Attribute that alters the behavior of the LoadBalancer
    *
    * @return the configured my-attribute, @{code null} if not set
    */
   public String getMyAttribute() {
      return parameters.get("my-attribute");
   }

   /**
    * Set the 'my-attribute' attribute.
    * 
    * @param value the value for my-attribute
    * @return the current AcmeLoadBalancerConfiguration to chain calls
    */
   public AcmeLoadBalancerConfiguration withMyAttribute(String value) {
      return extend("my-attribute", value);
   }
}
