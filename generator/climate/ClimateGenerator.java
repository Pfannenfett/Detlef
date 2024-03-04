package generator.climate;

import java.util.Random;

import data.WorldSettings;
import data.biomes.Biome;
import data.map.HeightMap;
import data.map.MapBase;
import data.map.Mask;
import data.map.OceanCurrentEffectMap;
import data.map.PrecipitationMap;
import data.map.TemperatureMap;
import data.util.MapUtils;
import generator.Generator;
import generator.noise.NoiseGenerator;
import generator.water.rivers.RiverInfo;
import io.ImageIO;

public class ClimateGenerator extends Generator {
	
	public static int[][][] winds;
	
	public static int[][][] createStandardWindfield(int xSize, int ySize, int cells, boolean northHalfOnly) {
		int[][][] ret = new int[xSize][ySize][2];
		Random rng = new Random(WorldSettings.seed);
		int cellSize = (int) ySize / cells;
		if(northHalfOnly) {
			for(int i = 0; i < xSize; i++) {
				for(int j = 1; j <= ySize; j++) {
					int cell = (int) j / cellSize;
					float c = j % cellSize;
					float r = rng.nextFloat();;
					c = c / cellSize;
					if(cell % 2 == 0) {
						ret[i][ySize - j][0] = r > c ? i - 1 : i;
						ret[i][ySize - j][1] = r < c ? ySize - j + 1 : ySize - j;
					} else {
						ret[i][ySize - j][0] = r < c ? i + 1 : i;
						ret[i][ySize - j][1] = r > c ? ySize - j - 1 : ySize - j;
					}
				}
			}
		} else {
			int yh = ySize / 2;
			cellSize /= 2;
			for(int i = 0; i < xSize; i++) {
				for(int j = 1; j <= yh; j++) {
					int cell = (int) j / cellSize;
					if(cell % 2 == 0) {
						ret[i][yh - j][0] = i - 1;
						ret[i][yh - j][1] = yh - j - 1;
					} else {
						ret[i][yh - j][0] = i + 1;
						ret[i][yh - j][1] = yh - j + 1;
					}
				}
				
				for(int j = 0; j < yh; j++) {
					int cell = (int) j / cellSize;
					if(cell % 2 == 0) {
						ret[i][yh + j][0] = i - 1;
						ret[i][yh + j][1] = yh + j + 1;
					} else {
						ret[i][yh + j][0] = i + 1;
						ret[i][yh + j][1] = yh + j + 1;
					}
				}
			}
		}
		return ret;
	}
	
