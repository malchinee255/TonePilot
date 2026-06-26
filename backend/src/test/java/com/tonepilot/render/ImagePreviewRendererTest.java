package com.tonepilot.render;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.LightroomHslParams;
import com.tonepilot.domain.Photo;
import com.tonepilot.service.ObjectStorageService;
import com.tonepilot.service.StoredFile;
import com.tonepilot.service.StoredObject;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImagePreviewRendererTest {

    @Test
    void rendersPreviewImageIntoObjectStorage() throws Exception {
        FakeStorage storage = new FakeStorage();
        String originalUrl = storage.putOriginal(sampleImage());
        ImagePreviewRenderer renderer = new ImagePreviewRenderer(storage);

        PreviewRender preview = renderer.render(
                new Photo(1L, "sample.png", originalUrl, "png", Instant.now()),
                adjustment(),
                "session-1",
                2
        );

        assertThat(preview.originalUrl()).isEqualTo(originalUrl);
        assertThat(preview.previewUrl()).isEqualTo("/files/previews/session-1-2.jpg");
        StoredObject object = storage.readObject(preview.previewUrl());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(object.bytes()));
        assertThat(image.getWidth()).isEqualTo(4);
        assertThat(image.getHeight()).isEqualTo(4);
        assertThat(new Color(image.getRGB(0, 0)).getRed()).isGreaterThan(80);
    }

    private byte[] sampleImage() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, new Color(80, 80, 80).getRGB());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private ColorAdjustment adjustment() {
        return new ColorAdjustment(
                1L,
                1L,
                "明亮暖调",
                "测试预览渲染",
                new LightroomBasicParams(0.6, 12, -10, 10, 0, 0, 12, 0, 0, 5, 0, 8, 4),
                new LightroomHslParams(
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0
                ),
                new LightroomEffectsParams(0, 0),
                List.of("提高曝光"),
                Map.of(),
                Instant.now()
        );
    }

    private static class FakeStorage implements ObjectStorageService {
        private final Map<String, StoredObject> objects = new HashMap<>();

        String putOriginal(byte[] bytes) {
            objects.put("/files/photos/sample.png", new StoredObject(bytes, "image/png", "sample.png"));
            return "/files/photos/sample.png";
        }

        @Override
        public StoredFile storeImage(MultipartFile file, String folder) {
            throw new UnsupportedOperationException("测试不需要上传文件");
        }

        @Override
        public String writeTextFile(String folder, String fileName, String content) {
            throw new UnsupportedOperationException("测试不需要写文本");
        }

        @Override
        public String writeBinaryFile(String folder, String fileName, byte[] bytes, String contentType) {
            String url = "/files/" + folder + "/" + fileName;
            objects.put(url, new StoredObject(bytes, contentType, fileName));
            return url;
        }

        @Override
        public StoredObject readObject(String fileUrl) {
            return objects.get(fileUrl);
        }

        @Override
        public String readAsDataUrl(String fileUrl) {
            throw new UnsupportedOperationException("测试不需要 data url");
        }

        @Override
        public String slug(String value) {
            return value;
        }
    }
}
