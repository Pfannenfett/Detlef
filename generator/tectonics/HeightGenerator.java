package generator.tectonics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import data.World;
import data.WorldSettings;
import data.biomes.Biome;
import data.map.CrustMap;
import data.map.HeightMap;
import data.map.Mask;
import data.map.layout.CellularLayoutMap;
import data.util.MapUtils;
import generator.Generator;
import generator.erosion.ErosionGenerator;
import generator.height.ITerrainGenerator;
import generator.noise.NoiseGenerator;
import generator.water.LakeBuilder;
import generator.water.rivers.RiverBuilder;
import generator.water.rivers.RiverInfo;
import io.ImageIO;

public class HeightGenerator extends Generator {
	
	public static World fromBiomeMap(CellularLayoutMap clm, HeightMap layout, long seed) {
		ImageIO.saveAsGreyImage(layout, "layout");
		HeightMap ret = new HeightMap(clm.getXSize() * WorldSettings.layoutRatio, clm.getYSize() * WorldSettings.layoutRatio);
		HeightMap lay = new HeightMap(layout.clone());
		MapUtils.biggen(lay, WorldSettings.layoutRatio);
		
		
		World world = new World(clm.getXSize() * WorldSettings.layoutRatio, clm.getYSize() * WorldSettings.layoutRatio);
		
		ArrayList<ITerrainGenerator> terrainGens = new ArrayList<>();
		ArrayList<Biome> biomes = clm.getBiomesPresent();
		for(int b = 0; b < biomes.size(); b++) {
			Biome biome = biomes.get(b);
			
			String id = biome.getTerrainGenerator().identify();
			
			boolean contains = false;
			for(int i = 0; i < terrainGens.size(); i++) {
				if(terrainGens.get(i).identify().equals(id)) {
					contains = true;
				}
			}
			
			if(!contains) {
				terrainGens.add(biome.getTerrainGenerator());
			}
		}
		
		Mask borders = new Mask(layout.getXSize(), layout.getYSize());
		for(int b = 0; b < terrainGens.size(); b++) {
			ArrayList<Biome> biomesForTG = clm.getBiomesForTerrainGenerator(terrainGens.get(b));
			Mask terraGen = clm.getMaskForBiomes(biomesForTG);
			Mask bm = MapUtils.findMaskBorder(terraGen);
			borders.or(bm);
			
		}
		
		HeightMap hBorders = borders.toHeightMap(1f, 0f);
		
		MapUtils.convSmooth(hBorders, 2);
		MapUtils.smoothBiggen(hBorders, WorldSettings.layoutRatio);
		
		
		
		HeightMap finishedHeightMap = new HeightMap(1, 1);
		for(int b = 0; b < terrainGens.size(); b++) {
			if(terrainGens.get(b).isLowerTerrain()) continue;
			//finishedHeightMap = terrainGens.get(b).generateTerrain(clm, layout, world, seed);
			
			
			for(int i = 0; i < ret.getXSize(); i++) {
				for(int j = 0; j < ret.getYSize(); j++) {
					float max = ret.getValueAt(i, j);
					
					max = Math.max(finishedHeightMap.getValueAt(i, j), max);
					
					ret.setValueAt(i, j, max);
				}
			}
			
		}
		//ImageIO.saveAsGreyImage(hBorders, "Borders");
		
		
		MapUtils.convSmooth(ret, 3, 0f, 4, hBorders.above(0.01f));
		MapUtils.convSmooth(ret, 3, 0f, 4, hBorders.above(0.05f));
		MapUtils.convSmooth(ret, 3, 0f, 4, hBorders.above(0.05f));
		
		MapUtils.convSmooth(ret, 8, 0f, 1, hBorders.above(0.05f));
		MapUtils.convSmooth(ret, 8, 0f, 1, hBorders.above(0.05f));
		MapUtils.convSmooth(ret, 8, 0f, 1, hBorders.above(0.001f));
		MapUtils.convSmooth(ret, 8, 0f, 1, hBorders.above(0.001f));
		
		world.setUpperTerrain(ret);
		
		
		finishedHeightMap.delete();
		ret = new HeightMap(clm.getXSize() * WorldSettings.layoutRatio, clm.getYSize() * WorldSettings.layoutRatio);
		for(int b = 0; b < terrainGens.size(); b++) {
			if(!terrainGens.get(b).isLowerTerrain()) continue;
			finishedHeightMap = terrainGens.get(b).generateTerrain(clm, layout, world, seed);
			
			
			for(int i = 0; i < ret.getXSize(); i++) {
				for(int j = 0; j < ret.getYSize(); j++) {
					float max = ret.getValueAt(i, j);
					
					max = Math.max(finishedHeightMap.getValueAt(i, j), max);
					
					ret.setValueAt(i, j, max);
				}
			}
			
		}
		world.setLowerTerrain(ret);
		return world;
	}
	
