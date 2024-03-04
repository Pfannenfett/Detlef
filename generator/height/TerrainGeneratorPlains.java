package generator.height;

import java.util.ArrayList;
import java.util.Random;

import data.World;
import data.WorldSettings;
import data.biomes.Biome;
import data.map.GroundCoverMap;
import data.map.HeightMap;
import data.map.Mask;
import data.map.layout.CellularLayoutMap;
import data.util.MapUtils;
import generator.climate.ClimateGenerator;
import generator.erosion.ErosionGenerator;
import generator.noise.NoiseGenerator;
import io.ImageIO;

public class TerrainGeneratorPlains implements ITerrainGenerator {
	
	private static final String identifier = "Plains";
	
	private boolean isDuneVariant = false;
	private static TerrainGeneratorPlains duneVariant = new TerrainGeneratorPlains(false);
	
	private TerrainGeneratorPlains(boolean dv) {
		this.isDuneVariant = dv;
	}
	
	public TerrainGeneratorPlains() {
		this.isDuneVariant = false;
	}
	
	public TerrainGeneratorPlains getDuneVariant() {
		return duneVariant;
	}
	
	@Override
	public boolean isLowerTerrain() {
		return true;
	}
	
	@Override
	public HeightMap generateTerrain(CellularLayoutMap clm, HeightMap layout, World world, long seed) {
		
		ArrayList<Biome> biomes = clm.getBiomesForTerrainGeneratorIdentifier(identifier);
		Mask terraGenMask = clm.getMaskForBiomes(biomes);
		
		
		HeightMap rflow = new HeightMap(clm.getRiverInfo().createFlowMap());
		MapUtils.repeatedConvSmooth(rflow, 2, 2);
		Mask rivers = rflow.above(0.1f);
		HeightMap riverInfluence = rivers.toHeightMap();
		MapUtils.repeatedConvSmooth(riverInfluence, 4, 3);
		riverInfluence.applyMask(terraGenMask);
		
		ImageIO.saveAsGreyImage(riverInfluence, "River Areas");
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		ArrayList<Biome> duneBiomes = clm.getBiomesForTerrainGenerator(duneVariant);
		Mask duneMask = clm.getMaskForBiomes(duneBiomes);
		
		Mask rv = riverInfluence.above(0.1f);
		rv.invert();
		duneMask.and(rv);
		
		ImageIO.saveAsGreyImage(duneMask, "Dune Mask");
		
		HeightMap ret = new HeightMap(clm.getXSize() * WorldSettings.layoutRatio, clm.getYSize() * WorldSettings.layoutRatio);
		
		int tileSize = 512;
		int xTiles = ret.getXSize() / tileSize;
		int yTiles = ret.getYSize() / tileSize;
		int lTilesSize = tileSize / WorldSettings.layoutRatio;
		
		HeightMap windDirX = NoiseGenerator.createHillsNoiseMap(layout.getXSize(), layout.getYSize(), seed + 2, 8);
		HeightMap windDirY = NoiseGenerator.createHillsNoiseMap(layout.getXSize(), layout.getYSize(), seed + 5, 8);
		
		for(int xt = 0; xt < xTiles; xt++) {
			for(int yt = 0; yt < yTiles; yt++) {
				HeightMap dm = duneMask.toHeightMap();
				dm = new HeightMap(MapUtils.submap(dm, xt * lTilesSize, yt * lTilesSize, 2 * lTilesSize, 2 * lTilesSize));
				if(dm.getMax() < 0.00001f) continue;
				MapUtils.convSmooth(dm, 3);
				Mask ds = MapUtils.submap(duneMask, xt * lTilesSize, yt * lTilesSize, 2 * lTilesSize, 2 * lTilesSize);
				dm.applyMask(ds);
				MapUtils.smoothBiggen(dm, WorldSettings.layoutRatio);
				float windX = ClimateGenerator.winds[xt * lTilesSize][yt * lTilesSize][0];
				float windY = ClimateGenerator.winds[xt * lTilesSize][yt * lTilesSize][1];
				windX += windDirX.getValueAt(xt * tileSize, yt * tileSize);
				windY += windDirY.getValueAt(xt * tileSize, yt * tileSize);
				
				HeightMap duneNoise = NoiseGenerator.createDuneNoiseMap(2 * tileSize, 2 * tileSize, seed + yt * xt + 2*xt, windX, windY);
				HeightMap hillNoise = NoiseGenerator.createHillsNoiseMap(2 * tileSize, 2 * tileSize, seed + xt * yt, 8);
				hillNoise.add(-0.6f);
				hillNoise.cap();
				hillNoise.mult(2.5f);
				hillNoise.cap();
				HeightMap eh = NoiseGenerator.createHillsNoiseMap(2 * tileSize, 2 * tileSize, seed + xt * yt, 4);
				eh.mult(0.4f);
				hillNoise = hillNoise.max(eh);
				
				for(int x = 0; x < 2 * tileSize; x++) {
					for(int y = 0; y < 2 * tileSize; y++) {
						float bx = 1f; 
						float by = 1f;
						
						if(x > tileSize) {
							bx = (4f * tileSize - 3 * x);
							bx /= tileSize;
							bx = Math.max(bx, 0f);
						}
						
						if(y > tileSize) {
							by = (4f * tileSize - 3 * y);
							by /= tileSize;
							by = Math.max(by, 0f);
						}
						
						float h = dm.getValueAt(x, y) * duneNoise.getValueAt(x, y) * hillNoise.getValueAt(x, y) * bx * by + WorldSettings.waterHeightDown;
						h = Math.max(ret.getValueAt(xt * tileSize + x, yt * tileSize + y), h);
						ret.setValueAt(xt * tileSize + x, yt * tileSize + y, h);
					}
				}
			}
		}
		HeightMap dm = duneMask.toHeightMap();
		MapUtils.convSmooth(dm, 3);
		MapUtils.smoothBiggen(dm, WorldSettings.layoutRatio);
		world.getGroundCover().setValues(dm.above(0.1f), GroundCoverMap.SAND);
		dm.delete();
		
		
		Mask coast = layout.below(WorldSettings.layoutSeaLevel);
		Mask coastLine = MapUtils.findMaskBorder(coast);
		coast = coastLine.clone();
		HeightMap chm = coast.toHeightMap(1f, 0f);
		MapUtils.repeatedConvSmooth(chm, 4, 4);
		coast = chm.above(0.01f);
		Mask coastAreas = coast.clone();
		coastAreas.and(terraGenMask);
		
		ImageIO.saveAsGreyImage(coastAreas, "CoastAreas");
		
		HeightMap expectedCoastlevels = clm.getTerracedLayout();
		expectedCoastlevels = new HeightMap(MapUtils.repAssimilateUp(expectedCoastlevels, 16));
		
		expectedCoastlevels.applyMask(coastLine);
		HeightMap coastLevels = new HeightMap(MapUtils.crystalize(expectedCoastlevels, coastLine));
		
		ImageIO.saveAsGreyImage(expectedCoastlevels, "ExpectedCoastLevels");
		
		coastLevels.applyMask(coastAreas);
		ImageIO.saveAsGreyImage(coastLevels, "coastLevels");
		
		Mask oceanMask = layout.below(WorldSettings.layoutSeaLevel);
		
		
		
		for(int xt = 0; xt < xTiles; xt++) {
			for(int yt = 0; yt < yTiles; yt++) {
				HeightMap cm = coastAreas.toHeightMap();
				cm = new HeightMap(MapUtils.submap(cm, xt * lTilesSize, yt * lTilesSize, 2 * lTilesSize, 2 * lTilesSize));
				if(cm.getMax() < 0.00001f) continue;
				MapUtils.convSmooth(cm, 3);
				Mask cs = MapUtils.submap(coastAreas, xt * lTilesSize, yt * lTilesSize, 2 * lTilesSize, 2 * lTilesSize);
				cm.applyMask(cs);
				MapUtils.smoothBiggen(cm, WorldSettings.layoutRatio);
				
				HeightMap tgsm = terraGenMask.toHeightMap();
				tgsm = new HeightMap(MapUtils.submap(tgsm, xt * lTilesSize, yt * lTilesSize, 2 * lTilesSize, 2 * lTilesSize));
				MapUtils.smoothBiggen(tgsm, WorldSettings.layoutRatio);
				
				HeightMap omsm = oceanMask.toHeightMap();
				omsm = new HeightMap(MapUtils.submap(omsm, xt * lTilesSize, yt * lTilesSize, 2 * lTilesSize, 2 * lTilesSize));
				MapUtils.smoothBiggen(omsm, WorldSettings.layoutRatio);
				
				HeightMap gd = MapUtils.geoDistAboveHeight(omsm, 0, 16);
				gd.mult(0.02f);
				
				HeightMap clsm = new HeightMap(MapUtils.submap(coastLevels, xt * lTilesSize, yt * lTilesSize, 2 * lTilesSize, 2 * lTilesSize));
				MapUtils.smoothBiggen(clsm, WorldSettings.layoutRatio);
				
				
				for(int x = 0; x < 2 * tileSize; x++) {
					for(int y = 0; y < 2 * tileSize; y++) {
						float bx = 1f; 
						float by = 1f;
						
						if(x > tileSize) {
							bx = (4f * tileSize - 3 * x);
							bx /= tileSize;
							bx = Math.max(bx, 0f);	
						}
						if(y > tileSize) {
							by = (4f * tileSize - 3 * y);
							by /= tileSize;
							by = Math.max(by, 0f);
						}
						
						float h = Math.max(ret.getValueAt(xt * tileSize + x, yt * tileSize + y), WorldSettings.waterHeightDown - gd.getValueAt(x, y) + 0.01f);
						ret.setValueAt(xt * tileSize + x, yt * tileSize + y, h);
					}
				}
			}
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		ImageIO.saveAsGreyImage(ret, "Plains");
		return ret;
	}
	
	public HeightMap createCoast(HeightMap coastHeight, HeightMap hinterland, Mask waterMask, long seed) {
		int cellsSize = 1024;
		int blendingSize = 128;
		
		int stepSize = cellsSize - blendingSize;
		
		HeightMap blendingMultiplier = MapUtils.createBlendingSquare(cellsSize, blendingSize);
		HeightMap ret = new HeightMap(hinterland.getXSize(), hinterland.getYSize());
		
		for(int i = 0; i < hinterland.getXSize(); i += stepSize) {
			for(int j = 0; j < hinterland.getYSize(); j += stepSize) {
				Mask sm = MapUtils.submap(waterMask, i, j, cellsSize, cellsSize);
				if(!sm.isMixed()) continue;
				
				HeightMap lsm = MapUtils.submap(coastHeight, i, j, cellsSize, cellsSize).toHeightMap();
				HeightMap hlsm = MapUtils.submap(hinterland, i, j, cellsSize, cellsSize).toHeightMap();
				
				HeightMap coast = createCoastSubMap(sm, lsm, hlsm, seed);
				coast = MapUtils.cappedMultiply(coast, blendingMultiplier).toHeightMap();
				
				coast.coordinateShift(i, j);
				ret = MapUtils.addCappedMap(ret, coast).toHeightMap();
			}
		}
		
		return ret;
	}
	
	public HeightMap createCoastSubMap(Mask coastMask, HeightMap coastHeight, HeightMap standardTerrain, long seed) {
		HeightMap ret = new HeightMap(coastMask.getXSize(), coastMask.getYSize());
		
		HeightMap cd = MapUtils.geoDistAboveHeight(coastMask.toHeightMap(), 0, 128);
		ret = createBeachSubMap(coastMask, cd, standardTerrain, seed);
		
		return ret;
	}
	
	public HeightMap createBeachSubMap(Mask coastMask, HeightMap coastDistance, HeightMap standardTerrain, long seed) {
		coastMask.invert();
		HeightMap cd = MapUtils.geoDistAboveHeight(coastMask.toHeightMap(), 0, 256);
		coastMask.invert();
		HeightMap ret = new HeightMap(cd.clone());
		ret.mult(0.2f / WorldSettings.waterHeightDown);
		ret.pow(1.5f);
		ret.invert();
		ret.mult(WorldSettings.waterHeightDown);
		
		HeightMap noise = NoiseGenerator.createHillsNoiseMap(ret.getXSize(), ret.getYSize(), seed, 4);
		
		for(int i = 0; i < ret.getXSize(); i++) {
			for(int j = 0; j < ret.getYSize(); j++) {
				if(!coastMask.getValueAt(i, j)) continue;
				float mult = coastDistance.getValueAt(i, j);
				mult -= 0.5f;
				mult *= mult;
				mult = 0.25f - mult;
				mult *= mult;
				mult *= 0.5f;
				
				float h = noise.getValueAt(i, j);
				h *= mult;
				h += mult * 0.5f;
				
				ret.setValueAt(i, j, ret.getValueAt(i, j) + h);
			}
		}
		noise.mult(0.005f);
		ret = new HeightMap(MapUtils.addDiminMap(ret, noise));
		return ret;
	}
	
	public HeightMap createCliffSubMap(Mask coastMask, HeightMap coastDistance, HeightMap standardTerrain, float extensionCoefficient, long seed) {
		coastMask.invert();
		HeightMap cd = MapUtils.geoDistAboveHeight(coastMask.toHeightMap(), 0, 256);
		
		coastMask.invert();
		HeightMap ret = new HeightMap(cd.clone());
		ret.invert();
		ret.mult(0.5f);
		MapUtils.addDiminMapOn(ret, coastDistance);
		
		HeightMap noise = NoiseGenerator.createHillsNoiseMap(ret.getXSize(), ret.getYSize(), seed, 4);
		ret.mult(1.5f * extensionCoefficient);
		ret.add(0.5f * (1f - extensionCoefficient));
		Mask m = noise.below(ret);
		MapUtils.fillHoles(m, 2048);
		
		HeightMap top = MapUtils.convertMapType(standardTerrain, true);
		top.applyMask(m);
		
		HeightMap cliffs1 = MapUtils.getSteepness(m.toHeightMap());
		HeightMap cliffs2 = MapUtils.getSteepness(m.toHeightMap());
		MapUtils.convSmooth(cliffs1, 5);
		cliffs1 = cliffs1.max(cliffs2);
		MapUtils.convSmooth(cliffs1, 3);
		cliffs1 = cliffs1.max(cliffs2);
		MapUtils.convSmooth(cliffs1, 2);
		cliffs1 = cliffs1.min(standardTerrain);
		
		HeightMap cliffNoise = NoiseGenerator.createMountainNoiseMap(ret.getXSize(), ret.getYSize(), seed);
		cliffNoise.mult(2f);
		cliffNoise.add(-0.5f);
		cliffNoise.cap();
		
		HeightMap cliffs = new HeightMap(MapUtils.cappedMultiply(cliffs1, cliffNoise));
		
		cliffs = cliffs.max(top);
		MapUtils.convSmooth(cliffs1, 8);
		
		Mask beachMask = cliffs.above(0.1f);
		HeightMap beaches = beachMask.toHeightMap();
		MapUtils.shrink(beaches, 8);
		MapUtils.convSmooth(beaches, 4);
		MapUtils.smoothBiggen(beaches, 8);
		
		Mask beachCapMask = cliffs.above(0.1f);
		beaches = beachMask.toHeightMap();
		MapUtils.shrink(beaches, 16);
		MapUtils.convSmooth(beaches, 2);
		MapUtils.smoothBiggen(beaches, 16);
		beachCapMask = beaches.above(0.18f);
		
		noise = NoiseGenerator.createHillsNoiseMap(ret.getXSize(), ret.getYSize(), seed + 4, 8);
		beachMask = noise.above(0.75f);
		beachMask.invert();
		HeightMap noise2 = new HeightMap(noise.clone());
		noise.applyMask(beachMask, 0.99f);
		MapUtils.repeatedConvSmooth(noise, 4, 4);
		noise.mult(WorldSettings.waterHeightDown + 0.03f);
		beaches = new HeightMap(MapUtils.cappedMultiply(noise, beaches));
		
		noise = NoiseGenerator.createHillsNoiseMap(ret.getXSize(), ret.getYSize(), seed + 124, 8);
		noise = new HeightMap(MapUtils.addDiminMap(cd, noise));
		beachMask = noise.below(0.75f);
		beachMask.and(beachCapMask);
		beaches.applyMask(beachMask);
		
		MapUtils.convSmooth(beaches, 4);
		
		ErosionGenerator.boringErosion(cliffs, 2, 0.2f, 2, 16, 0.001f, 0.002f, 0.02f, 0.1f, cliffs1.above(0.001f));
		Mask erosionCleanUp = cliffs.above(0.03f);
		cliffs.applyMask(erosionCleanUp);
		
		beaches.mult(0.05f);
		
		cliffs = cliffs.max(beaches);
		Mask underwaterMask = cliffs.below(0.001f);
		
		cliffs = MapUtils.convertMapType(cliffs, false);
		
		beachMask.invert();
		cliffs2 = beachMask.toHeightMap();
		HeightMap uwd = MapUtils.geoDistAboveHeight(cliffs2, 0.01f, 128);
		cliffs2 = new HeightMap(uwd.clone());
		cliffs2.invert();
		cliffs2.mult(WorldSettings.waterHeightDown * 0.05f);
		cliffs2.add(WorldSettings.waterHeightDown * 0.45f);
		noise = NoiseGenerator.createHillsNoiseMap(ret.getXSize(), ret.getYSize(), seed + 147, 8);
		noise.applyMask(beachMask);
		uwd.invert();
		noise = new HeightMap(MapUtils.addDiminMap(uwd, noise));
		noise = new HeightMap(MapUtils.cappedMultiply(noise, cliffs2));
		cliffs2 = new HeightMap(MapUtils.addCappedMap(cliffs2, noise));
		
		MapUtils.convSmooth(cliffs2, 2);
		
		
		
		
		cliffs = cliffs.max(cliffs2);
		
		MapUtils.convSmooth(cliffs, 1);
		
		return cliffs;
	}
	
	@Override
	public String identify() {
		return identifier;
	}
}
