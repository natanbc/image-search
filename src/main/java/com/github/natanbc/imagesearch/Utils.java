package com.github.natanbc.imagesearch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static void rm(String path) {
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (IOException e) {
            System.err.println("Error deleting image:");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }
    }

    public static String copyToIndex(String path, String hash) {
        try {
            var dest = Path.of("index", hash + ".png");
            Files.createDirectories(dest.getParent());
            Files.copy(Path.of(path), dest);
            return dest.toString();
        } catch (IOException e) {
            System.err.println("Error copying file to index:");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }
    }

    public static BufferedImage readImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] getData(BufferedImage image) {
        var buffer = image.getRaster().getDataBuffer();
        if(buffer instanceof DataBufferByte) {
            return ((DataBufferByte) buffer).getData();
        }
        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            // ByteArrayOutputStream never throws IOException
            throw new AssertionError(e);
        }
        return baos.toByteArray();
    }

    public static String hash(byte[] array) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            // SHA1 should be supported
            throw new AssertionError(e);
        }
        md.update(array);
        return toHex(md.digest());
    }

    private static String toHex(byte[] b) {
        var result = new StringBuilder();
        for (var value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
