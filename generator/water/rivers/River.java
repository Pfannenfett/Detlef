package generator.water.rivers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import data.map.Mask;

public class River {
	private float size;
	private int sx, sy, ex, ey;
	private ArrayList<int[]> riverPoints;
	private River child;
	private boolean isFirst = false;
	
	public River(float size, int sx, int sy, int ex, int ey) {
		this.size = size;
		this.ex = ex;
		this.ey = ey;
		this.sx = sx;
		this.ey = ey;
	}
	
	public River(ArrayList<int[]> rPoints, int xSize, int ySize, float size) {
		this.size = size;
		this.ex = rPoints.get(rPoints.size() - 1)[0];
		this.ey = rPoints.get(rPoints.size() - 1)[1];
		this.sx = rPoints.get(0)[0];
		this.sy = rPoints.get(0)[1];
		riverPoints = rPoints;
	}
	
	public static Mask toRiverMask(ArrayList<int[]> points, int xSize, int ySize) {
	//	System.out.println("Mask size " + xSize + "/" + ySize);
		Mask ret = new Mask(xSize, ySize);
		Iterator i = points.iterator();
		while(i.hasNext()) {
			int[] c = (int[]) i.next();
			ret.setValueAt(c[0], c[1], true);
		}
		return ret;
	}
	
	public River split(int x, int y, float addFlow) {
		int index = 0;
		River ret;
		for(int i = 0; i < riverPoints.size(); i++) {
			int[] t = riverPoints.get(i);
			if(t[0] == x && t[1] == y) {
				index = i;
			}
		}
		if(index == 0) {
			this.addFlow(addFlow);
			ret = this;
		} else {
			List<int[]> l = riverPoints.subList(index, riverPoints.size());
			ArrayList<int[]> newList = new ArrayList<>();
			newList.addAll(l);
			ret = new River(size, x, y, ex, ey);
			ret.setRiverpoints(newList);
			ex = x;
			ey = y;
			l = riverPoints.subList(0, index);
			riverPoints = new ArrayList<>();
			riverPoints.addAll(l);
			ret.setChild(child);
			ret.addFlow(addFlow);
			child = ret;
		}
		return ret;
	}
	
	public void addFlow(float flow) {
		addFlow(flow, new ArrayList<River>());
	}
	
	private void addFlow(float flow, ArrayList<River> addedAlready) {
		if(addedAlready.contains(this)) return;
		size += flow;
		addedAlready.add(this);
		if(child == null) return;
		child.addFlow(flow, addedAlready);
	}
	
	public void setChild(River child) {
		this.child = child;
		
	}
	
	public River getChild() {
		return child;
	}
	
	public void setRiverpoints(ArrayList<int[]> riverpoints) {
		this.riverPoints = riverpoints;
		int[] t = riverPoints.get(0);
		sx = t[0];
		sy = t[1];
		t = riverPoints.get(riverpoints.size() - 1);
		ex = t[0];
		ey = t[0];
	}
	
	public ArrayList<int[]> getRiverpoints() {
		return riverPoints;
	}
	
	public float getSize() {
		return size;
	}
	
	public int indexOf(int x, int y) {
		for(int i = 0; i < riverPoints.size(); i++) {
			int[] p = riverPoints.get(i);
			if(p[0] == x && p[1] == y) return i;
		}
		return -1;
	}
	
	public int[] getStartPoint() {
		int[] ret = {sx, sy};
		return ret;
	}
	
	public int[] getEndPoint() {
		int[] ret = {ex, ey};
		return ret;
	}
	
	public void markAsSource() {
		isFirst = true;
	}
	
	public boolean isSource() {
		return isFirst;
	}

	public boolean isAbove(River r) {
		return isAbove(r, new ArrayList<River>());
	}
	
	private boolean isAbove(River r, ArrayList<River> rivers) {
		if(rivers.contains(this)) {
			System.out.println("Theres a river going in a circle...");
			return false;
		}
		if(child == r) return true;
		if(child == null) return false;
		rivers.add(this);
		return child.isAbove(r, rivers);
	}
	
	public void markDelta() {
		size *= 2;
	}
	
	@Override
	public String toString() {
		return "River from " + sx + "/" + sy + " to " + ex + "/" + ey + " with length " + riverPoints.size() + " and size " + size;	
	}
}
