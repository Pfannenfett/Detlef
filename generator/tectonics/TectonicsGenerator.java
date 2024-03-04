package generator.tectonics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import data.map.CrustMap;
import generator.Generator;

public class TectonicsGenerator extends Generator {
	
	public static CrustMap createTectonicMap(int xSize, int ySize, int plateCount, float oceanPerc, long seed) {
		CrustMap map = new CrustMap(xSize, ySize);
		Random r = new Random(seed);
		ArrayList<LayoutSeed> seeds = new ArrayList<>();
		for(int i = 0; i < plateCount; i++) {
			Plate p = new Plate(r.nextLong(), oceanPerc);
			//p.setColor(new Color((int) (r.nextDouble() * 255), (int) (r.nextDouble() * 255), (int) (r.nextDouble() * 255)));
			seeds.add(new LayoutSeed(r.nextInt(xSize), r.nextInt(ySize), p));
		}
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				LayoutSeed ls = getClosestPlateTo(i, j, seeds);
				map.setPlateAt(i, j, ls.getPlate());
			}
		}
		seeds = new ArrayList<>();
		for(int i = 0; i < plateCount * 4; i++) {
			int x = r.nextInt(xSize);
			int y = r.nextInt(ySize);
			seeds.add(new LayoutSeed(x, y, map.getPlateAt(x, y)));
		}
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				LayoutSeed ls = getClosestPlateTo(i, j, seeds);
				if(getDistanceToLS(i, j, ls) < 400) {
					map.setPlateAt(i, j, ls.getPlate());
				}
			}
		}
		seeds = new ArrayList<>();
		for(int i = 0; i < plateCount * 8; i++) {
			int x = r.nextInt(xSize);
			int y = r.nextInt(ySize);
			seeds.add(new LayoutSeed(x, y, map.getPlateAt(x, y)));
		}
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				LayoutSeed ls = getClosestPlateTo(i, j, seeds);
				if(getDistanceToLS(i, j, ls) < 200) {
					map.setPlateAt(i, j, ls.getPlate());
				}
			}
		}
		seeds = new ArrayList<>();
		for(int i = 0; i < plateCount * 16; i++) {
			int x = r.nextInt(xSize);
			int y = r.nextInt(ySize);
			seeds.add(new LayoutSeed(x, y, map.getPlateAt(x, y)));
		}
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				LayoutSeed ls = getClosestPlateTo(i, j, seeds);
				if(getDistanceToLS(i, j, ls) < 100) {
					map.setPlateAt(i, j, ls.getPlate());
				}
			}
		}
		return map;
	}
	
	public static CrustMap toIslandContinent(CrustMap c) {
		for(int i = 0; i < c.getXSize(); i++) {
			for(int j = 0; j < c.getYSize(); j++) {
				c.getPlateAt(i, j).setOceanPlate(false);
			}
		}
		for(int i = 0; i < c.getXSize(); i++) {
			for(int j = 0; j < c.getYSize(); j++) {
				if(i == 0 || i == c.getXSize() - 1 || j == 0 || j == c.getYSize() - 1) {
					c.getPlateAt(i, j).setOceanPlate(true);
				}
			}
		}
		return c;
	}
	
	public static CrustMap toIsland(CrustMap c) {
		for(int i = 0; i < c.getXSize(); i++) {
			for(int j = 0; j < c.getYSize(); j++) {
				if(i == 0 || i == c.getXSize() - 1 || j == 0 || j == c.getYSize() - 1) {
					c.getPlateAt(i, j).setOceanPlate(true);
				}
			}
		}
		return c;
	}
	
	private static LayoutSeed getClosestPlateTo(int x, int y, ArrayList<LayoutSeed> seeds) {
		float minDist = 2000000F;
		LayoutSeed closest = null;
		Iterator it = seeds.iterator();
		while(it.hasNext()) {
			LayoutSeed seed = (LayoutSeed) it.next();
			float dist = getDistanceToLS(x, y, seed);
			if(dist < minDist) {
				minDist = dist;
				closest = seed;
			}
		}
		return closest;
	}
	
	private static float getDistanceToLS(int x, int y, LayoutSeed seed) {
		int xl = seed.getX() - x;
		int yl = seed.getY() - y;
		
		return (float) Math.sqrt(xl * xl + yl * yl);
	}
	
	private static class LayoutSeed {
		int x;
		int y;
		Plate plate;
		
		public LayoutSeed(int x, int y, Plate p) {
			this.x = x;
			this.y = y;
			plate = p;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}
		
		public Plate getPlate() {
			return plate;
		}
	}
}
