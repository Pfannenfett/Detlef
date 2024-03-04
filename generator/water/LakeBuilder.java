package generator.water;

import java.util.Random;

import data.map.HeightMap;
import data.map.Mask;
import data.util.MapUtils;
import generator.noise.NoiseGenerator;

public class LakeBuilder {
	
	public static Mask createLakeMask(HeightMap layout) {
		Mask m1 = layout.above(0.62F);
		Mask m2 = layout.below(0.7F);
		m1.and(m2);
		HeightMap h = MapUtils.asHeightMap(m1);
		MapUtils.smooth(h, 16);
		m1 = h.above(0.95F);
		h = MapUtils.asHeightMap(m1);
		MapUtils.smooth(h, 16);
		m1 = h.above(0.95F);
		return m1;
	}
	
	public static HeightMap createLakes(Mask spots, long seed) {
		Random rng = new Random(seed);
		HeightMap h = NoiseGenerator.createPerlinNoiseMap(spots.getXSize() / 4, spots.getYSize() / 4, rng.nextLong(), 4, false);
		Mask sm = new Mask(spots.getContent());
		HeightMap hm = MapUtils.asHeightMap(sm);
		MapUtils.shrink(hm, 4);
		sm = hm.above(0.9F);
		h.applyMask(sm);
		Mask m = h.above(0.8F);
		h = MapUtils.asHeightMap(m);
		MapUtils.smooth(h, 4);
		m = h.above(0.3F);
		h = MapUtils.asHeightMap(m);
		MapUtils.smoothBiggen(h, 4);
		h.applyMask(spots);
		MapUtils.invert(h);
		return h;
	}
	
	
}
