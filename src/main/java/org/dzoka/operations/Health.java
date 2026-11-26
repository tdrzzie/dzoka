package org.dzoka.operations;

import java.sql.*;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Process/DB readiness only. Provider readiness is tenant-authorized operational state. */
@RestController
public final class Health {
    private final DataSource dataSource;
    public Health(DataSource dataSource){this.dataSource=dataSource;}
    @GetMapping("/health/live") public ResponseEntity<Map<String,String>> live(){return reply(200,"UP");}
    @GetMapping("/health/ready") public ResponseEntity<Map<String,String>> ready(){
        try(var db=dataSource.getConnection();var s=db.prepareStatement("SELECT 1")){s.setQueryTimeout(2);try(var r=s.executeQuery()){if(r.next()&&r.getInt(1)==1)return reply(200,"UP");}}
        catch(SQLException unavailable){return reply(503,"UNAVAILABLE");}return reply(503,"UNAVAILABLE");
    }
    private static ResponseEntity<Map<String,String>> reply(int status,String state){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("status",state));}
}
