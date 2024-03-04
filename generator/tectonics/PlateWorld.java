package generator.tectonics;

import java.util.HashSet;
import java.util.Iterator;

import data.map.CrustMap;
import data.util.Position;

public class PlateWorld {
	
	private Plate plate;
	private float[][] plateHeights;
	private float[][] temp;
	
	public PlateWorld(CrustMap m, Plate p) {
		plate = p;
		plateHeights = m.getPlateCompletely(p);
		for(int i = 0; i < plateHeights.length; i++) {
			for(int j = 0; j < plateHeights[i].length; j++) {
				if(plateHeights[i][j] == 0) plateHeights[i][j] = -1;
			}
		}
	}
	
	public float getHeightAt(int x, int y) {
		return plateHeights[x][y] == -1 ? 0 : plateHeights[x][y];
	}
	
	public float[][] getPlateHeights() {
		return plateHeights;
	}
	
	public void addNew(Position p) {
		plateHeights[p.getxPos()][p.getyPos()] = plate.isOceanPlate() ? 0.2F : 0.6F;
	}
	
	public void prepareMoving() {
		
		temp = new float[plateHeights.length][plateHeights[0].length];
		float f;
		//long time = System.currentTimeMillis();
		int xd = plate.getNextXStep();
		int yd = plate.getNextYStep();
		if(xd == 0 && yd == 0) {
			temp = plateHeights;
			return;
		}
		
		for(int i = 0; i < plateHeights.length; i++) {
			for(int j = 0; j < plateHeights[i].length; j++) {
				temp[i][j] = -1;
			}
		}
		
		for(int i = 0; i < plateHeights.length; i++) {
			for(int j = 0; j < plateHeights[i].length; j++) {
				f = plateHeights[i][j];
				try {
					temp[i + xd][j + yd] = f;
				} catch(ArrayIndexOutOfBoundsException r) {
					
				}
			}
		}
	}
	
	public void doMove(boolean[][] b) {
		boolean[][] posTemp = new boolean[plateHeights.length][plateHeights[0].length];
		for(int i = 0; i < plateHeights.length; i++) {
			for(int j = 0; j < plateHeights[i].length; j++) {
				posTemp[i][j] = (temp[i][j] <= 0 && plateHeights[i][j] >= 0);
			}
		}
		plateHeights = temp;
		for(int i = 0; i < plateHeights.length; i++) {
			for(int j = 0; j < plateHeights[i].length; j++) {
				if(posTemp[i][j]) plateHeights[i][j] = plate.isOceanPlate() ? 0.2F : 0.6F;
			}
		}
	}
	
	public double getSubductId() {
		return plate.getSubductId();
	}
	
	public void subduct(int x, int y, float amount) {
		plateHeights[x][y] -= amount;
		plateHeights[x][y] = Math.max(plateHeights[x][y], 0);
	}
}
