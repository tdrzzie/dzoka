import java.util.*;
import org.dzoka.places.domain.PlaceDraft;
import org.dzoka.identity.domain.VerifiedIdentity;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
class SpringPlaceRetryHashProbe {
 @Configuration(proxyBeanMethods=false)
 @ImportAutoConfiguration(JacksonAutoConfiguration.class)
 static class Config {}
 public static void main(String[] args) {
  try(var context=new SpringApplicationBuilder(Config.class).web(WebApplicationType.NONE).properties("spring.main.banner-mode=off","logging.level.root=OFF").run()) {
   var mapper=context.getBean(ObjectMapper.class);
   var d=new PlaceDraft("Synthetic institution","FACILITY",null,null,null,null,"Africa/Harare",List.of("Synthetic alias"),true,0L);
   String hash=VerifiedIdentity.digest(mapper.writeValueAsString(Map.of("id","CREATE","draft",d)));
   System.out.println("PROBE:"+mapper.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)+":"+hash);
  }
 }
}
