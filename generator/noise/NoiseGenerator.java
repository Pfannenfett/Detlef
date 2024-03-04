package generator.noise;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.SplittableRandom;

import data.map.HeightMap;
import data.map.Mask;
import data.util.MapUtils;
import generator.erosion.ErosionGenerator;
import io.ImageIO;

public class NoiseGenerator {
	private static SplittableRandom rng = new SplittableRandom(13553);
	private static Random oldRng = new Random(13513);
	
	public static HeightMap createFloatNoiseMap(int xSize, int ySize, long seed) {
		setSeed(seed);
		HeightMap map = new HeightMap(xSize, ySize);
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				map.setValueAt(i, j, sampleFloat(i, j));
			}
		}
		return map;
	}
	
	public static HeightMap createGaussianNoiseMap(int xSize, int ySize, long seed) {
		setSeed(seed);
		HeightMap map = new HeightMap(xSize, ySize);
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				map.setValueAt(i, j, sampleGaussian(i, j));
			}
		}
		return map;
	}
	
	public static HeightMap createVoronoiNoiseMap(int xSize, int ySize, float density, int holeSize, long seed) {
		setSeed(seed);
		long time = System.currentTimeMillis();
		HeightMap ret = new HeightMap(xSize, ySize);
		HashSet<int[]> set = new HashSet<>();
		for(int i = 0; i < xSize * ySize * density * 0.01F; i++) {
			int x = rng.nextInt(xSize);
			int y = rng.nextInt(ySize);
			int[] t = {x, y};
			set.add(t);
		}
		
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				Iterator it = set.iterator();
				float minDist = Float.POSITIVE_INFINITY;
				while(it.hasNext()) {
					int[] p = (int[]) it.next();
					float f = (p[0] - i) * (p[0] - i) + (p[1] - j) * (p[1] - j);
					minDist = Math.min(minDist, f);
				}
				minDist = (float) Math.sqrt(minDist);
				minDist /= holeSize;
				minDist = Math.min(minDist, 1);
				ret.setValueAt(i, j, minDist);
			}
		}
		System.out.println("Voronoi noise took " + (System.currentTimeMillis() - time) + "ms");
		return ret;
	}
	
	public static HeightMap createDuneVoronoiNoiseMap(int xSize, int ySize, long seed) {
		setSeed(seed);
		long time = System.currentTimeMillis();
		HeightMap ret = createHillsNoiseMap(xSize, ySize, seed, 1);
		MapUtils.smooth(ret, 1);
		byte linetonoiseratio = 4;
		BufferedImage img = new BufferedImage(xSize / linetonoiseratio, ySize / linetonoiseratio, BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = img.getGraphics();
		g.setColor(Color.WHITE);
		for(int t = 0; t < 4096; t++) {
			int x = rng.nextInt(xSize / linetonoiseratio - 2) + 1;
			int y = rng.nextInt(ySize / linetonoiseratio - 2) + 1;
			
			int iterations = 0;
			while(x > 0 && y > 0 && x < xSize / linetonoiseratio - 1 && y < ySize / linetonoiseratio - 1 && iterations < 2) {
				//System.out.println(xRange + " " + yRange);
				int xoff = rng.nextInt(xSize / 64) - xSize / 128;
				int yoff = rng.nextInt(ySize / 64) - ySize / 128;
				g.drawLine(x, y, x + xoff, y + yoff);
				x += xoff;
				y += (int) (yoff * 0.125F);
				iterations++;
			}
		}
		HeightMap map = new HeightMap(xSize, ySize);
		for(int i = 0; i < xSize / linetonoiseratio; i++) {
			for(int j = 0; j < xSize / linetonoiseratio; j++) {
				Color c = new Color(img.getRGB(i, j));
				float f = c.getBlue();
				f /= 255;
				map.setValueAt(i, j, f);
			}
		}
		MapUtils.normalizeTo(map, 0.5F);
		MapUtils.invert(map);
		MapUtils.smoothBiggen(map, 4);
		ret.setContent(MapUtils.cappedMultiply(ret, map).getContent());
		MapUtils.shrink(ret, 8);
		MapUtils.biggen(ret, 2);
		MapUtils.smoothBiggen(ret, 4);
		return ret;
	}
	
	public static HeightMap fastVoronoi(int xSize, int ySize, int pSize, boolean emptyAllowed, long seed) {
		setSeed(seed);
		HeightMap ret = new HeightMap(xSize, ySize);
		int xCount = xSize / pSize;
		int yCount = ySize / pSize;
		
		int[][][] seedCoords = new int[xCount][yCount][2];
		
		for(int i = 0; i < xCount; i++) {
			for(int j = 0; j < yCount; j++) {
				int x = rng.nextInt(pSize);
				int y = rng.nextInt(pSize);
				x += i * pSize;
				y += j * pSize;
				int[] c = {x, y};
				seedCoords[i][j] = c;
			}
		}
		
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				int px = i / pSize;
				int py = j / pSize;
				
				int[][] nbs = nbs(seedCoords, px, py);
				float minDist = Float.POSITIVE_INFINITY;
				for(int t = 0; t < nbs.length; t++) {
					if(nbs[t] != null) {
						int sx = nbs[t][0];
						int sy = nbs[t][1];
						
						float dist = (i - sx) * (i - sx) + (j - sy) * (j - sy);
						minDist = Math.min(minDist, dist);
					}
				}
				ret.setValueAt(i, j, (float) (Math.sqrt(minDist) / (2 * pSize)));
			}
		}
		return ret;
	}
	
	private static int[][] nbs(int[][][] seeds, int x, int y) {
		int[][] ret = new int[9][2];
		ret[0] = seeds[x][y];
		try { ret[1] = seeds[x][y + 1]; } catch(ArrayIndexOutOfBoundsException e) {}
		try { ret[2] = seeds[x + 1][y]; } catch(ArrayIndexOutOfBoundsException e) {}
		try { ret[3] = seeds[x][y - 1]; } catch(ArrayIndexOutOfBoundsException e) {}
		try { ret[4] = seeds[x - 1][y]; } catch(ArrayIndexOutOfBoundsException e) {}
		try { ret[5] = seeds[x + 1][y + 1]; } catch(ArrayIndexOutOfBoundsException e) {}
		try { ret[6] = seeds[x + 1][y - 1]; } catch(ArrayIndexOutOfBoundsException e) {}
		try { ret[7] = seeds[x - 1][y - 1]; } catch(ArrayIndexOutOfBoundsException e) {}
		try { ret[8] = seeds[x - 1][y + 1]; } catch(ArrayIndexOutOfBoundsException e) {}
		return ret;
	}
	
	public static HeightMap testMountains(int xSize, int ySize, long seed) {
		HeightMap h = NoiseGenerator.createPerlinNoiseMap(xSize * 8, ySize * 8, seed, 6, true);
		MapUtils.distToHeight(h, 0.5f);
		MapUtils.shrink(h, 8);
		ImageIO.saveAsImage(h);
		return h;
	}
	
	public static HeightMap createPerlinNoiseMap(int xSize, int ySize, long seed, int octaves, boolean gaussian) {
		long time = System.currentTimeMillis();
		
		HeightMap ret = new HeightMap(xSize, ySize);
		float factor = 2F;
		for(int i = 0; i < octaves; i++) {
			int xts = (int) (xSize / Math.pow(factor, octaves - 1) * Math.pow(factor,i));
			int yts = (int) (ySize / Math.pow(factor, octaves - 1) * Math.pow(factor,i));
			if(xts == 0 || yts == 0) continue;
			HeightMap h;
			if(gaussian) {
				h = createGaussianNoiseMap(xts, yts, seed * i);
			} else {
				h = createFloatNoiseMap(xts, yts, seed * i);
			}
			//ImageIO.saveAsGreyImage(h, "Perlin " + i);
			MapUtils.normalize(h);
			MapUtils.smoothBiggen(h, (int) (Math.pow(factor, octaves - 1) / Math.pow(factor,i)));
			MapUtils.normalizeTo(h, (float) (1 / Math.pow(factor,i)));
			//MapUtils.flipRandom(h, seed*i);
			ret = new HeightMap(MapUtils.addNormalizedMap(ret, h).getContent());
			
		}
		MapUtils.normalize(ret);
		time = System.currentTimeMillis() - time;
		//System.out.println("created Perlin Noise of Size " + xSize + "/" + ySize + " with " + octaves + "octaves in " + time + "ms");
		return ret;
	}
	
	public static HeightMap createDuneNoiseMap(int xSize, int ySize, long seed, float xDir, float yDir) {
		setSeed(seed);
		xSize /= 2;
		ySize /= 2;
		HeightMap offsets = createHillsNoiseMap(xSize, ySize, seed, 4);
		HeightMap scalings = createHillsNoiseMap(xSize, ySize, seed, 4);
		HeightMap base = createHillsNoiseMap(xSize, ySize, seed, 4);
		base.mult(0.02f);
		HeightMap heights = createHillsNoiseMap(xSize, ySize, seed, 8);
		heights.mult(0.06f);
		heights.add(0.03f);
		HeightMap ret = new HeightMap(xSize, ySize);
		
		float l = xDir * xDir + yDir * yDir;
		l = (float) Math.sqrt(l);
		xDir /= l;
		yDir /= l;
		
		float xRot = yDir;
		float yRot = -xDir;
		
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				
				
				float x = i * xDir + j * yDir;
				float y = i * xRot + j * yRot;
				
				x += Math.sin(0.08 * y + 4 * offsets.getValueAt(i, j)) * scalings.getValueAt(i, j) * 8;
				
				float h = heights.getValueAt(i, j) * getDuneSideViewHeight(x);
				ret.setValueAt(i, j, h);
			}
		}
		ret = ret.max(base);
		MapUtils.smoothBiggen(ret, 2);
		return ret;
	}
	
	public static HeightMap createDuneNoiseMap(int xSize, int ySize, long seed) {
		return createDuneNoiseMap(xSize, ySize, seed, (float) rng.nextDouble(), (float) rng.nextDouble());
	}
	
	private static float getDuneSideViewHeight(float x) {
		x = x % 40;
		
		if(x < 4f) {
			return 0f;
		} else if(x < 29f) {
			return (float) Math.sqrt(x - 4f) / 5f;
		} else if(x < 36f){
			return (x - 36f) * (x - 36f) * 0.014f;
		} else {
			return 0f;
		}
	}
	
	public static HeightMap createMountainNoiseMap(int xSize, int ySize, long seed) {
		setSeed(rng.nextLong() + seed);
		boolean b = false;
		int r = 64;
		if(xSize < r || ySize < r) b = true;
		xSize = Math.max(r, xSize);
		ySize = Math.max(r, ySize);
		HeightMap m1 = createPerlinNoiseMap(xSize / 4, ySize / 4, seed, 4, false);
		HeightMap m2 = createPerlinNoiseMap(xSize, ySize, seed, 6, false);
		MapUtils.absoluteinvert(m1);
		MapUtils.smoothBiggen(m1, 4);
		HeightMap ret = new HeightMap(MapUtils.addNormalizedMap(m1, m2).getContent());
		MapUtils.normalize(ret);
		ret.addDimin(0.3F);
		MapUtils.normalize(ret);
		ret = new HeightMap(MapUtils.invert(ret));
		if(b) ret = new HeightMap(MapUtils.submap(ret, 0, 0, xSize, ySize));
		ret.cap();
		return ret;
	}
	
	public static HeightMap createHillsNoiseMap(int xSize, int ySize, long seed, int scalar) {
		HeightMap m1 = createPerlinNoiseMap(xSize / scalar, ySize / scalar, seed, 2, true);
		HeightMap m2 = createPerlinNoiseMap(xSize / scalar, ySize / scalar, seed, 2, false);
		MapUtils.normalize(m1);
		MapUtils.normalize(m2);
		HeightMap ret = new HeightMap(MapUtils.addNormalizedMap(m1, m2).getContent());
		MapUtils.normalizeTo(ret, 0.5F);
		MapUtils.smooth(ret, scalar);
		MapUtils.smoothBiggen(ret, scalar);
		MapUtils.normalize(ret);
		ret.cap();
		return ret;
	}
	
	public static float sampleGaussian(int x, int y) {
		return (float) ((oldRng.nextGaussian() + 1) / 2);
	}
	
	public static float sampleFloat(int x, int y) {
		return (float) rng.nextDouble();
	}
	
	public static void setSeed(long seed) {
		seed += rng.nextLong();
		rng = new SplittableRandom(seed);
		oldRng = new Random(rng.nextLong());
	}

}
