package generator.erosion;

import java.util.Random;

import data.WorldSettings;
import data.map.HeightMap;
import data.util.MapUtils;
import io.ImageIO;

public class GridBasedErosion {
	
	public static HeightMap erode(HeightMap h, int iterations) {
		int xS = h.getXSize();
		int yS = h.getYSize();
		
		Random rng = new Random(WorldSettings.seed + 13);
		
		float[][] waterHeight = new float[xS][yS];
		float[][] tempHeights = new float[xS][yS];
		float[][] tempWater = new float[xS][yS];
		float[][] sedAmount = new float[xS][yS];
		float[][] tempSedAm = new float[xS][yS];
		
		System.out.println();
		for(int t = 0; t < iterations; t++) {
			System.out.print("|");
		}
		System.out.println();
		
		tempHeights = h.getContent().clone();
		
		
		
		for(int t = 0; t < iterations; t++) {
			for(int i = 0; i < xS; i++) {
				for(int j = 0; j < yS; j++) {
					waterHeight[i][j] = 0.001f; // 0.004f = 1 block
					tempHeights[i][j] -= waterHeight[i][j];
					sedAmount[i][j] += waterHeight[i][j];
				}
			}
			int lifetime = xS / 1;
			for(int p = 0; p < lifetime; p++) {
				for(int i = 0; i < xS; i++) {
					for(int j = 0; j < yS; j++) {
						if(waterHeight[i][j] == 0f) continue;
						
						float c = tempHeights[i][j] + waterHeight[i][j];
						float u = j != 0 ? tempHeights[i][j - 1] + waterHeight[i][j - 1] : 10f;
						float r = i != xS - 1 ? tempHeights[i + 1][j] + waterHeight[i + 1][j] : 10f;
						float d = j != yS - 1 ? tempHeights[i][j + 1] + waterHeight[i][j + 1] : 10f;
						float l = i != 0 ? tempHeights[i - 1][j] + waterHeight[i - 1][j] : 10f;
						
						u = Math.max(c - u, 0f);
						r = Math.max(c - r, 0f);
						d = Math.max(c - d, 0f);
						l = Math.max(c - l, 0f);
						
						float biggest = Math.max(Math.max(u, r), Math.max(d, l));
						
						c = -biggest * 0.5f;
						c = Math.max(c, -waterHeight[i][j]);
						
						u = biggest == u ? -c : 0f;
						r = biggest == r ? -c : 0f;
						d = biggest == d ? -c : 0f;
						l = biggest == l ? -c : 0f;
						
						
						
						tempWater[i][j] += c;
						if(j != 0) tempWater[i][j - 1] += u;
						if(i != xS - 1) tempWater[i + 1][j] += r;
						if(j != yS - 1) tempWater[i][j + 1] += d;
						if(i != 0) tempWater[i - 1][j] += l;
						
						float s = sedAmount[i][j];
						if(s < 0f) System.out.println("38uq08tq8  " + sedAmount[i][j] + "   " + waterHeight[i][j]);
						s = Math.min(c, s);
						
						float th = 0f;
						th = u != 0 ? tempHeights[i][j - 1] : th;
						th = r != 0 ? tempHeights[i + 1][j] : th;
						th = d != 0 ? tempHeights[i][j + 1] : th;
						th = l != 0 ? tempHeights[i - 1][j] : th;
						
						th = Math.max(tempHeights[i][j] - th, 0f);
						
						s = Math.min(th, s);
						s = -s;
						
						u = u != 0 ? -s : 0f;
						r = r != 0 ? -s : 0f;
						d = d != 0 ? -s : 0f;
						l = l != 0 ? -s : 0f;
						
						tempSedAm[i][j] += s;
						if(j != 0) tempSedAm[i][j - 1] += u;
						if(i != xS - 1) tempSedAm[i + 1][j] += r;
						if(j != yS - 1) tempSedAm[i][j + 1] += d;
						if(i != 0) tempSedAm[i - 1][j] += l;
						
					}
					
				}
				/*
				float deltaWaterMin = 0f;
				float deltaWaterPos = 0f;
				float deltaSedMin = 0f;
				float deltaSedPos = 0f;
				
				for(int i = 0; i < xS; i++) {
					for(int j = 0; j < yS; j++) {
						//System.out.println(tempSedAm[i][j] + " / " + tempWater[i][j]);
						if(tempWater[i][j] > 0f) {
							deltaWaterPos += tempWater[i][j];
						} else {
							deltaWaterMin += tempWater[i][j];
						}
						if(tempSedAm[i][j] > 0f) {
							deltaSedPos += tempSedAm[i][j];
						} else {
							deltaSedMin += tempSedAm[i][j];
						}
					}
				}
				System.out.println(deltaSedMin + " + " + deltaSedPos + " / " + deltaWaterMin + " + " + deltaWaterPos);
				if(deltaWaterMin + deltaWaterPos > 1f) {
					float a = -deltaWaterMin / deltaWaterPos;
					for(int i = 0; i < xS; i++) {
						for(int j = 0; j < yS; j++) {
							if(tempWater[i][j] > 0) tempWater[i][j] *= a;
						}
					}
				} else if(deltaWaterMin + deltaWaterPos < 1f) {
					float a = -deltaWaterPos / deltaWaterMin;
					for(int i = 0; i < xS; i++) {
						for(int j = 0; j < yS; j++) {
							if(tempWater[i][j] < 0) tempWater[i][j] *= a;
						}
					}
				}
				
				if(deltaSedMin + deltaSedPos > 1f) {
					float a = -deltaSedMin / deltaSedPos;
					for(int i = 0; i < xS; i++) {
						for(int j = 0; j < yS; j++) {
							if(tempSedAm[i][j] > 0) tempSedAm[i][j] *= a;
						}
					}
				} else if(deltaSedMin + deltaSedPos < 1f) {
					float a = -deltaSedPos / deltaSedMin;
					for(int i = 0; i < xS; i++) {
						for(int j = 0; j < yS; j++) {
							if(tempSedAm[i][j] < 0) tempSedAm[i][j] *= a;
						}
					}
				}
				*/
				
			}
			
			
			for(int i = 0; i < xS; i++) {
				for(int j = 0; j < yS; j++) {
					waterHeight[i][j] += tempWater[i][j];
					waterHeight[i][j] = Math.max(waterHeight[i][j], 0f);
					sedAmount[i][j] += tempSedAm[i][j];
					sedAmount[i][j] = Math.max(sedAmount[i][j], 0f);
					tempHeights[i][j] += Math.max(sedAmount[i][j] - waterHeight[i][j], 0f);
					tempHeights[i][j] = Math.min(tempHeights[i][j], 1f);
					sedAmount[i][j] = Math.min(sedAmount[i][j], waterHeight[i][j]);
					
				}
			}
			//ImageIO.saveAsGreyImage(MapUtils.normalize(new HeightMap(tempWater.clone())), "wat ");
			tempWater = new float[xS][yS];
			tempSedAm = new float[xS][yS];
			
			for(int i = 0; i < xS; i++) {
				for(int j = 0; j < yS; j++) {
					tempHeights[i][j] += Math.max(sedAmount[i][j], 0f);
				}
			}
			
			System.out.print("|");
		}
		
		System.out.println();
		return new HeightMap(tempHeights);
	}

}
