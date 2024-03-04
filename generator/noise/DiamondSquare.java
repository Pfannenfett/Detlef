package generator.noise;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

class Heightmap {
    public Heightmap(int size, float xScale, float yScale, float heightScale, float roughness) {
        assert IsPowerOfTwo(size);
        this.size = size;
        this.xScale = xScale;
        this.yScale = yScale;
        this.heightScale = heightScale;
        this.roughness = roughness;
        heightmap = new float[size][size];
        rand = new Random();
    }

    static private boolean IsPowerOfTwo(int x) {
        return (x != 0) && ((x & (x - 1)) == 0);
    }

    private final int size;
    private final float xScale;
    private final float yScale;
    private final float heightScale;
    private final float roughness;
    private final float[][] heightmap;

    private float minHeight = 1000f;
    private float maxHeight = -1000f;
    Random rand;

    public void initializeCorners() {
        heightmap[0][0] = rand.nextFloat();
        heightmap[0][size - 1] = rand.nextFloat();
        heightmap[size - 1][0] = rand.nextFloat();
        heightmap[size - 1][size - 1] = rand.nextFloat();
    }

    public void diamondStep(int x, int y, int step, float scale) {
        int hs = step / 2; //< half step

        int total = 0;
        float value = 0;

        boolean left = x - hs >= 0;
        boolean right = x + hs < size;
        boolean up = y - hs >= 0;
        boolean down = y + hs < size;

        if (up && left) {
            total++;
            value += heightmap[y - hs][x - hs];
        }
        if (up && right) {
            total++;
            value += heightmap[y - hs][x + hs];
        }
        if (down && left) {
            total++;
            value += heightmap[y + hs][x - hs];
        }
        if (down && right) {
            total++;
            value += heightmap[y + hs][x + hs];
        }
        if (step < size / 32) {
            heightmap[y][x] = value / total + (float)rand.nextGaussian() * scale;
        } else {
            heightmap[y][x] = value / total;
        }

    }

    public void squareStep(int x, int y, int step, float scale) {
        int hs = step / 2; //< half step

        int total = 0;
        float value = 0;
        if (x - hs >= 0) {
            value += heightmap[y][x - hs];
            total++;
        }
        if (x + hs < size) {
            value += heightmap[y][x + hs];
            total++;
        }
        if (y - hs >= 0) {
            value += heightmap[y - hs][x];
            total++;
        }
        if (y + hs < size) {
            value += heightmap[y + hs][x];
            total++;
        }
        heightmap[y][x] = value / total + (float)rand.nextGaussian() * scale;
    }

    public void generate() {
        initializeCorners();

        float scale = 1.0f;
        for (int step = size - 1; step > 1; step /= 2) {
            int halfstep = step / 2;

            for (int y = halfstep; y < size; y += step) {
                for (int x = halfstep; x < size; x += step) {
                    diamondStep(x, y, step, scale);
                }
            }

            // Two for loops since the diamond centers are offset every second row
            for (int y = 0; y < size; y += step) {
                for (int x = halfstep; x < size; x += step) {
                    squareStep(x, y, step, scale);
                }
            }
            for (int y = halfstep; y < size; y += step) {
                for (int x = 0; x < size; x += step) {
                    squareStep(x, y, step, scale);
                }
            }

            if (roughness == 1.0f) {
                scale /= 2.0f;
            } else {
                scale /= Math.pow(2.0, roughness);
            }
        }
    }


    public void updateMaxMinHeight() {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float value = heightmap[y][x];
                if (value > maxHeight) {
                    maxHeight = value;
                } else if (value < minHeight) {
                    minHeight = value;
                }
            }
        }
    }

    public void writeToImage() {
        float delta;
        if (minHeight <= 0) {
            delta = Math.abs(minHeight);
        } else {
            delta = minHeight * -1;
        }

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = (int)((heightmap[x][y] + delta) / (maxHeight + delta) * 255);
                g.setColor(new Color(color, color, color));
                g.drawRect(x, y, 1, 1);
            }
        }
        File outputFile = new File("image.jpg");
        try {
            ImageIO.write(image, "jpg", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
};

public class DiamondSquare {
    public static void main(String[] args) {
        Heightmap heightmap = new Heightmap(1025, 1.0f, 1.0f, 1.0f, 1f);
        heightmap.generate();
        heightmap.updateMaxMinHeight();
        heightmap.writeToImage();
    }
}