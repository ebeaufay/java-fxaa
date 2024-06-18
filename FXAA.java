
import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

public class FXAA {

    private static final float[] LUMINANCE_WEIGHTS = {0.299f, 0.587f, 0.114f};
    private static final float EDGE_THRESHOLD = 0.05f; // Adjustable edge threshold
    private static final int[] OFFSETS = {-1, 0, 1};

    public static void applyFXAA(BufferedImage image, int numPasses) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][] luminanceMap = new float[width][height];
        int[] pixels = new int[image.getWidth()* image.getHeight()];
        image.getRGB(0,0,image.getWidth(), image.getHeight(),pixels, 0,  image.getWidth());
        

        int[] originalPixels = pixels.clone();

        // Initialize luminance map
        computeLuminanceMap(originalPixels, luminanceMap, width, height);

        for (int pass = 0; pass < numPasses; pass++) {
            applyFXAASinglePass(originalPixels, pixels, luminanceMap, width, height);
            int[] temp = originalPixels;
            originalPixels = pixels;
            pixels = temp;
        }

        image.setRGB(0,0,image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
    }

    
    private static void computeLuminanceMap(int[] pixels, float[][] luminanceMap, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = pixels[y * width + x];
                luminanceMap[x][y] = luminance(rgb);
            }
        }
    }

    private static void applyFXAASinglePass(int[] originalPixels, int[] pixels, float[][] luminanceMap, int width, int height) {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float lum = luminanceMap[x][y];

                // Edge detection using precomputed luminance values
                float edgeDetect = computeEdgeDetection(luminanceMap, x, y);

                if (edgeDetect > EDGE_THRESHOLD) {
                    // Adaptive blending using precomputed luminance values
                    pixels[y * width + x] = adaptiveBlend(originalPixels, luminanceMap, x, y, edgeDetect, width, height);
                } else {
                    pixels[y * width + x] = originalPixels[y * width + x];
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

    private static int adaptiveBlend(int[] pixels, float[][] luminanceMap, int x, int y, float edgeDetect, int width, int height) {
        float[] sum = new float[4]; // Including alpha
        float weightSum = 0;

        for (int i : OFFSETS) {
            for (int j : OFFSETS) {
                int sampleX = clamp(x + i, 0, width - 1);
                int sampleY = clamp(y + j, 0, height - 1);

                int argb = pixels[sampleY * width + sampleX];
                float sampleLum = luminanceMap[sampleX][sampleY];
                float weight = 1.0f - Math.abs(sampleLum - luminanceMap[x][y]) * edgeDetect;

                sum[0] += ((argb >> 24) & 0xFF) * weight; // Alpha
                sum[1] += ((argb >> 16) & 0xFF) * weight; // Red
                sum[2] += ((argb >> 8) & 0xFF) * weight;  // Green
                sum[3] += (argb & 0xFF) * weight;         // Blue
                weightSum += weight;
            }
        }

        int a = clamp((int) (sum[0] / weightSum), 0, 255);
        int r = clamp((int) (sum[1] / weightSum), 0, 255);
        int g = clamp((int) (sum[2] / weightSum), 0, 255);
        int b = clamp((int) (sum[3] / weightSum), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void main(String[] args) throws IOException {
        // Example usage:
        BufferedImage inputImage = ImageIO.read(new File("input.png"));
        applyFXAA(inputImage, 3); // Apply FXAA with 3 passes
        ImageIO.write(inputImage, "png", new File("output.png"));
    }
}
