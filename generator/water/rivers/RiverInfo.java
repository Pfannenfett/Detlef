package generator.water.rivers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import data.map.Mask;

public class RiverInfo {
	private HashSet<River> rivers;
	private Mask riverMask;
	private Mask lakeMask;
	private HashSet<int[]> startPoints;
	private HashSet<int[]> endPoints;
	
	private float[][] flowMap;
	private float minFlow;
	
	private River[][] riverLookUp;
	private float[][] waterHeight;
	
	private int xSize;
	private int ySize;
	
	public RiverInfo(int xSize, int ySize) {
		this.xSize = xSize;
		this.ySize = ySize;
		startPoints = new HashSet<>();
		endPoints = new HashSet<>();
	}
	
	public void init(HashSet<River> rivers) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			startPoints.add(r.getStartPoint());
			endPoints.add(r.getEndPoint());
		}

		this.rivers = rivers;
		riverLookUp = RiverUtils.toRiverLookUp(rivers, xSize, ySize);
		riverMask = new Mask(xSize, ySize);
		updateMask();
	}
	
	public void biggen(int factor) {
		System.out.println("biggen " + rivers.size() + " rivers");
		xSize *= factor;
		ySize *= factor;
		RiverUtils.biggenRivers(rivers, factor);
		RiverUtils.fixRiverConnection(rivers);
		startPoints = new HashSet<>();
		endPoints = new HashSet<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			startPoints.add(r.getStartPoint());
			endPoints.add(r.getEndPoint());
		}
		riverLookUp = RiverUtils.toRiverLookUp(rivers, xSize, ySize);
		riverMask = new Mask(xSize, ySize);
		updateMask();
	}
	
	public void cleanUpSmallRivers() {
		ArrayList<River> toRemove = new ArrayList<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			if(r.getSize() < minFlow) toRemove.add(r);
		}
		it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			if(toRemove.contains(r.getChild())) r.setChild(null);
		}
		rivers.removeAll(toRemove);
	}
	
	public void setMinFlow(float minFlow) {
		this.minFlow = minFlow;
	}
	
	public float getMinFlow() {
		return minFlow;
	}
	
	public float[][] createFlowMap() {
		flowMap = RiverUtils.createFlowMap(rivers, xSize, ySize);
		return flowMap;
	}
	
	public void updateMask() {
		Mask newMask = new Mask(riverMask.getXSize(), riverMask.getYSize());
		for(int i = 0; i < newMask.getXSize(); i++) {
			for(int j = 0; j < newMask.getYSize(); j++) {
				newMask.setValueAt(i, j, riverLookUp[i][j] != null && riverLookUp[i][j].getSize() > minFlow);
			}
		}
		riverMask = newMask;
	}
	
	public Mask getRiverMask(float minFlow, float maxFlow) {
		Mask newMask = new Mask(riverMask.getXSize(), riverMask.getYSize());
		for(int i = 0; i < newMask.getXSize(); i++) {
			for(int j = 0; j < newMask.getYSize(); j++) {
				newMask.setValueAt(i, j, riverLookUp[i][j] != null && riverLookUp[i][j].getSize() >= minFlow && riverLookUp[i][j].getSize() < maxFlow);
			}
		}
		return newMask;
	}
	
	public void setStartPoints(HashSet<int[]> startPoints) {
		this.startPoints = startPoints;
	}
	
	public void setLakeMask(Mask lakeMask) {
		this.lakeMask = lakeMask;
	}
	
	public Mask getLakeMask() {
		return lakeMask;
	}
	
	public void setEndPoints(HashSet<int[]> endPoints) {
		this.endPoints = endPoints;
	}
	
	
	public void setRivers(HashSet<River> rivers) {
		init(rivers);
	}
	
	public int getxSize() {
		return xSize;
	}
	
	public int getySize() {
		return ySize;
	}
	
	public HashSet<int[]> getEndPoints() {
		return endPoints;
	}
	
	public HashSet<int[]> getStartPoints() {
		return startPoints;
	}
	
	public HashSet<River> getRivers() {
		return rivers;
	}
}
