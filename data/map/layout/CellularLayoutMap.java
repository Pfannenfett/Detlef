package data.map.layout;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import data.WorldSettings;
import data.biomes.Biome;
import data.biomes.BiomeMapGenerator;
import data.biomes.Biomes;
import data.map.HeightMap;
import data.map.IPrintableMap;
import data.map.Mask;
import data.map.PrecipitationMap;
import data.map.TemperatureMap;
import data.util.MapUtils;
import generator.climate.ClimateGenerator;
import generator.height.ITerrainGenerator;
import generator.noise.NoiseGenerator;
import generator.water.rivers.River;
import generator.water.rivers.RiverInfo;
import generator.water.rivers.RiverLayoutGenerator;
import generator.water.rivers.RiverUtils;
import io.ImageIO;

public class CellularLayoutMap implements IPrintableMap {
	
	private Biome[][] biomes;
	private Biome[][] biomeLookUp;
	private RiverInfo rinf;
	private HeightMap terracedLayout;
	private HeightMap terracedLayoutRaised;
	
	private CellularLayoutMap() {
		
	}
	
	public CellularLayoutMap(HeightMap layout, int biomeSize, long seed) {
		
		biomes = new Biome[layout.getXSize() / biomeSize][layout.getYSize() / biomeSize];
		biomeLookUp = new Biome[layout.getXSize()][layout.getYSize()];
		
		TemperatureMap temperature = ClimateGenerator.createTemperatureMap(layout, 0.5f, true);
		PrecipitationMap rainfall = ClimateGenerator.createAdvancedRainfallMap(layout, temperature, WorldSettings.layoutSeaLevel, 3, true);
		//PrecipitationMap rainfall = ClimateGenerator.createRainfallMap(layout, temperature, WorldSettings.layoutSeaLevel);
		
		ImageIO.saveAsImage(temperature, "temperature");
		ImageIO.saveAsImage(rainfall, "rainfall");
		
		RiverLayoutGenerator.setRainfall(rainfall);
		rinf = RiverLayoutGenerator.createCellularLayout(layout, Biome.PEAK[0], Biome.UNDERWATER[1] - 0.05f, 1, seed);
		
		rainfall = ClimateGenerator.addRiverMoistureSpread(rinf.getRiverMask(1f, 10f), rainfall, temperature);
		
		Random rng = new Random(seed);
		int[][][] offset = new int[biomes.length][biomes[0].length][2];
		for(int i = 0; i < offset.length; i++) {
			for(int j = 0; j < offset[0].length; j++) {
				offset[i][j][0] = rng.nextInt(biomeSize);
				offset[i][j][1] = rng.nextInt(biomeSize);
			}
		}
		
		HeightMap biomeDecMakerMap = NoiseGenerator.createFloatNoiseMap(layout.getXSize() / 8, layout.getYSize() / 8, seed);
		MapUtils.smoothBiggen(biomeDecMakerMap, 8);
		ImageIO.saveAsGreyImage(biomeDecMakerMap, "decisionMap");
		
		for(int i = 0; i < biomes.length; i++) {
			for(int j = 0; j < biomes[0].length; j++) {
				int x = i * biomeSize + offset[i][j][0];
				int y = j * biomeSize + offset[i][j][1];
				biomes[i][j] = BiomeMapGenerator.getBiomeFor(layout.getValueAt(x, y), rainfall.getValueAt(x, y), temperature.getValueAt(x, y), biomeDecMakerMap.getValueAt(x, y)).clone();
			}
		}
		
		HashMap<Mask, Float> levels = new HashMap<>();
		Mask riverMask = rinf.getRiverMask(8, 200);
		riverMask = MapUtils.widen(riverMask, 4);
		Mask streamMask = rinf.getRiverMask(2, 4);
		streamMask = MapUtils.widen(streamMask, 1);
		Mask srMask = rinf.getRiverMask(4, 8);
		srMask = MapUtils.widen(srMask, 2);
		levels.put(riverMask, WorldSettings.waterHeightUp);
		levels.put(srMask, 0.4f);
		levels.put(riverMask, 0.6f);
		
		riverMask.or(streamMask);
		riverMask.or(srMask);
		riverMask.or(rinf.getRiverMask(1, 2));
		Mask deltaMask = RiverUtils.deltaDetect(rinf.getRivers(), layout, WorldSettings.layoutSeaLevel, WorldSettings.layoutSeaLevel - 0.05f, WorldSettings.layoutSeaLevel + 0.1f);
		ImageIO.saveAsGreyImage(deltaMask, "deltaMask");
		
		for(int i = 0; i < biomeLookUp.length; i++) {
			for(int j = 0; j < biomeLookUp[0].length; j++) {
				int cx = i / biomeSize;
				int cy = j / biomeSize;
				
				int minDist = layout.getXSize();
				int minX = cx;
				int minY = cy;
				
				for(int d = -1; d < 2; d++) {
					for(int e = -1; e < 2; e++) {
						try {
							int x = (cx + d) * biomeSize + offset[cx + d][cy + e][0];
							int y = (cy + e) * biomeSize + offset[cx + d][cy + e][0];
							
							x -= i;
							y -= j;
							
							int dist = x * x + y * y;
							if(dist < minDist) {
								minDist = dist;
								minX = cx + d;
								minY = cy + e;
							}
						} catch(Exception ee) {
							continue;
						}
					}
				}
				biomeLookUp[i][j] = biomes[minX][minY];
				if(riverMask.getValueAt(i, j)) {
					biomes[minX][minY].markAsRiver();
				}
			}
		}
		float[] terraceHeights = {0f, WorldSettings.layoutSeaLevel / 2, WorldSettings.layoutSeaLevel, Biome.HIGH[0], (Biome.HIGH[0] + Biome.HIGH[1]) / 2, Biome.HIGH[1], 1f};
		terracedLayout = MapUtils.terraces(layout, terraceHeights);
		terracedLayout.add(-WorldSettings.layoutSeaLevel);
		terracedLayout.cap();
		Mask cyclepreventer = new Mask(layout.getXSize(), layout.getYSize());
		HashSet<River> rivers = rinf.getRivers();
		HashSet<River> riversDone = new HashSet<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			if(riversDone.contains(r)) continue;
			boolean finished = false;
			while(!finished) {
				int valleySize = (int) (rng.nextInt(3) * r.getSize() + 2);
				valleySize = Math.min(valleySize, 32);
				int valleySize2 = 2 * valleySize;
				ArrayList<int[]> rps = r.getRiverpoints();
				int[] sp = rps.get(0);
				float height = terracedLayout.getValueAt(sp[0], sp[1]);
				for(int i = 0; i < rps.size(); i++) {
					int[] p = rps.get(i);
					
					
					if(cyclepreventer.getValueAt(p[0], p[1])) {
						finished = true;
						break;
					}
					
					for(int x = -valleySize2; x < valleySize2; x++) {
						for(int y = -valleySize2; y < valleySize2; y++) {
							if(x*x+y*y>valleySize*valleySize) continue;
							height = Math.min(height, terracedLayout.getValueAt(p[0], p[1]));
							terracedLayout.setValueAt(p[0]+x, p[1]+y, Math.min(terracedLayout.getValueAt(p[0] + x, p[1] + y), height));
							
							
						}
					}
					
				}
				
				riversDone.add(r);
				
				if(finished) {
					break;
				}
				
				if(r.getChild() == null) {
					finished = true;
				} else {
					finished = false;
					r = r.getChild();
				}
			} 
		}
		terracedLayoutRaised = new HeightMap(terracedLayout.clone());
		MapUtils.remapHeight(terracedLayoutRaised, (Biome.HIGH[0] + Biome.HIGH[1]) / 2 - WorldSettings.layoutSeaLevel, Biome.HIGH[1] - WorldSettings.layoutSeaLevel);
		MapUtils.remapHeight(terracedLayoutRaised, Biome.HIGH[0] - WorldSettings.layoutSeaLevel, (Biome.HIGH[0] + Biome.HIGH[1]) / 2 - WorldSettings.layoutSeaLevel);
		
