package generator.water.rivers;

import java.awt.Color;
import java.util.HashSet;
import java.util.Random;

public class DrainageCell {
	private HashSet<DrainageCell> neighbours;
	private DrainageCell outflow;
	private Color mapColor;
	
	private int collectedHeights = 0;
	
	private float height;
	
	private int xCenter;
	private int yCenter;
	private Random rng;
	
	private float flow = 0;
	private boolean isSpringCell = false;
	private boolean isLake = false;
	
	
	public DrainageCell(int x, int y, float height, long seed) {
		rng = new Random(seed);
		xCenter = x;
		yCenter = y;
		this.height = height;
		mapColor = createColorFromHeight(height);
	}
	
	public void addFlow(float f) {
		flow += f;
		if(outflow != null && outflow != this && outflow.outflow != this) {
			//System.out.println(xCenter + "/" + yCenter + " ---> " + outflow.xCenter + "/" + outflow.yCenter);
			outflow.addFlow(f);
		}
	}
	
	public float getFlow() {
		return flow;
	}
	
	public boolean isSpringCell() {
		return isSpringCell;
	}
	
	public void setSpringCell(boolean isSpringCell) {
		this.isSpringCell = isSpringCell;
	}
	
	public void setLake(boolean isLake) {
		this.isLake = isLake;
	}
	
	public boolean isLake() {
		return isLake;
	}
	
	public void updateTo(float h) {
		float hnew = height * collectedHeights + h;
		collectedHeights++;
		hnew /= collectedHeights;
		height = hnew;
	}
	
	
	public Color createColorFromHeight(float h) {
		return new Color(h, h, h);
	}
	
	public float getHeight() {
		return height;
	}
	
	public Color getMapColor() {
		return mapColor;
	}
	
	public int getxCenter() {
		return xCenter;
	}
	
	public int getyCenter() {
		return yCenter;
	}
	
	public DrainageCell getOutflow() {
		return outflow;
	}
	
	public HashSet<DrainageCell> getNeighbours() {
		return neighbours;
	}
	
	public void setOutflow(DrainageCell outflow) {
		this.outflow = outflow;
	}
	
	public void setNeighbours(HashSet<DrainageCell> neighbours) {
		this.neighbours = neighbours;
	}
	
	public void setMapColor(Color mapColor) {
		this.mapColor = mapColor;
	}
}