	public static PrecipitationMap simulateWinds(OceanCurrentEffectMap oce, int[][][] winds, HeightMap heights) {
		
		assert oce.getXSize() == winds.length && oce.getYSize() == winds[0].length && oce.getXSize() == heights.getXSize() && oce.getYSize() == heights.getYSize() : "Kartengrößen der Feuchtigkeitssimulation stimmen nicht überein!";
		
		Random rng = new Random(WorldSettings.seed);
		Mask oceans = heights.below(WorldSettings.layoutSeaLevel);
		HeightMap temp = new HeightMap(oce.getContent());
		temp.applyMask(oceans);
		PrecipitationMap ret = new PrecipitationMap(temp.getContent().clone());
		float airspace = 1f - WorldSettings.layoutSeaLevel;
		
		int simTime = Math.max(heights.getXSize(), heights.getYSize());
		
		float oneOverT = 1f / simTime;
		oneOverT = 1f;
		
		System.out.println("\n\nPerforming Windsimulation");
		System.out.println("IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
		
		int percentTimer = simTime / 100;
		
		for(int t = 0; t < simTime; t++) {
			for(int i = 0; i < oce.getXSize(); i++) {
				for(int j = 0; j < oce.getYSize(); j++) {
					int x = winds[i][j][0];
					int y = winds[i][j][1];
					if(x < 0 || x >= oce.getXSize() || y < 0 || y >= oce.getYSize()) continue;
					float f = temp.getValueAt(i, j);
					float delta = heights.getValueAt(x, y) - heights.getValueAt(i, j);
					delta = Math.max(delta, 0f);
					delta /= airspace;
					if(heights.getValueAt(x, y) > 0.8f) delta = 0.2f;
					float d = rng.nextFloat() * oneOverT * 0.95f;
					ret.setValueAt(x, y, Math.max(ret.getValueAt(x, y), (1 - delta) * f)); 
					
					
					ret.setValueAt(x + 1, y, Math.max(ret.getValueAt(x + 1, y) , (1 - delta) * f * d));
					ret.setValueAt(x, y + 1, Math.max(ret.getValueAt(x, y + 1) , (1 - delta) * f * d));
					ret.setValueAt(x - 1, y, Math.max(ret.getValueAt(x - 1, y) , (1 - delta) * f * d));
					ret.setValueAt(x, y - 1, Math.max(ret.getValueAt(x, y - 1) , (1 - delta) * f * d));
				}
			}
			if(t % percentTimer == 0) System.out.print("I");
			temp = new HeightMap(ret.getContent().clone());
		}
		
		System.out.println();
		return ret;
	}
	
	public static TemperatureMap createStandardTemperaturMap(int xSize, int ySize, boolean northHalfOnly) {
		TemperatureMap ret = new TemperatureMap(xSize, ySize);
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				float f = j;
				f /= ySize;
				if(!northHalfOnly) {
					f -= 0.5F;
					f = Math.abs(f);
					f *= -2;
					f += 1;
				}
				ret.setValueAt(i, j, f);
			}
		}
		return ret;
	}
	
	public static TemperatureMap createTemperatureMap(HeightMap l, float waterLevel, boolean northHalfOnly) {
		TemperatureMap t = ClimateGenerator.createStandardTemperaturMap(l.getXSize(), l.getYSize(), northHalfOnly);
		HeightMap layout = new HeightMap(l.clone());
		HeightMap coastDist = MapUtils.geoDistAboveHeight(layout, 0.5f, layout.getXSize() / 4);
		coastDist.mult(0.1f);
		layout.add(-waterLevel);
		layout.add(-0.2f);
		layout.cap();
		MapUtils.normalize(layout);
		layout.mult(0.2f);
		
		t = new TemperatureMap(MapUtils.addDiminMap(t, coastDist));
		MapUtils.shrink(t, 16);
		MapUtils.smooth(t, 1);
		MapUtils.biggen(t, 4);
		MapUtils.smoothBiggen(t, 4);
		t = new TemperatureMap(MapUtils.substractDiminMap(t, layout));
		t.cap();
		
		MapBase noise = NoiseGenerator.createHillsNoiseMap(l.getXSize() * 2, l.getYSize() * 2, WorldSettings.seed + 2, l.getXSize() / 128);
		noise = MapUtils.submap(noise, l.getXSize() / 2, l.getYSize() / 2, l.getXSize(), l.getYSize());
		noise.mult(0.4f);
		ImageIO.saveAsGreyImage(noise, "noise");
		noise.add(-0.2f);
		
		t = new TemperatureMap(MapUtils.addDiminMap(t, noise));
		return t;
	}
	
	public static PrecipitationMap createAdvancedRainfallMap(HeightMap l, TemperatureMap tm, float waterLevel, int cells, boolean northHalfOnly) {
		PrecipitationMap ret = createRainfallMap(l, tm, waterLevel);
		ret.mult(0.25f);
		winds = createStandardWindfield(l.getXSize(), l.getYSize(), cells, northHalfOnly);
		OceanCurrentEffectMap oce = createOceanCurrentEffectMap(l, 4, cells, northHalfOnly);
		PrecipitationMap windSim = simulateWinds(oce, winds, l);
		MapUtils.repeatedConvSmooth(windSim, 8, 4);
		return new PrecipitationMap(MapUtils.addDiminMap(ret, windSim));
	}
	
	public static PrecipitationMap createRainfallMap(HeightMap l, TemperatureMap tm, float waterLevel) {
		HeightMap coastDist = MapUtils.geoDistAboveHeight(l, 0.5f, l.getXSize() / 2);
		MapUtils.invert(coastDist);
		TemperatureMap tmp = new TemperatureMap(tm.clone());
		PrecipitationMap pm = new PrecipitationMap(MapUtils.cappedMultiply(coastDist, tmp).clone());
		HeightMap layout = new HeightMap(l.clone());
		layout.add(-waterLevel);
		layout.add(-0.4f);
		layout.cap();
		MapUtils.normalize(layout);
		layout.mult(0.4f);
		pm = new PrecipitationMap(MapUtils.substractDiminMap(pm, layout));
		pm = new PrecipitationMap(MapUtils.addDiminMap(pm, crf(l)));
		
		MapBase noise = NoiseGenerator.createHillsNoiseMap(l.getXSize() * 2, l.getYSize() * 2, WorldSettings.seed + 4, l.getXSize() / 128);
		noise = MapUtils.submap(noise, l.getXSize() / 2, l.getYSize() / 2, l.getXSize(), l.getYSize());
		ImageIO.saveAsGreyImage(noise, "noise");
		noise.add(-0.5f);
		
		pm = new PrecipitationMap(MapUtils.addDiminMap(pm, noise));
		
		pm.cap();
		return pm;
	}
	
	public static PrecipitationMap crf(HeightMap layout) {
		HeightMap slopes = MapUtils.slopes(layout);
		slopes.mult(1f / WorldSettings.XYRATIO);
		//MapUtils.normalize(slopes);
		//MapUtils.curve(slopes);
		//slopes.cap();
		MapUtils.normalize(slopes);
		HeightMap cd = MapUtils.geoDistAboveHeight(layout, 0.5f, layout.getXSize() / 2);
		MapUtils.invert(cd);
		PrecipitationMap pm = new PrecipitationMap(MapUtils.cappedMultiply(slopes, cd));
		MapUtils.curve(pm);
		pm.mult(64f);
		pm.cap();
		MapUtils.shrink(pm, 16);
		MapUtils.smooth(pm, 1);
		MapUtils.biggen(pm, 4);
		MapUtils.smoothBiggen(pm, 4);
		//pm.cap();
		return pm;
	}
	
	public static OceanCurrentEffectMap createOceanCurrentEffectMap(HeightMap layout, int diffusion, int cells, boolean northHalfOnly) {
		Mask m = layout.above(WorldSettings.layoutSeaLevel);
		HeightMap landMask = m.toHeightMap(1, 0);
		float[][] edgeDetectKernel = {{1,1,1},{0,0,0},{-1,-1,-1}};
		float[][] convoluted = MapUtils.convolute(landMask, edgeDetectKernel);
		
		for(int i = 0; i < convoluted.length; i++) {
			for(int j = 0; j < convoluted[0].length; j++) {
				if(i > 0 && i < convoluted.length - 1 && j > 0 && j < convoluted[0].length - 1) {
					convoluted[i][j] += 0.5f;
				} else {
					convoluted[i][j] = 0.5f;
				}
			}
		}
		
		int cellSize = (int) (convoluted[0].length / cells);
		if(!northHalfOnly) {
			cellSize /= 2;
			for(int i = 0; i < convoluted.length; i++) {
				for(int j = 0; j < convoluted[0].length / 2; j++) {
					if((j/cellSize)%2 == 1) {
						convoluted[i][convoluted[0].length / 2 - 1 - j] = 1f - convoluted[i][convoluted[0].length / 2 - 1 - j];
					}
				}
				for(int j = 0; j < convoluted[0].length / 2; j++) {
					if((j/cellSize)%2 == 1) {
						convoluted[i][convoluted[0].length / 2 + j] = 1f - convoluted[i][convoluted[0].length / 2 + j];
					}
				}
			}
		} else {
			for(int i = 0; i < convoluted.length; i++) {
				for(int j = 0; j < convoluted[0].length; j++) {
					if((j/cellSize)%2 == 1) {
						convoluted[i][convoluted[0].length - 1 - j] = 1f - convoluted[i][convoluted[0].length - 1 - j];
					}
				}
			}
		}
		
		
		/*float[][] temp = new float[convoluted.length][convoluted[0].length];
		
		for(int i = 0; i < convoluted.length; i++) {
			for(int j = 0; j < convoluted[0].length / 2; j++) {
				temp[i][j] = convoluted[i][j + convoluted[0].length / 2];
			}
			for(int j = convoluted[0].length / 2; j < convoluted[0].length; j++) {
				temp[i][j] = convoluted[i][convoluted[0].length + (convoluted[0].length / 2) - j - 1];
			}
		}
		convoluted = temp;*/
		
		OceanCurrentEffectMap ret = new OceanCurrentEffectMap(convoluted);
		MapUtils.normalize(ret);
		ImageIO.saveAsImage(ret, "before");
		for(int k = 0; k < diffusion; k++) {
			convoluted = MapUtils.widenExtremes(ret.getContent(), 8);
			ret = new OceanCurrentEffectMap(convoluted);
			for(int i = 0; i < convoluted.length; i++) {
				for(int j = 0; j < convoluted[0].length; j++) {
					if(convoluted[i][j] > 0.55f) {
						convoluted[i][j] = 1f;
					} else if(convoluted[i][j] < 0.45f) {
						convoluted[i][j] = 0f;
					}
				}
			}
			MapUtils.repeatedConvSmooth(ret, 3, diffusion, 0.5f);
			convoluted = MapUtils.widenExtremes(ret.getContent(), 8);
			ret = new OceanCurrentEffectMap(convoluted);
			MapUtils.normalize(ret);
		}
		ImageIO.saveAsImage(ret, "after");
		m.invert();
		ret.applyMask(m, 0.5f);
		MapUtils.repeatedConvSmooth(ret, 3, diffusion, 0.5f);
		return ret;
	}
	
	
	public static PrecipitationMap addRiverMoistureSpread(Mask riverMask, PrecipitationMap base, TemperatureMap temperature) {
		HeightMap prec = new HeightMap(base.clone());
		HeightMap rivers = riverMask.toHeightMap(0.5f, 0f);
		MapUtils.convSmooth(rivers, 2);
		prec = new HeightMap(MapUtils.addDiminMap(prec, rivers));
		rivers.applyMask(temperature.below(Biome.HOT[0]));
		MapUtils.convSmooth(rivers, 4);
		prec = new HeightMap(MapUtils.addDiminMap(prec, rivers));
		rivers.applyMask(temperature.above(Biome.COLD[1]));
		MapUtils.convSmooth(rivers, 4);
		prec = new HeightMap(MapUtils.addDiminMap(prec, rivers));
		
		return new PrecipitationMap(prec);
	}
}
