package main;

import java.util.Random;

import data.World;
import data.WorldSettings;
import data.biomes.BiomeMapGenerator;
import data.map.CrustMap;
import data.map.HeightMap;
import data.map.Mask;
import data.map.OceanCurrentEffectMap;
import data.map.PrecipitationMap;
import data.map.TemperatureMap;
import data.map.layout.CellularLayoutMap;
import data.util.MapUtils;
import data.util.MathUtils;
import generator.GeneratorHandler;
import generator.climate.ClimateGenerator;
import generator.erosion.ErosionGenerator;
import generator.erosion.GridBasedErosion;
import generator.erosion.ThermalWeatheringGenerator;
import generator.height.TerrainGeneratorPlains;
import generator.noise.BiomeHeightsGenerator;
import generator.noise.NoiseGenerator;
import generator.tectonics.HeightGenerator;
import generator.tectonics.TectonicsGenerator;
import generator.water.LakeBuilder;
import generator.water.rivers.RiverBuilder;
import generator.water.rivers.RiverFlowField;
import generator.water.rivers.RiverInfo;
import generator.water.rivers.RiverLayoutGenerator;
import io.ImageIO;
import io.WorldIO;

public class WorldBuilderTester {
	private static long seed = WorldSettings.seed;
	
	public static void main(String[] args) {
		testBeach();
	}
	
	private static void testImageIo() {
		HeightMap h1 = ImageIO.loadGreyImage("test");
		HeightMap h2 = ImageIO.loadGreyImage("test1");
		HeightMap h3 = ImageIO.loadGreyImage("test11");
		ImageIO.saveAsGreyImage(h3, "test2");
	}
	
	private static void testLakes() {
		HeightMap h = ImageIO.loadGreyImage("1");
		Mask lakeMask = LakeBuilder.createLakeMask(h);
		ImageIO.saveAsGreyImage(lakeMask, "LakeMask");
		HeightMap l = LakeBuilder.createLakes(lakeMask, seed);
		ImageIO.saveAsGreyImage(l, "Lakes");
	}
	
	private static void testWaterFlow() {
		CrustMap m = GeneratorHandler.generateTectonicMap(1024, 1024, 20, 0.5F, seed);
		ImageIO.saveAsImage(m);
		HeightMap h = GeneratorHandler.generateTectonicLayoutMap(m, 64);
		ImageIO.saveAsImage(h);
		HeightMap f = HeightGenerator.noiseLayout(h, null, seed);
		ImageIO.saveAsImage(f);
		Mask w = RiverLayoutGenerator.createTraceLayout(f, 0.65F, 0.45F, seed).getRiverMask(1, 200);
		ImageIO.saveAsImage(w);
	}
	
	private static void testLayoutCombiner() {
		HeightMap l = ImageIO.loadGreyImage("test");
		MapUtils.smoothBiggen(l, 8);
		HeightMap w = HeightGenerator.noiseLayout(l, null, seed);
		ImageIO.saveAsGreyImage(w, "lh");
	}
	
	private static void testAlternativeRiverGen() {
		CrustMap m = GeneratorHandler.generateTectonicMap(1024, 1024, 20, 0.5F, seed);
		ImageIO.saveAsImage(m);
		HeightMap h = GeneratorHandler.generateTectonicLayoutMap(m, 64);
		//MapUtils.smoothBiggen(h, 2);
		System.out.println(h.getXSize());
		ImageIO.saveAsImage(h);
		RiverLayoutGenerator.createCellularLayout(h, 0.7F, 0.45F, 2, seed);
	}
	
	private static void testTectonic() {
		CrustMap m = GeneratorHandler.generateTectonicMap(512, 512, 30, 0.5F, seed);
		m = TectonicsGenerator.toIsland(m);
		ImageIO.saveAsImage(m);
		HeightMap h = GeneratorHandler.generateTectonicLayoutMap(m, 64);
		System.out.println(h.getXSize());
		ImageIO.saveAsImage(h);
		RiverInfo riverInfo = RiverLayoutGenerator.createCellularLayout(h, 0.8F, 0.45F, 1, seed);
		HeightMap f = HeightGenerator.noiseLayout(h, riverInfo, seed);
		riverInfo.biggen(4);
		//ErosionGenerator.realisticMountainErosion(f);
		ImageIO.saveAsImage(f);
		System.out.println(f.getXSize());
		MapUtils.biggen(f, 4);
		//ErosionGenerator.boringErosion(f, 2, 0.01f, 4, 128, 0.1f, 0.1f, 0.01f, 0.25f);
		MapUtils.smoothBiggen(f, 2);
		//ErosionGenerator.erode(f, 32);
		MapUtils.smoothBiggen(f, 2);
		
		riverInfo.biggen(4);
		RiverBuilder.finishLowRivers(riverInfo, f, 16, 0.3F, 0.24F, seed);
		MapUtils.smoothAt(f, 1, (short) 0, 9000);
		
		ImageIO.saveAsGreyImage(f, "world");
		
		//HeightMap forest = BiomeMapGenerator.generateForests(f, seed);
		//ImageIO.saveAsGreyImage(forest, "forest");
	}
	
