package org.dzoka;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.ObjectMapper;
public class UnicodePlaceHttpProbe extends IntegrationFixture {
 @Override protected void initializeProviders()throws Exception {membership(A,"unicode-manager","places:manage","places:read");}
 @Override protected List<String> extraOptions(){return List.of("--dzoka.media.enabled=false","--logging.level.root=WARN");}
 private HttpResponse<String> send(String body,String idem)throws Exception {
  var r=HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+A+"/places")).timeout(Duration.ofSeconds(15))
   .header("Authorization","Bearer "+token("unicode-manager")).header("Content-Type","application/json").header("Idempotency-Key",idem)
   .POST(HttpRequest.BodyPublishers.ofString(body)).build();
  return http.send(r,HttpResponse.BodyHandlers.ofString());
 }
 public static void main(String[] args)throws Exception {
  var p=new UnicodePlaceHttpProbe();
  try {
   p.start();String common=",\"kind\":\"FACILITY\",\"parentId\":null,\"latitude\":null,\"longitude\":null,\"uncertaintyMeters\":null,\"timezone\":\"Africa/Harare\",\"aliases\":[],\"active\":true,\"expectedVersion\":0}";
   String a="{\"name\":\"?\""+common;String b="{\"name\":\""+"\\"+"ud800\""+common;String key=UUID.randomUUID().toString();
   long before=p.count("places",p.A);var first=p.send(a,key);var changed=p.send(b,key);
   var different=p.send("{\"name\":\"Different\""+common,key);
   var mapper=p.app.getBean(ObjectMapper.class);
   System.out.println("PROBE firstStatus="+first.statusCode());
   System.out.println("PROBE changedMalformedStatus="+changed.statusCode());
   System.out.println("PROBE sameOriginalResult="+mapper.readTree(first.body()).equals(mapper.readTree(changed.body())));
   System.out.println("PROBE ordinaryChangedStatus="+different.statusCode());
   System.out.println("PROBE addedPlaces="+(p.count("places",p.A)-before));
   if(first.statusCode()!=200 || changed.statusCode()!=200 || different.statusCode()!=409)throw new IllegalStateException("Probe observation differed");
  } finally {p.stop();}
 }
}
