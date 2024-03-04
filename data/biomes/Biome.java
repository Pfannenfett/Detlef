package data.biomes;

import java.awt.Color;

import data.WorldSettings;
import data.map.HeightMap;
import data.map.Mask;
import data.map.PrecipitationMap;
import data.map.TemperatureMap;
import generator.height.ITerrainGenerator;
import generator.height.TerrainGeneratorHills;
import generator.height.TerrainGeneratorMountains;
import generator.height.TerrainGeneratorOceans;
import generator.height.TerrainGeneratorPlains;
import generator.noise.BiomeHeightsGenerator;
import generator.tectonics.HeightGenerator;

public class Biome {
	public static final float[] ALLT = {0f, 1f};
	public static final float[] HOT = {0.8f, 1f};
	public static final float[] WARM = {0.6f, 0.8f};
	public static final float[] TEMPERATE = {0.4f, 0.6f};
	public static final float[] COLD = {0.2f, 0.4f};
	public static final float[] ARCTIC = {0f, 0.2f};
	
	public static final float[] ALLP = {0f, 1f};
	public static final float[] WET = {0.6f, 1f};
	public static final float[] MODERATE = {0.25f, 0.6f};
	public static final float[] DRY = {0f, 0.25f};
	
	public static final float[] UNDERWATER = {0f, WorldSettings.layoutSeaLevel};
	public static final float[] LAND = {WorldSettings.layoutSeaLevel, 1f};
	public static final float[] LOW = {WorldSettings.layoutSeaLevel, (WorldSettings.layoutForestMaxLevel - WorldSettings.layoutSeaLevel) / 2 + WorldSettings.layoutSeaLevel};
	public static final float[] HIGH = {(WorldSettings.layoutForestMaxLevel - WorldSettings.layoutSeaLevel) / 2 + WorldSettings.layoutSeaLevel, WorldSettings.layoutForestMaxLevel};
	public static final float[] PEAK = {WorldSettings.layoutForestMaxLevel, 1f};
	
	
	public static final Color OCEANCOLOR = Color.BLUE;
	public static final Color FORESTCOLOR = new Color(0, 0.5f, 0);
	public static final Color PLAINCOLOR = Color.GREEN;
	public static final Color MOUNTAINCOLOR = Color.DARK_GRAY;
	public static final Color ICECOLOR = Color.WHITE;
	public static final Color TAIGACOLOR = new Color(0.5f, 0.5f, 1f);
	public static final Color TUNDRACOLOR = new Color(0.5f, 0.1f, 0.6f);
	public static final Color DESERTCOLOR = Color.YELLOW;
	public static final Color MESACOLOR = Color.ORANGE;
	public static final Color SWAMPCOLOR = new Color(0.1f, 0.3f, 0.1f);
	public static final Color SAVANNACOLOR = new Color(0.3f, 0.8f, 0);
	public static final Color JUNGLECOLOR = new Color(0.0f, 0.2f, 0);
	public static final Color BOREALFORESTCOLOR = new Color(0.0f, 0.8f, 0.4f);
	public static final Color REDDESERTCOLOR = Color.RED;
	
	public static final TerrainGeneratorOceans OCEANS = new TerrainGeneratorOceans();
	public static final TerrainGeneratorPlains PLAINS = new TerrainGeneratorPlains();
	public static final TerrainGeneratorHills HILLS = new TerrainGeneratorHills();
	public static final TerrainGeneratorMountains MOUNTAINS = new TerrainGeneratorMountains();
	
	private Color biomePreviewColor;
	private String biomeID;
	
	private float minTemperature;
	private float maxtemperature;
	
	private float minPrecipitation;
	private float maxPrecipitation;
	
	private float minHeight;
	private float maxHeight;
	
	private boolean isRiver = false;
	private ITerrainGenerator hgen;
	
	public Biome(String name, Color c, float lt, float mt, float lp, float mp, float lh, float mh, ITerrainGenerator hg) {
		biomeID = name;
		biomePreviewColor = c;
		minTemperature = lt;
		maxtemperature = mt;
		minPrecipitation = lp;
		maxPrecipitation = mp;
		minHeight = lh;
		maxHeight = mh;
		hgen = hg;
	}
	
	public Biome(String name, Color c, float[] t, float[] p, float[] h, ITerrainGenerator hg) {
		biomeID = name;
		biomePreviewColor = c;
		minTemperature = t[0];
		maxtemperature = t[1];
		minPrecipitation = p[0];
		maxPrecipitation = p[1];
		minHeight = h[0];
		maxHeight = h[1];
		hgen = hg;
	}
	
	public Biome clone() {
		return new Biome(biomeID, biomePreviewColor, minTemperature, maxtemperature, minPrecipitation, maxPrecipitation, minHeight, maxHeight, hgen);
	}
	
	public void markAsRiver() {
		if(maxHeight <= WorldSettings.layoutSeaLevel) return;
		isRiver = true;
		biomePreviewColor = new Color(0.2f, 0.6f, 1f);
	}
	
	public boolean isValid(float height, float temperature, float precipitation) {
		boolean ret = true;
		ret &= height >= minHeight && height <= maxHeight;
		ret &= precipitation >= minPrecipitation && precipitation <= maxPrecipitation;
		ret &= temperature >= minTemperature && temperature <= maxtemperature;
		return ret;
	}
	
	public float isPartValid(float height, float temperature, float precipitation, float hd, float td, float pd) {
		float uhd = Math.max(height - maxHeight, minHeight - height) / hd;
		float upd = Math.max(precipitation - maxPrecipitation, minPrecipitation - precipitation) / pd;
		float utd = Math.max(temperature - maxtemperature, minTemperature - temperature) / td;
		return Math.max(Math.max(upd, uhd), utd);
	}
	
	public ITerrainGenerator getTerrainGenerator() {
		return hgen;
	}
	
	public Color getBiomePreviewColor() {
		return biomePreviewColor;
	}
	
	public float getMaxHeight() {
		return maxHeight;
	}
	
	public float getMinHeight() {
		return minHeight;
	}
	
	public float getMeanHeight() {
		return (maxHeight + minHeight) / 2;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Biome) {
			Biome b = (Biome) obj;
			return b.biomeID == biomeID && (b.isRiver == isRiver);
		}
		return super.equals(obj);
	}
	
	public boolean isHillBiome() {
		return minHeight >= HIGH[0] && maxHeight <= HIGH[1];
	}
	
	public boolean isRiver() {
		return isRiver;
	}
}