	private static void testNoise() {
		HeightMap h = NoiseGenerator.createHillsNoiseMap(1024, 1024, seed, 4);
		ImageIO.saveAsImage(h);
	}
	
	private static void testErosion() {
		//HeightMap h = NoiseGenerator.createMountainNoiseMap(256, 256, seed);
		//ErosionGenerator.erode(h, 128);
		//MapUtils.smoothBiggen(h, 8);
		
		//ImageIO.saveAsImage(h, "old");
		HeightMap h = NoiseGenerator.createHillsNoiseMap(512, 512, seed, 8);
		//HeightMap h = MapUtils.createGradientWorld(512, 512);
		//ErosionGenerator.advancedErosion(h, 16);
		ImageIO.saveAsImage(h, "old");
		//ErosionGenerator.advancedErosion(h, 4, 0.5F, 64, 4, false);
		long time = System.currentTimeMillis();
		HeightMap hm = new HeightMap(h.clone());
		//ErosionGenerator.fractalErosion(h, 4, 0.75f, 0.5f, false);
		ErosionGenerator.boringErosion(h, 4, 0.75f, 4, 128, 0.01f, 0.2f, 0.001F, 0.5f, 6f);
		//ErosionGenerator.realisticMountainErosion(h);
		//ErosionGenerator.erode(h, 32);
		//h = GridBasedErosion.erode(h, 1);
		System.out.println(System.currentTimeMillis() - time + "ms ");
		ImageIO.saveAsImage(h, "new");
		ImageIO.saveAsGreyImage(MapUtils.difference(h, hm), "erosion changes");
	}
	
	private static void testClimate() {
		Random rng = new Random(seed + 69420);
		
		for(int i = 0; i < 1; i++) {
			long nseed = rng.nextLong();
			
			CrustMap m = GeneratorHandler.generateTectonicMap(1024, 1024, 50, 0.4F, nseed);
			m = TectonicsGenerator.toIsland(m);
			ImageIO.saveAsImage(m);
			HeightMap h = GeneratorHandler.generateTectonicLayoutMap(m, 64);
			System.out.println(h.getXSize());
			//ImageIO.saveAsImage(h);
			
			
			
			CellularLayoutMap clm = new CellularLayoutMap(h, 1, nseed);
			ImageIO.saveAsImage(clm, "biomes" + i);
		}
	}
	
	private static void testConv() {
		CrustMap m = GeneratorHandler.generateTectonicMap(1024, 1024, 20, 0.2F, seed + 323522);
		HeightMap h = GeneratorHandler.generateTectonicLayoutMap(m, 64);
		MapUtils.normalize(h);
		ImageIO.saveAsGreyImage(h.above(WorldSettings.layoutSeaLevel), "land");
		OceanCurrentEffectMap oce = ClimateGenerator.createOceanCurrentEffectMap(h, 4, 3, false);
		ImageIO.saveAsImage(oce, "current effects");
		
	}
	
	private static void testBiomeHeights() {
		CrustMap m = GeneratorHandler.generateTectonicMap(1024, 1024, 20, 0.2F, seed + 323522);
		CrustMap c = TectonicsGenerator.toIsland(m);
		HeightMap h = GeneratorHandler.generateTectonicLayoutMap(c, 64);
		MapUtils.normalize(h);
		System.out.println(h.getXSize());
		
		CellularLayoutMap clm = new CellularLayoutMap(h, 1, seed + 323522);
		ImageIO.saveAsImage(clm, "biomes");
		//BiomeHeightsGenerator.initHMaps(8192, 8192);
		World hm = HeightGenerator.fromBiomeMap(clm, h, seed);
		hm.saveAll();
	}
	
	private static void testWorldCompression() {
		World w = new World(1024, 1024);
		HeightMap h = NoiseGenerator.createHillsNoiseMap(1024, 1024, seed, 8);
		ImageIO.saveAsGreyImage(h, "noise");
		w.setUpperTerrain(h);
		w.saveAll();
	}
	
	
	private static void testThermalWeathering() {
		HeightMap h = NoiseGenerator.createMountainNoiseMap(1024, 1024, seed);
		ImageIO.saveAsImage(h, "old");
		long time = System.currentTimeMillis();
		HeightMap hm = new HeightMap(h.clone());
		System.out.println(System.currentTimeMillis() - time + "ms ");
		ImageIO.saveAsImage(h, "new");
		ImageIO.saveAsGreyImage(MapUtils.difference(h, hm), "erosion changes");
	}
	
	private static void testBeach() {
		HeightMap h = MapUtils.createGradientWorld(1024, 1024);
		Mask m = h.above(0.5f);
		h.add(-0.5f);
		h.mult(2f);
		h.cap();
		HeightMap hm = NoiseGenerator.createHillsNoiseMap(1024, 1024, seed, 4);
		hm.mult(0.15f);
		hm.add(0.1f);
		HeightMap beach = new TerrainGeneratorPlains().createBeachSubMap(m, h, hm, seed);
		ImageIO.saveAsGreyImage(beach, "Beach");
	}
	
}
