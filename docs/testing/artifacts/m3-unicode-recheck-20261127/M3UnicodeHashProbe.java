import java.util.List;
import org.dzoka.places.domain.PlaceDraft;
import org.dzoka.places.adapters.PlaceCommandHash;
import tools.jackson.databind.json.JsonMapper;
class M3UnicodeHashProbe {
 public static void main(String[] args) {
  var mapper=JsonMapper.builder().build();
  String common=",\"kind\":\"FACILITY\",\"parentId\":null,\"latitude\":null,\"longitude\":null,\"uncertaintyMeters\":null,\"timezone\":\"Africa/Harare\",\"aliases\":[],\"active\":true,\"expectedVersion\":0}";
  var a=mapper.readValue("{\"name\":\"?\""+common,PlaceDraft.class);
  String escaped="\\"+"ud800";
  var b=mapper.readValue("{\"name\":\""+escaped+"\""+common,PlaceDraft.class);
  System.out.println("PROBE namesEqual="+a.name().equals(b.name()));
  System.out.println("PROBE secondCodeUnit="+Integer.toHexString(b.name().charAt(0)));
  System.out.println("PROBE digestA="+PlaceCommandHash.canonical(null,a));
  System.out.println("PROBE digestB="+PlaceCommandHash.canonical(null,b));
  System.out.println("PROBE acceptsChangedBody="+PlaceCommandHash.matches(2,PlaceCommandHash.canonical(null,a),null,b));
 }
}
