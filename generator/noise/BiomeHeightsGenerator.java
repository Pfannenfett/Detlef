package generator.noise;

import data.WorldSettings;
import data.map.HeightMap;
import data.util.MapUtils;
import io.ImageIO;

public class BiomeHeightsGenerator {
	private static HeightMap plains;
	private static HeightMap hills;
	private static HeightMap mountains;
	
	private float height;
	
	public static void initHMaps(int xSize, int ySize) {
		plains = NoiseGenerator.createFloatNoiseMap(xSize / 64, ySize / 64, WorldSettings.seed);
		MapUtils.smoothBiggen(plains, 64);
		MapUtils.normalizeTo(plains, 0.1f);
		
		hills = NoiseGenerator.createHillsNoiseMap(xSize + 32, ySize + 32, WorldSettings.seed, 16);
		hills = new HeightMap(MapUtils.submap(hills, 16, 16, xSize, ySize));
		MapUtils.normalize(hills);
		hills.mult(0.2f);
		
		mountains = NoiseGenerator.createMountainNoiseMap(xSize, ySize, WorldSettings.seed);
		MapUtils.smoothBiggen(mountains, 2);
		mountains = new HeightMap(MapUtils.submap(mountains, 16, 16, xSize, ySize));
		MapUtils.normalize(mountains);
		
		ImageIO.saveAsGreyImage(plains, "plains");
		ImageIO.saveAsGreyImage(hills, "hills");
		ImageIO.saveAsGreyImage(mountains, "mountains");
	}
	
	public BiomeHeightsGenerator(float h) {
		height = h;
	}
	
	public float convertToLandRela(float lh) {
		return Math.max(0, lh - WorldSettings.layoutSeaLevel) / (1 - WorldSettings.layoutSeaLevel);
	}
	
	public float[] getValuesFor(float lh) {
		lh = convertToLandRela(lh);
		float fh = lh - 0.5f;
		fh *= 2;
		fh *= fh;
		float plains = lh < 0.5f ? fh : 0f;
		float hills = 1 - fh;
		float mounts = lh > 0.5f ? fh : 0f;
		float[] ret = {plains, hills, mounts};
		return ret;
	}
	
	public float getHeight(int x, int y, float lh, long seed) {
		float[] coef = getValuesFor(lh);
		float p = coef[0] * plains.getValueAt(x, y);
		float h = coef[1] * hills.getValueAt(x, y);
		float m = coef[2] * mountains.getValueAt(x, y);
		
		if(lh > WorldSettings.layoutSeaLevel) {
			return (p + h + m) * (1 - WorldSettings.waterHeightDown) * (Math.min((lh - WorldSettings.layoutSeaLevel) * 10, 1f)) + WorldSettings.waterHeightDown;
		} else {
			return WorldSettings.waterHeightDown - hills.getValueAt(x, y) * (WorldSettings.layoutSeaLevel - lh) * 3;
		}
	}
}
