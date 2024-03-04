package generator.erosion;

import java.util.Random;

import data.map.HeightMap;
import data.util.MapUtils;
import data.util.MathUtils;

public class ThermalWeatheringGenerator {
	
	public static void weather(HeightMap hm, float erosiveMultiplier, float intensity, int simTime, float stableDiff, long seed) {
		Random rng = new Random(seed +3155153);
		float itera = hm.getXSize() * hm.getYSize() * intensity;
		int iterations = (int) itera;
		
		int iterPC = iterations / 100;
		
		
		if(intensity <= 1f) {
			for(int i = 0; i < iterations; i++) {
				int x = rng.nextInt(hm.getXSize() - 4) + 2;
				int y = rng.nextInt(hm.getYSize() - 4) + 2;
				
				if(i % iterPC == 0) System.out.print(1);
				weatherAt(x, y, hm, erosiveMultiplier, intensity, simTime, stableDiff, seed);
			}
		} else {
			
			iterations -= hm.getXSize() * hm.getYSize();
			for(int i = 0; i < iterations; i++) {
				int x = rng.nextInt(hm.getXSize() - 4) + 2;
				int y = rng.nextInt(hm.getYSize() - 4) + 2;
				
				if(i % iterPC == 0) System.out.print(1);
				weatherAt(x, y, hm, erosiveMultiplier, intensity, simTime, stableDiff, seed);
			}
			
			for(int i = 0; i < hm.getXSize(); i++) {
				for(int j = 0; j < hm.getYSize(); j++) {
					int x = (13 * i) % hm.getXSize();
					int y = (3 * j) % hm.getYSize();

					weatherAt(x, y, hm, erosiveMultiplier, intensity, simTime, stableDiff, seed);
				}
			}
		}
			
		for(int x = 0; x < hm.getXSize(); x++) {
			for(int y = 0; y < hm.getYSize(); y++) {
				float h = hm.getValueAt(x, y);
				float[] nbs = hm.get4Nb(x, y);
				
				float maxNB = Math.max(Math.max(nbs[0], nbs[1]), Math.max(nbs[2], nbs[3])) + 0.003f;
				if(h > maxNB) hm.setValueAt(x, y, maxNB);
			}
		}
		
		System.out.println();
	}
	
	private static void weatherAt(int x, int y, HeightMap hm, float erosiveMultiplier, float intensity, int simTime, float stableDiff, long seed) {
		float h = hm.getValueAt(x, y);
		float[] nbs = hm.get4Nb(x, y);
		
		float xs = nbs[0] - nbs[2];
		float ys = nbs[1] - nbs[3];
		
		
		
		float talus = Math.max(Math.abs(xs), Math.abs(ys));
		if(talus < stableDiff) return;
		talus -= stableDiff;
		
		byte dir = 0;
		float min = 1f;
		for(byte b = 0; b < 4; b++) {
			if(nbs[b] < min) {
				dir = b;
				min = nbs[b];
			}
		}
		if(min > h - talus) talus = h - min;
		
		float maxNB = Math.max(Math.max(nbs[0], nbs[1]), Math.max(nbs[2], nbs[3]));
		talus *= erosiveMultiplier;
		talus = Math.max(talus, h - maxNB); // Prevents 1 Tile spikes
		
		
		
		
		hm.setValueAt(x, y, h - talus);
		for(int t = 0; t < simTime; t++) {
			dir = 0;
			min = 1f;
			for(byte b = 0; b < 4; b++) {
				if(nbs[b] < min) {
					dir = b;
					min = nbs[b];
				}
			}
			byte[] ddir = MapUtils.decomposeDirection(dir);
			x += ddir[0];
			y += ddir[1];
			
			h = hm.getValueAt(x, y);
			nbs = hm.get4Nb(x, y);
			
			xs = nbs[0] - nbs[2];
			ys = nbs[1] - nbs[3];
			
			float delta = Math.max(Math.abs(xs) - stableDiff, Math.abs(ys) - stableDiff);
			if(delta > 0) {
				delta = Math.min(delta, talus);
				talus -= delta;
				hm.setValueAt(x, y, h + delta);
			}
			if(talus < 0.001f) break;
			if(!hm.isInBounds(x, y) || h < 0.01f) break;
		}
	
	}
}
