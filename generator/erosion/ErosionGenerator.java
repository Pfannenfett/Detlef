package generator.erosion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import data.map.HeightMap;
import data.map.Mask;
import data.util.MapUtils;
import generator.Generator;
import io.ImageIO;

public class ErosionGenerator extends Generator {
	
	private static int activeThreads = 0;
	private static int index = 0;
	
	public static void carveMountains(HeightMap mountains, HeightMap layout) {
		Mask mask = layout.above(0.5F);
		mountains.applyMask(mask);
		erode(mountains, 32);
		mountains.addDimin(0.1F);
	}
	
	public static void erode(HeightMap w, int iterations) {
		float[][] hms = w.getContent();
		float del[][] = new float[w.getXSize()][w.getYSize()];
		
		for(int o = 0; o < w.getXSize(); o += 1) {
			for(int p = 0; p < w.getYSize(); p += 1) {
				del[o][p] = 0.5F;
			}
		}
		
		int m = 2048;
		for(int o = 0; o < w.getXSize(); o += m) {
			for(int p = 0; p < w.getYSize(); p += m) {
				HashSet<Droplet> drops = new HashSet<>(m * m * 2);
				System.out.println("Erosion - Setup (" + o + " | " + p + ") - with Size: " + w.getXSize() + " and m: " + m);
				for(int i = 0; i < m * m; i++) {
					drops.add(new Droplet((int) (Math.random() * m + o), (int) (Math.random() * m + p), w, 0.1F, del, false));
				}
				
				System.out.println("Erosion - Simulation (" + o + " | " + p + ")");
				for(int i = 0; i < iterations; i++) {
					System.out.print("|");
				}
				System.out.println();
				for(int i = 0; i < iterations; i++) {
					Iterator it = drops.iterator();
					while(it.hasNext()) {
						Droplet d = (Droplet) it.next();
						if(!d.isAlive()) {
							it.remove();
						} else {
							d.doLivingTick();
						}
						
					}
					System.out.print("|");
				}
				System.out.println();
			}
		}
		ImageIO.saveAsImage(new HeightMap(del), "Erosion Delta");
	}
	
	public static void fractalErosion(HeightMap w, int detail, float intensity, float maxChange, float minHeight) {
		int radius = (int) Math.pow(2, detail);
		for(int i = 0; i < detail; i++) {
			boringErosion(w, 1, intensity / (2 * radius * radius), radius, 8 * radius * radius, 0.08f, 0.1f, maxChange, minHeight);
			radius /= 2;
		}
	}
	
	public static void realisticMountainErosion(HeightMap w) {
		boringErosion(w, 1, 0.003f, 16, 128, 0.08f, 0.1f, 0.25f, 07f);
		boringErosion(w, 1, 0.01f, 8, 128, 0.1f, 0.1f, 0.1f, 0f);
		boringErosion(w, 4, 0.1f, 3, 128, 0.4f, 0.4f, 0.05f, 0f);
		boringErosion(w, 4, 0.4f, 1, 128, 0.4f, 0.4f, 0.01f, 0.7f);
	}
	
	public static void advancedErosion(HeightMap w, int iterations, float density, int radius, int lifespan, float maxChange, boolean mountsOnly) {
		Random rng = new Random();
		System.out.println();
		int boxSize = 512;
		for(int t = 0; t < (w.getXSize() * w.getYSize()); t+=boxSize*boxSize) {
			System.out.print("|");
		}
		System.out.println();
		for(int z = 0; z < iterations; z++) {
			for(int i = 0; i < w.getXSize(); i += boxSize) {
				for(int j = 0; j < w.getYSize(); j += boxSize) {
					if(w.getValueAt(i + boxSize/2, j + boxSize/2) < 0.5f) continue;
					ErosionThread t = new ErosionThread(w, boxSize, i, j, rng.nextLong(), radius, lifespan, density, maxChange, mountsOnly);
					t.start();
					try {
						t.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		System.out.println();
		ImageIO.saveAsGreyImage(w.getChangeLog(), "deltaErosion " + index);
		index++;
	}
	
	public static void boringErosion(HeightMap w, int iterations, float density, int radius, int lifespan, float inP, float outP, float maxChange, float minHeight) {
		boringErosion(w, iterations, density, radius, lifespan, inP, outP, maxChange, minHeight, 1f, new Mask(w.getXSize(), w.getYSize(), true));
	}
	
	public static void boringErosion(HeightMap w, int iterations, float density, int radius, int lifespan, float inP, float outP, float maxChange, float minHeight, Mask m) {
		boringErosion(w, iterations, density, radius, lifespan, inP, outP, maxChange, minHeight, 1f, m);
	}
	
	public static void boringErosion(HeightMap w, int iterations, float density, int radius, int lifespan, float inP, float outP, float maxChange, float minHeight, float maskExponent) {
		boringErosion(w, iterations, density, radius, lifespan, inP, outP, maxChange, minHeight, maskExponent, new Mask(w.getXSize(), w.getYSize(), true));
	}
	
	public static void boringErosion(HeightMap w, int iterations, float density, int radius, int lifespan, float inP, float outP, float maxChange, float minHeight, float maskExponent, Mask m) {
		Random rng = new Random();
		System.out.println();
		
		
		int dropCount = w.getXSize() * w.getYSize();
		dropCount = (int) (dropCount * density);
		System.out.println("Simulating Erosion with " + dropCount + " Drops");
		int l = (int) (dropCount / 100);
		for(int t = 0; t < iterations; t++) {
			for(int i = 0; i <= 100; i++) {
				System.out.print("|");
			}
			System.out.print("  ");
		}
		System.out.println();
		int percent = (int) (dropCount / 100) + 1;
		for(int z = 0; z < iterations; z++) {
			
			BetterDroplet d = new BetterDroplet(w, radius);
			d.setMaskExponent(maskExponent);
			float[][] changes;
			int x, y;
			int[] c;
			for(int i = 0; i < dropCount; i++) {
				x = rng.nextInt(w.getXSize());
				y = rng.nextInt(w.getYSize());
				if(w.getValueAt(x, y) < minHeight || !m.getValueAt(x, y)) continue;
				changes = new float[(lifespan + 1) * (2 * radius + 1) * (2 * radius + 1)][];
				d.reset(lifespan, changes, radius, maxChange);
				d.relocate(x, y);
				for(int t = 0; t < lifespan; t++) {
					
					if(!d.doLivingTick()) break;
					
				}
				d.drop();
				//checkChanges(changes);
				w.perfChanges(changes);
				if(i % percent == 0) System.out.print("|");
			}
			System.out.print("  ");
		}
		System.out.println();
		//ImageIO.saveAsGreyImage(w.getChangeLog(), "deltaErosion " + index);
		index++;
		
		
	}
	
	
	public static void threadFinished() {
		activeThreads--;
		if(activeThreads == 0) {
			ErosionGenerator.class.notifyAll();
		}
	}
}
