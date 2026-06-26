package com.tonepilot.render;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.Photo;
import com.tonepilot.service.ObjectStorageService;
import com.tonepilot.service.StoredObject;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Iterator;
import java.util.Random;

@Service
public class ImagePreviewRenderer {

    private static final int MAX_PREVIEW_EDGE = 1600;

    private final ObjectStorageService storageService;

    public ImagePreviewRenderer(ObjectStorageService storageService) {
        this.storageService = storageService;
    }

    public PreviewRender render(Photo photo, ColorAdjustment adjustment, String sessionId, int turnIndex) {
        StoredObject source = storageService.readObject(photo.fileUrl());
        BufferedImage image = readImage(source);
        BufferedImage normalized = resizeIfNeeded(toRgb(image));
        BufferedImage rendered = applyAdjustment(normalized, adjustment);
        byte[] bytes = writeJpeg(rendered);
        String fileName = sessionId + "-" + turnIndex + ".jpg";
        String previewUrl = storageService.writeBinaryFile("previews", fileName, bytes, "image/jpeg");
        return new PreviewRender(photo.fileUrl(), previewUrl, Instant.now());
    }

    private BufferedImage readImage(StoredObject source) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(source.bytes()));
            if (image == null) {
                throw new IllegalArgumentException("暂不支持该图片格式：" + source.fileName());
            }
            return image;
        } catch (Exception exception) {
            throw new IllegalStateException("读取预览源图失败：" + source.fileName(), exception);
        }
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage resizeIfNeeded(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int edge = Math.max(width, height);
        if (edge <= MAX_PREVIEW_EDGE) {
            return source;
        }
        double scale = (double) MAX_PREVIEW_EDGE / edge;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage applyAdjustment(BufferedImage source, ColorAdjustment adjustment) {
        LightroomBasicParams basic = adjustment.basic();
        LightroomEffectsParams effects = adjustment.effects();
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        double exposureScale = Math.pow(2, basic.exposure());
        double contrastScale = 1 + (basic.contrast() + basic.clarity() * 0.35 + basic.dehaze() * 0.25) / 100.0;
        double saturationScale = Math.max(0, 1 + (basic.saturation() + basic.vibrance() * 0.75) / 100.0);
        double temperature = basic.temperature() / 100.0;
        double tint = basic.tint() / 100.0;
        long seed = 31L + (adjustment.id() == null ? 0L : adjustment.id());
        Random grain = new Random(seed);

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                Color color = new Color(source.getRGB(x, y));
                double r = color.getRed() / 255.0;
                double g = color.getGreen() / 255.0;
                double b = color.getBlue() / 255.0;

                r *= exposureScale;
                g *= exposureScale;
                b *= exposureScale;

                double luminance = luminance(r, g, b);
                double shadowLift = basic.shadows() / 100.0 * Math.max(0, 0.55 - luminance);
                double highlightPull = basic.highlights() / 100.0 * Math.max(0, luminance - 0.45);
                r += shadowLift + highlightPull;
                g += shadowLift + highlightPull;
                b += shadowLift + highlightPull;

                r = (r - 0.5) * contrastScale + 0.5;
                g = (g - 0.5) * contrastScale + 0.5;
                b = (b - 0.5) * contrastScale + 0.5;

                r += temperature * 0.08 + tint * 0.03;
                g -= tint * 0.04;
                b -= temperature * 0.08 - tint * 0.03;

                double gray = luminance(r, g, b);
                r = gray + (r - gray) * saturationScale;
                g = gray + (g - gray) * saturationScale;
                b = gray + (b - gray) * saturationScale;

                if (effects != null && effects.vignette() != 0) {
                    double vignette = vignetteFactor(x, y, source.getWidth(), source.getHeight(), effects.vignette());
                    r *= vignette;
                    g *= vignette;
                    b *= vignette;
                }

                if (effects != null && effects.grain() > 0) {
                    double noise = (grain.nextDouble() - 0.5) * effects.grain() / 300.0;
                    r += noise;
                    g += noise;
                    b += noise;
                }

                target.setRGB(x, y, new Color(clampColor(r), clampColor(g), clampColor(b)).getRGB());
            }
        }
        return target;
    }

    private double luminance(double r, double g, double b) {
        return 0.299 * r + 0.587 * g + 0.114 * b;
    }

    private double vignetteFactor(int x, int y, int width, int height, int vignette) {
        double dx = (x - width / 2.0) / Math.max(1, width / 2.0);
        double dy = (y - height / 2.0) / Math.max(1, height / 2.0);
        double distance = Math.min(1, Math.sqrt(dx * dx + dy * dy));
        double strength = Math.abs(vignette) / 100.0 * distance * distance * 0.6;
        return vignette < 0 ? 1 - strength : 1 + strength * 0.5;
    }

    private int clampColor(double value) {
        return (int) Math.round(Math.max(0, Math.min(1, value)) * 255);
    }

    private byte[] writeJpeg(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                ImageIO.write(image, "jpg", output);
                return output.toByteArray();
            }
            ImageWriter writer = writers.next();
            try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(imageOutput);
                ImageWriteParam params = writer.getDefaultWriteParam();
                if (params.canWriteCompressed()) {
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(0.88f);
                }
                writer.write(null, new IIOImage(image, null, null), params);
            } finally {
                writer.dispose();
            }
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("写入预览图失败", exception);
        }
    }
}
