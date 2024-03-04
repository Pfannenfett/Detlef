package data.map;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class RegionMap implements IPrintableMap {
	
	private int[][] data;
	private ArrayList<int[]> translate;
	
	private RegionMap(int xSize, int ySize) {
		data = new int[xSize][ySize];
	}
	
	public static RegionMap voronoi(ArrayList<int[]> seeds, int xSize, int ySize) {
		System.out.println("  creating voronoi");
		System.out.print("  ");
		HashMap<Integer, int[]> hashSeeds = new HashMap<>();
		int s = seeds.size();
		
		for(int k = 0; k < s; k++) {
			hashSeeds.put(k, seeds.get(k));
		}
		int xt = xSize / 16;
		
		RegionMap ret = new RegionMap(xSize, ySize);
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				float minDist = Float.POSITIVE_INFINITY;
				int minIndex = -1;
				for(int k = 0; k < s; k++) {
					int[] p = hashSeeds.get(k);
					float f = (p[0] - i) * (p[0] - i) + (p[1] - j) * (p[1] - j);
					if(f < minDist) {
						minDist = Math.min(minDist, f);
						minIndex = k;
					}
					
				}
				ret.data[i][j] = minIndex;
			}
			if(i % xt == 0) System.out.print("!");
		}
		System.out.println();
		ret.translate = (ArrayList<int[]>) seeds.clone();
		return ret;
	}
	
	public int[] getValueAt(int x, int y) {
		return translate.get(data[x][y]);
	}
	
	@Override
	public Color getColorAt(int x, int y) {
		double d = data[x][y];
		d /= translate.size();
		
		return new Color((int) (d * 255 * 255 * 255));
	}

	@Override
	public int getXSize() {
		return data.length;
	}

	@Override
	public int getYSize() {
		return data[0].length;
	}

}
