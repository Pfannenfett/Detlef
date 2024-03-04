package generator.height;

import java.util.ArrayList;

import data.World;
import data.WorldSettings;
import data.biomes.Biome;
import data.map.HeightMap;
import data.map.Mask;
import data.map.layout.CellularLayoutMap;
import data.util.MapUtils;

public class TerrainGeneratorOceans implements ITerrainGenerator {
	private static final String identifier = "Oceans";
	
	@Override
	public boolean isLowerTerrain() {
		return true;
	}
	
	@Override
	public HeightMap generateTerrain(CellularLayoutMap clm, HeightMap layout, World world, long seed) {
		HeightMap ret = new HeightMap(clm.getXSize(), clm.getYSize());
		
		ArrayList<Biome> biomes = clm.getBiomesForTerrainGeneratorIdentifier(identifier);
		Mask terraGenMask = clm.getMaskForBiomes(biomes);
		
		HeightMap tgm = terraGenMask.toHeightMap();
		HeightMap gd = MapUtils.geoDistAboveHeight(tgm, 0f, 16);
		
		gd.invert();
		gd.mult(0.5f);
		
		MapUtils.smoothBiggen(gd, WorldSettings.layoutRatio);
		
		return gd;
	}
	
	@Override
	public String identify() {
		return identifier;
	}
}
