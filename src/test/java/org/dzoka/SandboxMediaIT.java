package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.*;
import java.util.zip.CRC32;
import org.dzoka.media.domain.MediaView;
import org.dzoka.media.ports.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SandboxMediaIT extends S3Fixture {
    private MediaView uploadImage(byte[] bytes,String type) throws Exception {
        var response=upload(report(),bytes,type,UUID.randomUUID().toString());assertEquals(200,response.statusCode());
        return app.getBean(ObjectMapper.class).readValue(response.body(),MediaView.class);
    }
    @Test void realSandboxCanonicalizesPixelsDropsMetadataAndOnlyThenAllowsPreview() throws Exception {
        byte[] plain=png(),text="GPS\0Synthetic-Secret-GPS".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var body=new java.io.ByteArrayOutputStream();body.write(plain,0,plain.length-12);
        body.write(ByteBuffer.allocate(4).putInt(text.length).array());byte[] type="tEXt".getBytes();body.write(type);body.write(text);
        CRC32 crc=new CRC32();crc.update(type);crc.update(text);body.write(ByteBuffer.allocate(4).putInt((int)crc.getValue()).array());body.write(plain,plain.length-12,12);
        var view=uploadImage(body.toByteArray(),"image/png");assertTrue(worker().processOne(who("processor"),A));
        var state=app.getBean(MediaStore.class).read(who("owner"),A,view.id());assertEquals("SANITIZED",state.state());assertEquals(8,state.width());assertEquals(7,state.height());
        var response=media(A,view.id(),"owner","/preview");assertEquals(200,response.statusCode());
        assertFalse(new String(response.body(),java.nio.charset.StandardCharsets.ISO_8859_1).contains("Synthetic-Secret-GPS"));
        assertEquals("no-store",response.headers().firstValue("cache-control").orElseThrow());
    }
    @Test void spoofedAndMalformedImageContentNeverActivates() throws Exception {
        var invalid=uploadImage("<svg><script>bad</script></svg>".getBytes(),"image/png");
        assertTrue(worker().processOne(who("processor"),A));assertEquals("REJECTED",app.getBean(MediaStore.class).read(who("owner"),A,invalid.id()).state());
        assertEquals(409,media(A,invalid.id(),"owner","/preview").statusCode());
        var mismatch=uploadImage(png(),"image/jpeg");assertTrue(worker().processOne(who("processor"),A));
        assertEquals("REJECTED",app.getBean(MediaStore.class).read(who("owner"),A,mismatch.id()).state());
        byte[] truncated=Arrays.copyOf(png(),png().length-1);
        byte[] excessive=png();ByteBuffer.wrap(excessive,16,8).putInt(8192).putInt(8192);
        CRC32 crc=new CRC32();crc.update(excessive,12,17);ByteBuffer.wrap(excessive,29,4).putInt((int)crc.getValue());
        for(byte[] malformed:List.of(truncated,excessive)){
            var view=uploadImage(malformed,"image/png");assertTrue(worker().processOne(who("processor"),A));
            assertEquals("REJECTED",app.getBean(MediaStore.class).read(who("owner"),A,view.id()).state());
        }
    }
    @Test void jpegAlsoProducesOnlyCanonicalPng() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(9,6,java.awt.image.BufferedImage.TYPE_INT_RGB),"jpeg",bytes);
        var view=uploadImage(bytes.toByteArray(),"image/jpeg");assertTrue(worker().processOne(who("processor"),A));
        var state=app.getBean(MediaStore.class).read(who("owner"),A,view.id());assertEquals("SANITIZED",state.state());
        var preview=media(A,view.id(),"owner","/preview");assertEquals(200,preview.statusCode());
        assertArrayEquals(new int[]{9,6},org.dzoka.media.sanitizer.CanonicalPng.inspect(preview.body()));
    }
}
