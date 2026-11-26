package org.dzoka.channels.adapters;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.dzoka.channels.ports.MessagingProvider;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public final class WhatsApp implements MessagingProvider {
    public static final VerifiedIdentity SERVICE=new VerifiedIdentity("urn:dzoka:channel","whatsapp");
    private final UUID tenant;
    private final String account,phone,verify,token,prefix,locale;
    private final byte[] appSecret,key;
    private final URI endpoint, graph;
    private final boolean loopback;
    private final ObjectMapper json;
    private final SecureRandom random=new SecureRandom();

    public WhatsApp(Environment env,ObjectMapper json) {
        this.json=json;
        String tenantValue=env.getProperty("DZOKA_WHATSAPP_TENANT","");
        tenant=tenantValue.isEmpty()?null:UUID.fromString(tenantValue);
        account=env.getProperty("DZOKA_WHATSAPP_ACCOUNT","");
        phone=env.getProperty("DZOKA_WHATSAPP_PHONE_ID","");
        verify=env.getProperty("DZOKA_WHATSAPP_VERIFY_TOKEN","");
        token=env.getProperty("DZOKA_WHATSAPP_ACCESS_TOKEN","");
        prefix=env.getProperty("DZOKA_WHATSAPP_TEMPLATE_PREFIX","dzoka_");
        locale=env.getProperty("DZOKA_WHATSAPP_TEMPLATE_LOCALE","en_US");
        String version=env.getProperty("DZOKA_WHATSAPP_GRAPH_VERSION","");
        String secret=env.getProperty("DZOKA_WHATSAPP_APP_SECRET","");
        String encryption=env.getProperty("DZOKA_WHATSAPP_CONTACT_KEY","");
        if(tenant!=null && (!account.matches("[0-9]{5,24}") || !phone.matches("[0-9]{5,24}") || verify.length()<24
                || !secret.matches("[0-9a-fA-F]{32,64}") || !encryption.matches("[0-9a-fA-F]{64}") || token.isBlank()
                || !version.matches("v[0-9]{2,3}\\.0") || !prefix.matches("[a-z][a-z0-9_]{0,39}")
                || !locale.matches("[a-z]{2}(_[A-Z]{2})?"))) throw new IllegalArgumentException("Invalid WhatsApp configuration");
        appSecret=secret.getBytes(StandardCharsets.UTF_8);
        key=tenant==null?new byte[0]:HexFormat.of().parseHex(encryption);
        URI origin=URI.create(env.getProperty("DZOKA_WHATSAPP_GRAPH_ORIGIN","https://graph.facebook.com"));
        boolean test=env.getProperty("DZOKA_WHATSAPP_TEST_LOOPBACK",Boolean.class,false)
                && "http".equals(origin.getScheme()) && "127.0.0.1".equals(origin.getHost()) && origin.getPort()>0;
        if(!origin.toString().equals("https://graph.facebook.com")
                && !(test && "".equals(origin.getRawPath()) && origin.getRawUserInfo()==null
                    && origin.getRawQuery()==null && origin.getRawFragment()==null))
            throw new IllegalArgumentException("Invalid WhatsApp endpoint");
        graph=origin.resolve(tenant==null?"/":"/"+version+"/");
        loopback=test;
        endpoint=graph.resolve(phone+"/messages");
    }

    public UUID tenant() {
        if(tenant==null) throw new Failures.NotFound();
        return tenant;
    }
    public String challenge(String mode,String supplied,String challenge) {
        tenant();
        if(!"subscribe".equals(mode) || !same(verify,supplied) || challenge==null || !challenge.matches("[0-9]{1,100}"))
            throw new Failures.Forbidden();
        return challenge;
    }
    public record Place(String replyTo,double latitude,double longitude,String source) {
        @Override public String toString() { return "PrivateWhatsAppPlace"; }
    }
    public record Attachment(String id,String type,String sha256) {}
    public record Inbound(String id,String address,String text,Place place, String replyTo, Attachment attachment,long timestamp) {
        @Override public String toString() { return "PrivateWhatsAppInbound"; }
    }
    public record Delivery(String id,String address,UUID operation,String state,long timestamp) {
        @Override public String toString() { return "PrivateWhatsAppDelivery"; }
    }
    public record Batch(List<Inbound> messages,List<Delivery> deliveries) {}

    public Batch parse(String signature,byte[] raw) {
        tenant();
        String expected="sha256="+HexFormat.of().formatHex(mac(appSecret,raw));
        if(!same(expected,signature)) throw new Failures.Forbidden();
        JsonNode root;
        try { root=json.readTree(raw); }
        catch(tools.jackson.core.JacksonException failure) { throw new IllegalArgumentException("Invalid webhook"); }
        if(root==null || !root.isObject() || !"whatsapp_business_account".equals(field(root,"object"))) throw new IllegalArgumentException("Invalid webhook");
        var messages=new ArrayList<Inbound>();
        var deliveries=new ArrayList<Delivery>();
        for(var entry:array(root,"entry",true)) {
            if(!account.equals(field(entry,"id"))) throw new Failures.Forbidden();
            for(var change:array(entry,"changes",true)) {
                if(!"messages".equals(field(change,"field"))) continue;
                var value=change.path("value");
                if(!"whatsapp".equals(field(value,"messaging_product")) || !phone.equals(field(value.path("metadata"),"phone_number_id")))
                    throw new Failures.Forbidden();
                for(var m:array(value,"messages",false)) {
                    if(fresh(m) && address(field(m,"from"))) messages.add(new Inbound(messageId(field(m,"id")),
                            field(m,"from"),"text".equals(field(m,"type"))?field(m.path("text"),"body"):null,
                            place(m),replyTo(m),attachment(m),timestamp(m)));
                }
                for(var s:array(value,"statuses",false)) {
                    String status=field(s,"status");
                    if(!fresh(s) || !address(field(s,"recipient_id")) || status==null
                            || !java.util.Set.of("sent","delivered","read","failed").contains(status)) continue;
                    String correlation=field(s,"biz_opaque_callback_data");
                    UUID operation=null;
                    if(correlation!=null && correlation.matches("[a-f0-9-]{36}")) {
                        try { operation=UUID.fromString(correlation); } catch(IllegalArgumentException ignored) { }
                    }
                    deliveries.add(new Delivery(messageId(field(s,"id")),field(s,"recipient_id"),operation,
                            status.toUpperCase(java.util.Locale.ROOT),timestamp(s)));
                }
                if(messages.size()+deliveries.size()>100) throw new IllegalArgumentException("Webhook batch exceeds limit");
            }
        }
        return new Batch(List.copyOf(messages),List.copyOf(deliveries));
    }

    private static Place place(JsonNode message) {
        if(!"location".equals(field(message,"type"))) return null;
        String reply=field(message.path("context"),"id");
        JsonNode value=message.path("location");
        var latitude=value.path("latitude");
        var longitude=value.path("longitude");
        if(reply==null || !reply.matches("[A-Za-z0-9_.=+/-]{5,255}") || !latitude.isNumber() || !longitude.isNumber()) return null;
        double lat=latitude.asDouble(),lon=longitude.asDouble();
        if(!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat)>90 || Math.abs(lon)>180) return null;
        String source=value.toString();
        return source.length()<=4000?new Place(reply,lat,lon,source):null;
    }

    private static String replyTo(JsonNode message) {
        String id=field(message.path("context"),"id");
        return id!=null && id.matches("[A-Za-z0-9_.=+/-]{5,255}")?id:null;
    }

    private static Attachment attachment(JsonNode message) {
        String kind=field(message,"type");
        if(kind==null || !java.util.Set.of("image","audio","video").contains(kind)) return null;
        JsonNode media=message.path(kind);
        String id=field(media,"id"),mime=field(media,"mime_type"),digest=field(media,"sha256");
        if(id==null || !id.matches("[0-9]{5,24}") || mime==null || digest==null) return null;
        String type=mime.split(";",2)[0];
        if(!type.startsWith(kind+"/") || !java.util.Set.of("image/jpeg","image/png","audio/ogg","audio/mpeg",
                "audio/mp4","audio/aac","audio/amr","video/mp4","video/3gpp").contains(type)) return null;
        try {
            byte[] hash=digest.matches("[0-9a-fA-F]{64}")?HexFormat.of().parseHex(digest):Base64.getDecoder().decode(digest);
            return hash.length==32?new Attachment(id,type,HexFormat.of().formatHex(hash)):null;
        } catch(IllegalArgumentException invalid) { return null; }
    }

    public String fingerprint(String purpose,String value) {
        tenant();
        return HexFormat.of().formatHex(mac(key,(purpose+"\n"+tenant+"\n"+value).getBytes(StandardCharsets.UTF_8)));
    }
    public String protect(UUID contact,String value) { return crypt(contact,value,true); }
    public String reveal(UUID contact,String value) { return crypt(contact,value,false); }
    private String crypt(UUID contact,String value,boolean encrypt) {
        tenant();
        try {
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");
            byte[] material=mac(key,"contact-encryption-v1".getBytes(StandardCharsets.UTF_8));
            byte[] nonce=new byte[12];
            byte[] encoded=encrypt?null:Base64.getDecoder().decode(value);
            if(encrypt) random.nextBytes(nonce);
            else System.arraycopy(encoded,0,nonce,0,nonce.length);
            cipher.init(encrypt?Cipher.ENCRYPT_MODE:Cipher.DECRYPT_MODE,new SecretKeySpec(material,"AES"),new GCMParameterSpec(128,nonce));
            cipher.updateAAD((tenant+":"+contact).getBytes(StandardCharsets.UTF_8));
            if(!encrypt) return new String(cipher.doFinal(encoded,nonce.length,encoded.length-nonce.length),StandardCharsets.UTF_8);
            byte[] payload=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(java.nio.ByteBuffer.allocate(nonce.length+payload.length).put(nonce).put(payload).array());
        } catch (GeneralSecurityException | IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new Failures.Unavailable(new IllegalStateException("Contact route unavailable"));
        }
    }

    @Override public Submission submit(UUID id,String address,Template template,String reply) {
        tenant();
        HttpURLConnection connection=null;
        try {
            connection=connect(endpoint);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            var packet=new LinkedHashMap<String,Object>(Map.of("messaging_product","whatsapp","recipient_type","individual",
                    "to",address,"biz_opaque_callback_data",id.toString()));
            if(template==Template.SESSION_REPLY) {
                if(reply==null || reply.isBlank() || reply.length()>2000) throw new IllegalArgumentException("Invalid session reply");
                packet.put("type","text");
                packet.put("text",Map.of("body",reply,"preview_url",false));
            } else {
                packet.put("type","template");
                packet.put("template",Map.of("name",prefix+template.name().toLowerCase(java.util.Locale.ROOT),"language",Map.of("code",locale)));
            }
            byte[] body=json.writeValueAsBytes(packet);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            try(var out=connection.getOutputStream()) { out.write(body); }
            int response=connection.getResponseCode();
            if(response==429) return new Submission(Result.RETRY,null);
            if(response>=400 && response<500 && response!=408) return new Submission(Result.FAILED,null);
            if(response!=200) return new Submission(Result.UNKNOWN,null);
            try(var in=connection.getInputStream()) {
                byte[] bytes=in.readNBytes(16385);
                if(bytes.length>16384) return new Submission(Result.UNKNOWN,null);
                String accepted=field(json.readTree(bytes).path("messages").path(0),"id");
                return new Submission(Result.SENT,messageId(accepted));
            }
        } catch(IOException | RuntimeException failure) {
            return new Submission(Result.UNKNOWN,null);
        } finally { if(connection!=null) connection.disconnect(); }
    }

    @Override public byte[] download(String id,String type,String sha256) {
        tenant();
        if(!id.matches("[0-9]{5,24}")) throw new IllegalArgumentException("Invalid media ID");
        try {
            var metadata=json.readTree(get(graph.resolve(id+"?phone_number_id="+phone),16384));
            if(!id.equals(field(metadata,"id"))) throw new IllegalArgumentException("Media binding failed");
            var content=URI.create(metadata.path("url").asString(""));
            boolean official="https".equals(content.getScheme()) && "lookaside.fbsbx.com".equals(content.getHost())
                    && (content.getPort()==-1 || content.getPort()==443);
            boolean fixture=loopback && graph.getScheme().equals(content.getScheme())
                    && graph.getHost().equals(content.getHost()) && graph.getPort()==content.getPort();
            if(!(official || fixture) || content.getRawUserInfo()!=null || content.getRawFragment()!=null)
                throw new IllegalArgumentException("Invalid media URL");
            String mime=field(metadata,"mime_type");
            if(mime==null || !type.equals(mime.split(";",2)[0])) throw new IllegalArgumentException("Media type mismatch");
            int limit=5*1024*1024;
            var size=metadata.path("file_size");
            String length=size.isIntegralNumber() || size.isTextual()?size.asString():"";
            if(!length.matches("[1-9][0-9]{0,6}") || Integer.parseInt(length)>limit)
                throw new IllegalArgumentException("Invalid media size");
            int expected=Integer.parseInt(length);
            byte[] bytes=get(content,expected);
            if(bytes.length!=expected || !sha256.equals(ContentDigest.sha256(bytes)))
                throw new IllegalArgumentException("Media hash mismatch");
            return bytes;
        } catch(IOException | tools.jackson.core.JacksonException failure) {
            throw new Failures.Unavailable(new IllegalStateException("WhatsApp media unavailable"));
        }
    }

    private byte[] get(URI uri,int limit) throws IOException {
        HttpURLConnection connection=connect(uri);
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        try {
            int status=connection.getResponseCode();
            if(status==404 || status==410 || status/100==3) throw new IllegalArgumentException("Media unavailable");
            if(status!=200) throw new IOException("Media request unavailable");
            if(connection.getContentLengthLong()>limit) throw new IllegalArgumentException("Media size exceeds limit");
            try(var in=connection.getInputStream()) {
                var output=new java.io.ByteArrayOutputStream(Math.min(8192,limit));
                byte[] buffer=new byte[8192];
                while(System.nanoTime()<deadline) {
                    int read=in.read(buffer,0,Math.min(buffer.length,limit+1-output.size()));
                    if(read<0) return output.toByteArray();
                    if(output.size()+read>limit) throw new IllegalArgumentException("Media size exceeds limit");
                    output.write(buffer,0,read);
                }
                throw new IOException("Media download deadline exceeded");
            }
        } finally { connection.disconnect(); }
    }

    private HttpURLConnection connect(URI uri) throws IOException {
        var connection=(HttpURLConnection)uri.toURL().openConnection(Proxy.NO_PROXY);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Authorization","Bearer "+token);
        return connection;
    }
    private static boolean same(String a,String b) {
        return b!=null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));
    }
    private static byte[] mac(byte[] key,byte[] message) {
        try { Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key,"HmacSHA256")); return mac.doFinal(message); }
        catch(GeneralSecurityException failure) { throw new IllegalStateException("HMAC unavailable"); }
    }
    private static String field(JsonNode node,String name) { var value=node.path(name); return value.isTextual()?value.asString():null; }
    private static JsonNode array(JsonNode node,String name,boolean required) {
        var value=node.path(name);
        if((required || !value.isMissingNode()) && (!value.isArray() || value.size()>100)) throw new IllegalArgumentException("Invalid webhook array");
        return value;
    }
    public static boolean address(String value) { return value!=null && value.matches("[1-9][0-9]{6,14}"); }
    private static String messageId(String value) {
        if(value==null || !value.matches("[A-Za-z0-9_.=+/-]{5,255}")) throw new IllegalArgumentException("Invalid provider ID");
        return value;
    }
    private static long timestamp(JsonNode event) {
        try { return Long.parseLong(field(event,"timestamp")); }
        catch(NumberFormatException failure) { return 0; }
    }
    private static boolean fresh(JsonNode event) {
        long now=Instant.now().getEpochSecond(), eventTime=timestamp(event);
        return eventTime>=now-604800 && eventTime<=now+300;
    }
}