	public static HeightMap noiseLayout(HeightMap layout, RiverInfo riverInfo, long seed) {
		float waterHeight = 0.244F;
		float waterLayoutHeight = 0.5F + 1 / 16;
		
		
		
		HeightMap mountains = NoiseGenerator.testMountains(layout.getXSize(), layout.getYSize(), seed);
		HeightMap noise = NoiseGenerator.createHillsNoiseMap(layout.getXSize(), layout.getYSize(), seed, 4);
		HeightMap hills = NoiseGenerator.createHillsNoiseMap(layout.getXSize(), layout.getYSize(), seed, 1);
		HeightMap ret = new HeightMap(layout.getXSize(), layout.getYSize());
		
		Mask lakeMask = LakeBuilder.createLakeMask(layout);
		HeightMap lakes = LakeBuilder.createLakes(lakeMask, seed);
		hills = new HeightMap(MapUtils.cappedMultiply(hills, lakes));
		noise = new HeightMap(MapUtils.cappedMultiply(noise, lakes));
		mountains = new HeightMap(MapUtils.cappedMultiply(mountains, lakes));
		
		hills.add(-0.2F);
		MapUtils.normalize(mountains);
		MapUtils.normalize(hills);
		
		MapUtils.normalizeTo(hills, 1F);
		MapUtils.normalizeTo(noise, 0.4F);
		MapUtils.normalizeTo(mountains, 1F);
		
		if(riverInfo != null) {
			HeightMap mult = RiverBuilder.createRiverCarvingMult(riverInfo.createFlowMap(), riverInfo.getMinFlow(), layout, waterHeight, 4);
			
			RiverBuilder.setRiverValleys(mult.below(0.1F));
			ImageIO.saveAsGreyImage(mult, "River degradation");
			
			hills = new HeightMap(MapUtils.cappedMultiply(hills, mult));
			noise = new HeightMap(MapUtils.cappedMultiply(noise, mult));
			//mountains = new HeightMap(MapUtils.cappedMultiply(mountains, mult));
		}
		
		
		
		ImageIO.saveAsGreyImage(lakes, "lakes");
		ImageIO.saveAsGreyImage(lakeMask, "lakeMask");
		ImageIO.saveAsImage(mountains, "mountains");
		ImageIO.saveAsImage(hills, "hills");
		ImageIO.saveAsImage(noise, "noise");
		
		mountains.addDimin(0.2F);
		
		float mountainWeight = 1F;
		float hillWeight = 1F;
		float noiseWeight = 0.5F;
		
		for(int i = 0; i < layout.getXSize(); i++) {
			for(int j = 0; j < layout.getYSize(); j++) {
				float n = noise.getValueAt(i, j);
				float m = mountains.getValueAt(i, j);
				float h = hills.getValueAt(i, j);
				float l = layout.getValueAt(i, j);
				
				h *= hillsCurve(l, waterHeight, waterLayoutHeight);
				m *= mountainsCurve(l, waterHeight, waterLayoutHeight);
				
				h *= hillWeight;
				m *= mountainWeight;
				n *= noiseWeight;
				
				float height = h + m + n;
				height *= heightCurve(l, waterHeight, waterLayoutHeight);
				height += layoutBaseLine(l, waterHeight, waterLayoutHeight);
				
			//	height = hillsCurve(l, waterHeight, waterLayoutHeight);
				//height += mountainsCurve(l, waterHeight, waterLayoutHeight);
				
				ret.setValueAt(i, j, height);
			}
		}
		
		
		ret.setContent(MapUtils.curveAbove(ret, 0.24F).getContent());
		//MapUtils.normalize(ret);
		//lakes.addDimin(0.6F);
		//ret = new HeightMap(MapUtils.cappedMultiply(ret, lakes));
		
		islandEdit(layout, ret, seed);
		
		return ret;
	}
	
