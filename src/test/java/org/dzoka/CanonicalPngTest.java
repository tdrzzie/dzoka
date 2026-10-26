package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.dzoka.media.sanitizer.CanonicalPng;
import org.junit.jupiter.api.Test;
class CanonicalPngTest {
    @Test void outputValidationRejectsMetadataAndTamperingWithoutDecoding() throws Exception {
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(3,2,BufferedImage.TYPE_INT_ARGB),"png",output);
        byte[] bytes=output.toByteArray();assertArrayEquals(new int[]{3,2},CanonicalPng.inspect(bytes));
        byte[] corrupt=bytes.clone();corrupt[20]^=1;assertThrows(IllegalArgumentException.class,()->CanonicalPng.inspect(corrupt));
        assertThrows(IllegalArgumentException.class,()->CanonicalPng.inspect(java.util.Arrays.copyOf(bytes,bytes.length+1)));
        assertThrows(IllegalArgumentException.class,()->CanonicalPng.inspect("<svg/>".getBytes()));
    }
}
