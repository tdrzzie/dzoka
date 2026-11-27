import java.util.List;
import org.dzoka.places.domain.PlaceDraft;
import org.dzoka.places.adapters.PlaceCommandHash;
class CanonicalPlaceRetryHashProbe {
 public static void main(String[] args) {
  var d=new PlaceDraft("Synthetic institution","FACILITY",null,null,null,null,"Africa/Harare",List.of("Synthetic alias"),true,0L);
  System.out.println("PROBE "+PlaceCommandHash.CURRENT_VERSION+" "+PlaceCommandHash.canonical(null,d));
 }
}