	private static void islandEdit(HeightMap layout, HeightMap h, long seed) {
		Mask ma = layout.above(0.5F);
		ma.and(layout.below(0.6F));
		
		
		HeightMap he = MapUtils.asHeightMap(ma);
		MapUtils.smooth(he, 16);
		ma = he.above(0.6F);
		he = MapUtils.asHeightMap(ma);
		MapUtils.smooth(he, 16);
		ma = he.above(0.7F);
		he = MapUtils.asHeightMap(ma);
		MapUtils.smooth(he, 16);
		ma = he.above(0.3F);
		he = MapUtils.asHeightMap(ma);
		MapUtils.smooth(he, 16);
		ma = he.above(0.1F);
		
		HeightMap nm = NoiseGenerator.createPerlinNoiseMap(h.getXSize(), h.getYSize(), seed, 4, true);
		MapUtils.normalize(nm);
		ImageIO.saveAsGreyImage(nm, "islands");
		ImageIO.saveAsGreyImage(ma, "islandMask");
		
		HeightMap nma = new HeightMap(nm.getContent().clone());
		nma.applyMask(nma.above(0.5F));
		nma.add(-0.5F);
		nma.mult(0.2F);
		
		HeightMap nmb = new HeightMap(nm.getContent().clone());
		nmb.applyMask(nmb.below(0.5F));
		nmb.mult(0.2F);
		
		nma.applyMask(ma);
		nmb.applyMask(ma);
		
		h.setContent(MapUtils.addDiminMap(h, nma).getContent());
		h.setContent(MapUtils.substractDiminMap(h, nmb).getContent());
		
		
	}
	
	private static float hillsCurve(float x, float waterHeight, float waterLayoutHeight) {
		float ret = 0;
		if(x < waterLayoutHeight) {
			ret = waterLayoutHeight - x;
		} else {
			float f = 1 - waterLayoutHeight;
			f *= 0.5F;
			x -= waterLayoutHeight;
			x -= f;
			x *= x;
			x *= 16;
			ret = 1 - x;
			ret -= 0.5F;
			ret *= 2;
		}
		ret = Math.max(0, ret);
		return ret;
	}
	
	private static float mountainsCurve(float x, float waterHeight, float waterLayoutHeight) {
		float ret = 0;
		if(x < waterLayoutHeight) {
			ret = waterLayoutHeight - x;
			ret *= 2;
		} else {
			float f = 1 - waterLayoutHeight;
			f /= 2;
			x -= waterLayoutHeight;
			x -= f;
			ret = x * 4;
		}
		ret = Math.max(0, ret);
		return ret;
	}
	
	private static float heightCurve(float x, float waterHeight, float waterLayoutHeight) {
		float ret = 0;
		if(x < waterLayoutHeight) {
			ret = waterLayoutHeight - x;
			ret /= waterLayoutHeight;
			ret *= waterHeight;
			ret *= 1.1F;
		} else {
			float f = 1 - waterLayoutHeight;
			x -= waterLayoutHeight;
			ret = x / f;
		}
		ret = Math.max(0, ret);
		return ret;
	}
	
	private static float layoutBaseLine(float x, float waterHeight, float waterLayoutHeight) {
		float ret = 0;
		if(x < waterLayoutHeight) {
			ret = x;
			ret *= waterHeight / waterLayoutHeight;
			ret = waterHeight - ret;
			ret *= 2;
			ret = waterHeight - ret;
		} else {
			ret = waterHeight + (x - waterHeight) * 0.125F;
		}
		ret = Math.max(0, ret);
		return ret;
	}
	