		ImageIO.saveAsGreyImage(terracedLayout, "BaseLevels");
	}
	
	public RiverInfo getRiverInfo() {
		return rinf;
	}
	
	public CellularLayoutMap submap(int x, int y, int xSize, int ySize) {
		CellularLayoutMap ret = new CellularLayoutMap();
		
		Biome[][] rbiomeLookUp = new Biome[xSize][ySize];
		
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				rbiomeLookUp[i][j] = biomeLookUp[i + x][j + y];
			}
		}
		ret.biomeLookUp = rbiomeLookUp;
		return ret;
	}
	
	public HeightMap getTerracedLayout() {
		return new HeightMap(terracedLayout.clone());
	}
	
	public HeightMap getTerracedLayoutRaised() {
		return new HeightMap(terracedLayoutRaised.clone());
	}
	
	public ArrayList<Biome> getBiomesPresent() {
		ArrayList<Biome> ret = new ArrayList<>();
		for(int i = 0; i < biomes.length; i++) {
			for(int j = 0; j < biomes[0].length; j++) {
				ret.add(biomes[i][j]);
			}
		}
		return ret;
	}
	
	public ArrayList<Biome> getBiomesForTerrainGenerator(ITerrainGenerator tgen) {
		ArrayList<Biome> ret = new ArrayList<>();
		for(int i = 0; i < biomes.length; i++) {
			for(int j = 0; j < biomes[0].length; j++) {
				if(biomes[i][j].getTerrainGenerator() == tgen) {
					if(!ret.contains(biomes[i][j])) {
						ret.add(biomes[i][j]);
					}
				}
			}
		}
		return ret;
	}
	
	public ArrayList<Biome> getBiomesForTerrainGeneratorIdentifier(String tgen) {
		ArrayList<Biome> ret = new ArrayList<>();
		for(int i = 0; i < biomes.length; i++) {
			for(int j = 0; j < biomes[0].length; j++) {
				if(biomes[i][j].getTerrainGenerator().identify().equals(tgen)) {
					if(!ret.contains(biomes[i][j])) {
						ret.add(biomes[i][j]);
					}
				}
			}
		}
		return ret;
	}
	
	public Mask getMaskForBiomes(ArrayList<Biome> biomes) {
		Mask ret = new Mask(getXSize(), getYSize());
		for(int i = 0; i < biomes.size(); i++) {
			ret.or(getMaskForBiome(biomes.get(i)));
		}
		return ret;
	}
	
	public Mask getMaskForBiome(Biome b) {
		Mask ret = new Mask(getXSize(), getYSize());
		for(int i = 0; i < biomeLookUp.length; i++) {
			for(int j = 0; j < biomeLookUp[0].length; j++) {
				ret.setValueAt(i, j, biomeLookUp[i][j].equals(b));
			}
		}
		return ret;
	}
	
	public Mask getMaskForBiomeIdentifiers(ArrayList<String> biomeIds) {
		Mask ret = new Mask(getXSize(), getYSize());
		for(int i = 0; i < biomeIds.size(); i++) {
			ret.or(getMaskForBiomeIdentifier(biomeIds.get(i)));
		}
		return ret;
	}
	
	public Mask getMaskForBiomeIdentifier(String s) {
		Mask ret = new Mask(getXSize(), getYSize());
		for(int i = 0; i < biomeLookUp.length; i++) {
			for(int j = 0; j < biomeLookUp[0].length; j++) {
				ret.setValueAt(i, j, biomeLookUp[i][j].getTerrainGenerator().identify().equals(s));
			}
		}
		return ret;
	}
	
	public Biome getBiomeAt(int x, int y) {
		return biomeLookUp[x][y];
	}
	
	@Override
	public Color getColorAt(int x, int y) {
		if(x % 4 == 0 && biomeLookUp[x][y].isHillBiome()) return Color.BLACK;
		return biomeLookUp[x][y].getBiomePreviewColor();
	}

	@Override
	public int getXSize() {
		return biomeLookUp.length;
	}

	@Override
	public int getYSize() {
		return biomeLookUp[0].length;
	}

}
