package com.escape.core.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FXAA {

    private static final float[] LUMINANCE_WEIGHTS = {0.299f, 0.587f, 0.114f};
    private static final float EDGE_THRESHOLD = 0.05f; // Adjustable edge threshold
    private static final int[] OFFSETS = {-1, 0, 1};

    public static BufferedImage applyFXAA(BufferedImage image, int numPasses) {
        int width = image.getWidth();
        int height = image.getHeight();

        float[][] luminanceMap = new float[width][height];
        BufferedImage result = new BufferedImage(width, height, image.getType());

        // Initialize pixel data and luminance map
        computeLuminanceMap(image, luminanceMap);

        for (int pass = 0; pass < numPasses; pass++) {
            applyFXAASinglePass(image, result, luminanceMap);
            // Swap images for the next pass
            BufferedImage temp = image;
            image = result;
            result = temp;
        }

        return image;
    }

    private static void computeLuminanceMap(BufferedImage image, float[][] luminanceMap) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                float lum = luminance(rgb);
                luminanceMap[x][y] = lum;
            }
        }
    }

    private static void applyFXAASinglePass(BufferedImage input, BufferedImage output, float[][] luminanceMap) {
        int width = input.getWidth();
        int height = input.getHeight();

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float lum = luminanceMap[x][y];

                // Edge detection using precomputed luminance values
                float edgeDetect = computeEdgeDetection(luminanceMap, x, y);

                if (edgeDetect > EDGE_THRESHOLD) {
                    // Adaptive blending using precomputed luminance values
                    int resultColor = adaptiveBlend(input, luminanceMap, x, y, edgeDetect);
                    output.setRGB(x, y, resultColor);
                } else {
                    output.setRGB(x, y, input.getRGB(x, y));
                }
            }
        }
    }

    private static float luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return LUMINANCE_WEIGHTS[0] * r / 255f
                + LUMINANCE_WEIGHTS[1] * g / 255f
                + LUMINANCE_WEIGHTS[2] * b / 255f;
    }

    private static float computeEdgeDetection(float[][] luminanceMap, int x, int y) {
        float lum = luminanceMap[x][y];
        float edgeDetect = 0;
        for (int i : OFFSETS) {
            for (int j : OFFSETS) {
                if (i != 0 || j != 0) {
                    edgeDetect += Math.abs(luminanceMap[x + i][y + j] - lum);
                }
            }
        }
        return edgeDetect / 8; // Average edge strength
    }

    private static int adaptiveBlend(BufferedImage image, float[][] luminanceMap, int x, int y, float edgeDetect) {
        int width = image.getWidth();
        int height = image.getHeight();

        float[] sum = new float[3];
        float weightSum = 0;

        for (int i : OFFSETS) {
            for (int j : OFFSETS) {
                int sampleX = clamp(x + i, 0, width - 1);
                int sampleY = clamp(y + j, 0, height - 1);

                int rgb = image.getRGB(sampleX, sampleY);
                float sampleLum = luminanceMap[sampleX][sampleY];
                float weight = 1.0f - Math.abs(sampleLum - luminanceMap[x][y]) * edgeDetect;

                sum[0] += ((rgb >> 16) & 0xFF) * weight;
                sum[1] += ((rgb >> 8) & 0xFF) * weight;
                sum[2] += (rgb & 0xFF) * weight;
                weightSum += weight;
            }
        }

        int r = clamp((int) (sum[0] / weightSum), 0, 255);
        int g = clamp((int) (sum[1] / weightSum), 0, 255);
        int b = clamp((int) (sum[2] / weightSum), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void main(String[] args) throws IOException {
        // Example usage:
        BufferedImage inputImage = ImageIO.read(new File("input.png"));
        BufferedImage outputImage = applyFXAA(inputImage, 3); // Apply FXAA with 3 passes
        ImageIO.write(outputImage, "png", new File("output.png"));
    }
}