	public static HeightMap createTectonicLayoutHeightMap(CrustMap crust, int worldAge) {
		HeightMap map = new HeightMap(crust.getXSize(), crust.getYSize());
		float[][] temp = new float[crust.getXSize()][crust.getYSize()];
		HashSet<Plate> plates = crust.getAllPlates();
		HashSet<PlateWorld> plateWorlds = new HashSet<>();
		ArrayList<PlateWorld> subductOrder = new ArrayList<>();
		
		Iterator it = plates.iterator();
		while(it.hasNext()) {
			Plate p = (Plate) it.next();
			plateWorlds.add(new PlateWorld(crust, p));
		}
		
		subductOrder.addAll(plateWorlds);
		subductOrder.sort(new SubductionComparator());
		
		long time;
		long time1;
		long time2;
		for(int t = 0; t < worldAge; t++) {
			System.out.print(t + " / " + worldAge + "  ");
			time = System.currentTimeMillis();
			it = plateWorlds.iterator();
			while(it.hasNext()) {
				PlateWorld world = (PlateWorld) it.next();
				world.prepareMoving();
			}
			time1 = System.currentTimeMillis() - time;
			System.out.print(time1 + "ms + ");
			boolean[][] b = subductionCheck(subductOrder, crust.getXSize(), crust.getYSize(), (float) (0.02));
			time2 = System.currentTimeMillis() - time - time1;
			System.out.println(time2 + "ms");
			it = plateWorlds.iterator();
			while(it.hasNext()) {
				PlateWorld world = (PlateWorld) it.next();
				world.doMove(b);
			}
		}
		
		it = plateWorlds.iterator();
		while(it.hasNext()) {
			PlateWorld p = (PlateWorld) it.next();
			for(int i = 0; i < temp.length; i++) {
				for(int j = 0; j < temp[0].length; j++) {
					temp[i][j] = MapUtils.addDimin(temp[i][j], p.getHeightAt(i, j));
				}
			}
		}
		for(int i = 0; i < temp.length; i++) {
			for(int j = 0; j < temp[0].length; j++) {
				temp[i][j] = Math.max(temp[i][j], 0.15F);
			}
		}
		HeightMap hm = new HeightMap(temp);
		MapUtils.smooth(hm, 4);
		MapUtils.shrink(hm, 16);
		MapUtils.biggen(hm, 16);
		MapUtils.smooth(hm, 4);
		HeightMap noise = NoiseGenerator.createGaussianNoiseMap(hm.getXSize() / 32, hm.getYSize() / 32, 420 * temp.length);
		noise.mult(2);
		noise.add(-1F);
		noise.cap();
		MapUtils.smoothBiggen(noise, 32);
		noise = new HeightMap(MapUtils.cappedMultiply(noise, hm));
		noise.mult(0.6F);
		hm = new HeightMap(MapUtils.addDiminMap(hm, noise));
		return hm;
	}
	
	private static boolean[][] subductionCheck(ArrayList<PlateWorld> worlds, int xSize, int ySize, float subductSpeed) {
		PlateWorld p = null;
		boolean[][] ret = new boolean[xSize][ySize];
		int b = 0;
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				p = null;
				b = 0;
				for(int t = 0; t < worlds.size(); t++) {
					if(worlds.get(t).getHeightAt(i, j) > 0) {
						if(p == null) {
							p = worlds.get(t);
						} else {
							if(b == 0) {
								p.subduct(i, j, subductSpeed);
								p = null;
							}
							b++;
							if(b == 2) {
								break;
							}
						}
					}
				}
				if(b == 1) {
					ret[i][j] = true;
				}
			}
		}
		return ret;
	}
	
	private static class SubductionComparator implements Comparator<PlateWorld> {
		@Override
		public int compare(PlateWorld o1, PlateWorld o2) {
			return (int) ((o1.getSubductId() - o2.getSubductId()) * 100);
		}
		
	}
}
