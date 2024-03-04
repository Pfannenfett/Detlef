package generator.height;

import java.util.ArrayList;

import data.World;
import data.WorldSettings;
import data.biomes.Biome;
import data.map.HeightMap;
import data.map.Mask;
import data.map.layout.CellularLayoutMap;
import data.util.MapUtils;
import generator.climate.ClimateGenerator;
import generator.erosion.ErosionGenerator;
import generator.erosion.ThermalWeatheringGenerator;
import generator.noise.NoiseGenerator;
import io.ImageIO;

public class TerrainGeneratorHills implements ITerrainGenerator {
	private static final String identifier = "Hills";
	
	
	private boolean isMesaVariant = false;
	private static TerrainGeneratorHills mesaVariant = new TerrainGeneratorHills(false, true);
	
	private TerrainGeneratorHills(boolean dv, boolean mv) {
		this.isMesaVariant = mv;
	}
	
	public TerrainGeneratorHills() {
		this.isMesaVariant = false;
	}
	
	public TerrainGeneratorHills getMesaVariant() {
		return mesaVariant;
	}
	
	@Override
	public boolean isLowerTerrain() {
		return false;
	}

	@Override
	public HeightMap generateTerrain(CellularLayoutMap clm, HeightMap layout, World world, long seed) {
		
		float overshootTolerance = 0.1f;
		
		HeightMap bl = new HeightMap(clm.getTerracedLayout().clone());
		HeightMap ml = new HeightMap(clm.getTerracedLayoutRaised().clone());
		
		float mountainHeight = ml.getMax();
		
		ImageIO.saveAsGreyImage(ml, "MaxLevels");
		
		
		
		MapUtils.convSmooth(bl, 5);
		
		for(int i = 0; i < 24; i++) {
			MapUtils.repeatedConvSmooth(ml, 3, 1);
			ml = ml.max(clm.getTerracedLayoutRaised());
		}
		
		
		ArrayList<Biome> biomes = clm.getBiomesForTerrainGeneratorIdentifier(identifier);
		Mask terraGenMask = clm.getMaskForBiomes(biomes);
		
		ArrayList<Biome> mesaBiomes = clm.getBiomesForTerrainGenerator(mesaVariant);
		Mask mesaMask = clm.getMaskForBiomes(mesaBiomes);
		
		ImageIO.saveAsGreyImage(mesaMask, "Mesa mask");
		
		HeightMap rflow = new HeightMap(clm.getRiverInfo().createFlowMap());
		MapUtils.repeatedConvSmooth(rflow, 2, 2);
		Mask rivers = rflow.above(0.1f);
		rivers.invert();
		terraGenMask.and(rivers);
		mesaMask.and(rivers);
		
		HeightMap rm = rivers.toHeightMap(1f, 0f);
		HeightMap gd = MapUtils.geoDistAboveHeight(rm, 0f, 8);
		
		bl = MapUtils.lincomb(bl, clm.getTerracedLayout(), gd);
		
		ml = ml.max(bl);
		
		ImageIO.saveAsGreyImage(terraGenMask, "Hill mask");
		HeightMap noise = NoiseGenerator.createPerlinNoiseMap(layout.getXSize(), layout.getYSize(), seed, 2, false);
		noise.add(-0.32f);
		noise.mult(2f);
		noise.cap();
		ImageIO.saveAsGreyImage(noise, "noise");
		noise = new HeightMap(MapUtils.cappedMultiply(noise, gd));
		
		HeightMap ret = new HeightMap(layout.getXSize(), layout.getYSize());
		
		for(int i = 0; i < layout.getXSize(); i++) {
			for(int j = 0; j < layout.getYSize(); j++) {
				float h = ml.getValueAt(i, j) - bl.getValueAt(i, j);
				h = Math.max(h, overshootTolerance);
				h = h * noise.getValueAt(i, j) + bl.getValueAt(i, j);
				ret.setValueAt(i, j, h);
			}
		}
		
		HeightMap tgmh = terraGenMask.toHeightMap(1f, 0f);
		ret = new HeightMap(MapUtils.cappedMultiply(ret, tgmh));
		
		bl = clm.getTerracedLayout();
		Mask mountains = bl.above(mountainHeight - 0.1f);
		bl.applyMask(mountains);
		ret = ret.max(bl);
		
		ErosionGenerator.boringErosion(ret, 1, 0.4f, 1, 32, 0.001f, 0.001f, 0.01f, 0.2f);
		HeightMap retE = new HeightMap(ret.clone());
		ErosionGenerator.boringErosion(retE, 4, 0.4f, 2, 16, 0.0001f, 0.001f, 0.001f, 0.01f);
		ErosionGenerator.boringErosion(retE, 4, 0.4f, 1, 8, 0.0001f, 0.001f, 0.01f, 0.01f);
		ret = MapUtils.lincomb(retE, ret, gd);
		ret.cap();
		MapUtils.convSmooth(ret, 1, 0f, 4);
		HeightMap retS = new HeightMap(ret.clone());
		Mask low = retS.below(0.2f);
		retS.applyMask(low);
		MapUtils.convSmooth(retS, 2, 0f, 4);
		ret = ret.max(retS);
		gd.invert();
		HeightMap riverBase = clm.getTerracedLayout();
		riverBase.applyMask(gd.above(0.5f));
		ret = ret.max(riverBase);
		
		HeightMap mesa = new HeightMap(ml.clone());
		mesa.applyMask(mesaMask);
		ret = ret.max(mesa);
		
		ImageIO.saveAsGreyImage(ret, "Hills");
		MapUtils.smoothBiggen(ret, WorldSettings.layoutRatio);
		
		ImageIO.saveAsGreyImage(ret, "Hills big");
		return ret;
	}

	@Override
	public String identify() {
		return identifier;
	}

}
