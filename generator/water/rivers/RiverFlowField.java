package generator.water.rivers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;

import data.biomes.Biome;
import data.map.HeightMap;
import data.map.Mask;
import io.ImageIO;

public class RiverFlowField {
	private short[][][] cellPos;
	private DrainageCell[][] cells;
	private Random rng;
	private int cellCount = 128;
	private int cellSizeX;
	private int cellSizeY;
	
	private float springChance = 0.4f;
	private float tolerance = 0.05f;
	private float randomSpringChance = 0.1f;
	private float mountainRiverChance= 0.5f;
	
	private float startHeight;
	private float endHeight;
	
	
	public RiverFlowField(int xSize, int ySize, int cellCount, float springChance, float tolerance, float randomSpringChance, float mountainRiverChance) {
		this.cellCount = cellCount;
		this.springChance = springChance;
		this.tolerance = tolerance;
		this.randomSpringChance = randomSpringChance;
		this.mountainRiverChance = mountainRiverChance;
		
		cellPos = new short[xSize][ySize][2];
	}
	
	public void init(HeightMap h, long seed, float startHeight, float endHeight) {
		rng = new Random(seed);
		cells = new DrainageCell[cellCount][cellCount];
		cellSizeX = h.getXSize() / cellCount;
		cellSizeY = h.getYSize() / cellCount;
		for(int i = 0; i < cellCount; i++) {
			for(int j = 0; j < cellCount; j++) {
				int x = rng.nextInt(cellSizeX);
				int y = rng.nextInt(cellSizeY);
				
				x += i * cellSizeX;
				y += j * cellSizeY;
				
				DrainageCell d = new DrainageCell(x, y, h.getValueAt(x, y), seed);
				cells[i][j] = d;
			}
		}
		
		this.startHeight = startHeight;
		this.endHeight = endHeight;
		
		
		for(int x = 0; x < h.getXSize(); x++) {
			for(int y = 0; y < h.getYSize(); y++) {
				int minDistSquared = Integer.MAX_VALUE;
				short cellX = (short) (x / cellSizeX);
				short cellY = (short) (y / cellSizeY);
				
				short minCellIndexX = -1;
				short minCellIndexY = -1;
				for(short i = -1; i < 2; i++) {
					for(short j = -1; j < 2; j++) {
						DrainageCell dc = null;
						try {
							dc = cells[i + cellX][j + cellY];
						} catch(Exception e) {
							
						}
						if(dc != null) {
							int xo = dc.getxCenter() - x;
							int yo = dc.getyCenter() - y;
							int distSquared = xo * xo + yo * yo;
							if(distSquared < minDistSquared) {
								minDistSquared = distSquared;
								minCellIndexX = (short) (i + cellX);
								minCellIndexY = (short) (j + cellY);
							}
						}
					}
				}
				
				cells[minCellIndexX][minCellIndexY].updateTo(h.getValueAt(x, y));
				short[] coords = {minCellIndexX, minCellIndexY};
				cellPos[x][y] = coords;
			}
		}
		
		for(int i = 0; i < cellCount; i++) {
			for(int j = 0; j < cellCount; j++) {
				DrainageCell d = cells[i][j];
				HashSet<DrainageCell> nbc = new HashSet<>();
				d.setNeighbours(nbc);
				for(byte o = -1; o < 2; o++) {
					for(byte p = -1; p < 2; p++) {
						try {
							if(!(o == 0 && p == 0) && neighbours(d, cells[i + o][j + p])) nbc.add(cells[i + o][j + p]);
						} catch(Exception e) {
							
						}
					}
				}
				//System.out.println(i + "/" + j + "  " + d.getNeighbours());
			}
		}
		
		HashSet<DrainageCell> deltas = new HashSet<>();
		for(int i = 0; i < cellCount * cellCount; i++) {
			//int x = rng.nextInt(cells.length - 4) + 2;
			//int y = rng.nextInt(cells[0].length - 4) + 2;
			
			
		}
		
		for(int x = 2; x < cellCount - 2; x++) {
			for(int y = 2; y < cellCount - 2; y++) {
				HashSet<DrainageCell> nbs = cells[x][y].getNeighbours();
				Iterator it = nbs.iterator();
				while(it.hasNext()) {
					DrainageCell dc = (DrainageCell) it.next();
					if(dc.getHeight() < endHeight && cells[x][y].getHeight() > endHeight) {
						cells[x][y].setMapColor(Color.BLACK);
						cells[x][y].setOutflow(dc);
						deltas.add(cells[x][y]);
						break;
					}
				}
			}
		}
		
		for(int x = 0; x < cellCount; x++) {
			for(int y = 0; y < cellCount; y++) {
				if((x != 0 && x < cellCount - 1) && (y != 0 && y < cellCount - 1)) continue;
				if(rng.nextFloat() < 0.1f) {
					int nx = (x != 0 && x < cellCount - 1) ? x : -1;
					int ny = (y != 0 && y < cellCount - 1) ? y : -1;
					if(nx == -1) nx = (x == 0) ? 0 : cellCount; 
					if(ny == -1) ny = (y == 0) ? 0 : cellCount; 
					nx *= cellSizeX;
					ny *= cellSizeY;
					DrainageCell dc = new DrainageCell(nx, ny, 0.4f, seed);
					cells[x][y].setOutflow(dc);
					cells[x][y].setMapColor(Color.BLACK);
					deltas.add(cells[x][y]);
				}
			}
		}
		
		//createNetwork(deltas);
		//downwards();
		
		relLocalMin(deltas);
		enableFlow(deltas);
		
		//straighten();
		//createLakes();
		//seperateStreams();
		
		ImageIO.saveImage(asImage(cellSizeX, cellSizeY, h.getXSize(), h.getYSize()), "Flusszellen");
	}

	public RiverInfo toRiverInfo(float minFlow) {
		RiverInfo rinf = new RiverInfo(cellPos.length, cellPos[0].length);
		rinf.setMinFlow(minFlow);
		HashSet<River> rivers = toRiverSet(minFlow);
		long time = System.currentTimeMillis();
		RiverUtils.fillHoles(rivers);
		System.out.println("filled Holes " + (System.currentTimeMillis() - time) + "ms");
		rinf.setRivers(rivers);
		rinf.updateMask();
		return rinf;
	}
	
	public HashSet<River> toRiverSet(float minFlow) {
		River[][] riverlookUp = new River[cells.length][cells[0].length];
		HashSet<River> rivers = new HashSet<>(cells.length * cells[0].length);
		for(int cellX = 0; cellX < cells.length; cellX++) {
			for(int cellY = 0; cellY < cells.length; cellY++) {
				if(cells[cellX][cellY].getFlow() >= minFlow && cells[cellX][cellY].getOutflow() != null && cells[cellX][cellY].getOutflow() != cells[cellX][cellY]) {
					ArrayList<int[]> rp = new ArrayList<>();
					int[] s = {cells[cellX][cellY].getxCenter(), cells[cellX][cellY].getyCenter()};
					rp.add(s);
					int[] e = {cells[cellX][cellY].getOutflow().getxCenter(), cells[cellX][cellY].getOutflow().getyCenter()};
					rp.add(e);
					River r = new River(rp, cellPos.length, cellPos[0].length, cells[cellX][cellY].getFlow());
					if(cells[cellX][cellY].isSpringCell()) r.markAsSource();
					rivers.add(r);
					riverlookUp[cellX][cellY] = r;
				}
			}
		}
		for(int cellX = 0; cellX < cells.length; cellX++) {
			for(int cellY = 0; cellY < cells.length; cellY++) {
				if(riverlookUp[cellX][cellY] != null) {
					DrainageCell dc = cells[cellX][cellY].getOutflow();
					River c = null;
					try {
					     c = riverlookUp[dc.getxCenter() / cellSizeX][dc.getyCenter() / cellSizeY];
					} catch(Exception e) {}
					riverlookUp[cellX][cellY].setChild(c);
				}
			}
		}
		for(int cellX = 0; cellX < cells.length; cellX++) {
			for(int cellY = 0; cellY < cells.length; cellY++) {
				if(riverlookUp[cellX][cellY] != null && riverlookUp[cellX][cellY].getChild() == null) {
					riverlookUp[cellX][cellY].markDelta();
				}
			}
		}
		return rivers;
	}
	
	public boolean neighbours(DrainageCell dc1, DrainageCell dc2) {
		int[] v = {dc2.getxCenter() - dc1.getxCenter(), dc2.getyCenter() - dc1.getyCenter()};
		float lv = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1]);
		float[] nv = {v[0] / lv, v[1] / lv};
		for(int i = 0; i < lv; i++) {
			float[] np = {i * nv[0] + dc1.getxCenter(), i * nv[1] + dc1.getyCenter()};
			int[] c = {(int) np[0], (int) np[1]};
			if(cells[cellPos[c[0]][c[1]][0]][cellPos[c[0]][c[1]][1]] == dc1) {
				continue;
			} else if(cells[cellPos[c[0]][c[1]][0]][cellPos[c[0]][c[1]][1]] == dc2) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	
	public void createLakes() {
		for(int t = 0; t < 4; t++) {
			for(int i = 0; i < cells.length; i++) {
				for(int j = 0; j < cells[0].length; j++) {
					DrainageCell dc = cells[i][j];
					if(dc.getOutflow() == null && dc.getFlow() > 0f && dc.getHeight() > endHeight) {
						dc.setLake(true);
					}
					if(dc.getOutflow() == null && dc.getFlow() > 18f && dc.getHeight() > endHeight) {
						dc.setLake(true);
						dc.addFlow(2f - dc.getFlow());
						Iterator it = dc.getNeighbours().iterator();
						while(it.hasNext()) {
							DrainageCell nbdc = (DrainageCell) it.next();
							nbdc.setLake(true);
							nbdc.addFlow(2f);
							nbdc.setOutflow(null);
						}
					}
				}
			}
		}
	}
	
	
	public void relLocalMin(HashSet<DrainageCell> deltas) {
		System.out.println("Starting River Gen");
		ArrayList<DrainageCell> localMins = new ArrayList<>();
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				DrainageCell dc = cells[i][j];
				if(dc.getHeight() < endHeight) continue;
				Iterator it = dc.getNeighbours().iterator();
				DrainageCell min = null;
				float minH = dc.getHeight();
				while(it.hasNext()) {
					DrainageCell nbdc = (DrainageCell) it.next();
					float h = nbdc.getHeight();
					if(h < minH) {
						minH = h;
						min = nbdc;
					}
				}
				if(!deltas.contains(dc)) dc.setOutflow(min);
				if(min == null) {
					localMins.add(dc);
				}
			}
		}
		
		boolean[][] hasOutlet = new boolean[cells.length][cells[0].length];
		ArrayList<DrainageCell> todoDcs = new ArrayList<>();
		ArrayList<DrainageCell> newTodoDcs = new ArrayList<>();
		
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				DrainageCell dc = cells[i][j];
				if(dc.getHeight() < endHeight || deltas.contains(dc)) {
					hasOutlet[i][j] = true;
					todoDcs.add(dc);
				}
				
			}
		}
		
		while(!todoDcs.isEmpty()) {
			Iterator it = todoDcs.iterator();
			while(it.hasNext()) {
				DrainageCell dc = (DrainageCell) it.next();
				Iterator ite = dc.getNeighbours().iterator();
				while(ite.hasNext()) {
					DrainageCell nbdc = (DrainageCell) ite.next();
					if(nbdc.getOutflow() == dc) {
						newTodoDcs.add(nbdc);
						hasOutlet[cellPos[dc.getxCenter()][dc.getyCenter()][0]][cellPos[dc.getxCenter()][dc.getyCenter()][1]] = true;
					}
				}
			}
			todoDcs = newTodoDcs;
			newTodoDcs = new ArrayList<>();
		}
		
		System.out.println("Starting Waycostcalculation for " + localMins.size() + " local minima");
		float distancePenalty = 0.001f;
		
		Iterator it = localMins.iterator();
		while(it.hasNext()) {
			DrainageCell dc = (DrainageCell) it.next();
			
			todoDcs = new ArrayList<>();
			newTodoDcs = new ArrayList<>();
			
			ArrayList<DrainageCell>[][] bestpaths = new ArrayList[cells.length][cells[0].length];
			float[][] wayCosts = new float[cells.length][cells[0].length];
			for(int i = 0; i < cells.length; i++) {
				for(int j = 0; j < cells[0].length; j++) {
					wayCosts[i][j] = cells.length * cells[0].length; // just some big number so the min works
					bestpaths[i][j] = new ArrayList<>();
				}
			}
			int xPos = cellPos[dc.getxCenter()][dc.getyCenter()][0];
			int yPos = cellPos[dc.getxCenter()][dc.getyCenter()][1];
			
			wayCosts[xPos][yPos] = 0f;
			bestpaths[xPos][yPos].add(dc);
			Iterator iter = dc.getNeighbours().iterator();
			while(iter.hasNext()) {
				DrainageCell nbdc = (DrainageCell) iter.next();
				
				int nXp = cellPos[nbdc.getxCenter()][nbdc.getyCenter()][0];
				int nYp = cellPos[nbdc.getxCenter()][nbdc.getyCenter()][1];
				
				wayCosts[nXp][nYp] = nbdc.getHeight();
				
				bestpaths[nXp][nYp] = new ArrayList<>();
				bestpaths[nXp][nYp].addAll(bestpaths[xPos][yPos]);
				bestpaths[nXp][nYp].add(nbdc);
				
				todoDcs.add(nbdc);
			}
			
			int limiter = 24;
			while(!todoDcs.isEmpty()) {
				Iterator ite = todoDcs.iterator();
				while(ite.hasNext()) {
					DrainageCell drc = (DrainageCell) ite.next();
					xPos = cellPos[drc.getxCenter()][drc.getyCenter()][0];
					yPos = cellPos[drc.getxCenter()][drc.getyCenter()][1];
					
					Iterator itera = drc.getNeighbours().iterator();
					while(itera.hasNext()) {
						DrainageCell nbdc = (DrainageCell) itera.next();
						
						int nXp = cellPos[nbdc.getxCenter()][nbdc.getyCenter()][0];
						int nYp = cellPos[nbdc.getxCenter()][nbdc.getyCenter()][1];
						
						float newCost = Math.max(wayCosts[xPos][yPos], nbdc.getHeight()) + distancePenalty;
						
						if(wayCosts[nXp][nYp] > newCost) {
							wayCosts[nXp][nYp] = newCost;
							
							bestpaths[nXp][nYp] = new ArrayList<>();
							bestpaths[nXp][nYp].addAll(bestpaths[xPos][yPos]);
							bestpaths[nXp][nYp].add(nbdc);
							
						}
						if(wayCosts[nXp][nYp] > wayCosts[xPos][yPos]) {
							newTodoDcs.add(nbdc);
						}
					}
				}
				todoDcs = newTodoDcs;
				newTodoDcs = new ArrayList<>();
				if(limiter <= 0) break;
				limiter--;
			}
			
			DrainageCell minDc = null;
			float minDcH = 1000000f;
			int minPX = -1;
			int minPY = -1;
			for(int i = 0; i < cells.length; i++) {
				for(int j = 0; j < cells[0].length; j++) {
					if(hasOutlet[i][j] && wayCosts[i][j] < minDcH) {
						minDcH = wayCosts[i][j];
						minDc = cells[i][j];
						minPX = i;
						minPY = j;
					}
				}
			}
			
			ArrayList<DrainageCell> optPath = bestpaths[minPX][minPY];
			for(int t = 0; t < optPath.size() - 1; t++) {
				DrainageCell nbdc = optPath.get(t);
				nbdc.setOutflow(optPath.get(t + 1));
				int nXp = cellPos[nbdc.getxCenter()][nbdc.getyCenter()][0];
				int nYp = cellPos[nbdc.getxCenter()][nbdc.getyCenter()][1];
				hasOutlet[nXp][nYp] = true;
			}
			System.out.print("~");
		}
		System.out.println();
	}
	
	public void downwards() {
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				DrainageCell dc = cells[i][j];
				boolean success = false;
				if(dc.getHeight() > startHeight && dc.getOutflow() == null) {
					HashSet<DrainageCell> dcnbs = dc.getNeighbours();
					boolean temp = false;
					Iterator it = dcnbs.iterator();
					while(it.hasNext()) {
						DrainageCell dcnb = (DrainageCell) it.next();
						if(dcnb.getHeight() < startHeight) {
							temp = true;
							break;
						}
					}
					if(!temp) continue;
					DrainageCell ndc = dc;
					while(ndc.getHeight() > endHeight) {
						HashSet<DrainageCell> nbs = ndc.getNeighbours();
						Iterator nbIt = nbs.iterator();
						float minHeight = 1F;
						DrainageCell minCell = null;
						while(nbIt.hasNext()) {
							DrainageCell nbdc = (DrainageCell) nbIt.next();
							if(nbdc.getHeight() < minHeight) {
								minHeight = nbdc.getHeight();
								minCell = nbdc;
							}
						}
						ndc.setOutflow(minCell);
						if(minCell.getHeight() < endHeight) {
							success = true;
							break;
						}
						if(minCell.getOutflow() != null) {
							if(minCell.getOutflow() != ndc) {
								success = true;
								break;
							}
							success = false;
							break;
						}
						ndc = minCell;
					}
					if(success && getRainfallAt(i, j) > Biome.MODERATE[1]) {
						dc.addFlow(getRainfallAt(i, j));
						dc.setSpringCell(true);
					}
				}
			}
		}
	}
	
	public void createNetwork(HashSet<DrainageCell> riverDeltaCells) {
		HashSet<DrainageCell> newList = new HashSet<>();
		while(!riverDeltaCells.isEmpty()) {
			Iterator it = riverDeltaCells.iterator();
			while(it.hasNext()) {
				DrainageCell dc = (DrainageCell) it.next();
				HashSet<DrainageCell> nbs = dc.getNeighbours();
				Iterator nbIt = nbs.iterator();
				while(nbIt.hasNext()) {
					DrainageCell nbdc = (DrainageCell) nbIt.next();
					if(nbdc.getOutflow() == null && nbdc.getHeight() > endHeight && nbdc.getHeight() > dc.getHeight() - tolerance) {
						nbdc.setOutflow(dc);
						if(nbdc.getHeight() > endHeight && (nbdc.getHeight() < startHeight || rng.nextFloat() < mountainRiverChance)) newList.add(nbdc);
					}
				}
			}
			riverDeltaCells = newList;
			newList = new HashSet<>();
		}
	}
	
	public void enableFlow(HashSet<DrainageCell> deltas) {
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				if(cells[i][j].getHeight() > startHeight && cells[i][j].getOutflow() != null && rng.nextFloat() < springChance) {
					cells[i][j].addFlow(getRainfallAt(i, j));
					cells[i][j].setSpringCell(true);
				}
				Iterator it = cells[i][j].getNeighbours().iterator();
				int fnb = 0;
				while(it.hasNext()) {
					DrainageCell dc = (DrainageCell) it.next();
					if(dc.getFlow() > 0 || dc.getHeight() < endHeight) {
						fnb++;
					}
				}
				if(rng.nextFloat() < randomSpringChance && cells[i][j].getHeight() > endHeight && cells[i][j].getHeight() < startHeight && cleanNbs(i, j, 3, deltas) && fnb == 0 && getRainfallAt(i, j) > Biome.MODERATE[1]) {
					cells[i][j].addFlow(getRainfallAt(i, j));
					cells[i][j].setMapColor(Color.CYAN);
					cells[i][j].setSpringCell(true);
				}
			}
		}
	}
	
	public boolean cleanNbs(int x, int y, int radius, HashSet<DrainageCell> deltas) {
		boolean ret = true;
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				if(Math.abs(i - x) + Math.abs(j - y) < radius) {
					ret &= cells[i][j].getFlow() == 0 && !deltas.contains(cells[i][j]);
				}
			}
		}
		return ret;
	}
	
	public void shortCuts() {
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				if(cells[i][j].getHeight() > startHeight && cells[i][j].getOutflow() != null) {
					cells[i][j].addFlow(getRainfallAt(i, j));
				}
				Iterator it = cells[i][j].getNeighbours().iterator();
				int fnb = 0;
				while(it.hasNext()) {
					DrainageCell dc = (DrainageCell) it.next();
					if(dc.getFlow() > 0 || dc.getHeight() < endHeight) {
						fnb++;
					}
				}
				if(rng.nextFloat() < 0.1F && cells[i][j].getHeight() > endHeight && cells[i][j].getHeight() < startHeight && cells[i][j].getFlow() == 0 && fnb == 0) {
					cells[i][j].addFlow(getRainfallAt(i, j));
					cells[i][j].setMapColor(Color.CYAN);
				}
			}
		}
	}
	
	public void seperateStreams() {
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				DrainageCell dc = cells[i][j];
				if(dc.getFlow() < 1 && dc.getHeight() > endHeight && dc.getHeight() < startHeight) {
					HashSet<DrainageCell> nbs = dc.getNeighbours();
					int flc = 0;
					Iterator it = nbs.iterator();
					while(it.hasNext()) {
						DrainageCell dcnb = (DrainageCell) it.next();
						if(dcnb.getFlow() >= 1 || dcnb.getHeight() < endHeight) flc++;
					}
					if(flc >= 2) {
						dc.setMapColor(Color.YELLOW);
					}
				}
			}
		}
	}
	
	public void straighten() {
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[0].length; j++) {
				DrainageCell dc = cells[i][j];
				if(dc.getOutflow() == null) continue;
				ArrayList<DrainageCell> visited = new ArrayList<>();
				visited.add(dc);
				while(dc.getOutflow() != null) {
					if(visited.contains(dc.getOutflow())) {
						dc.setOutflow(null);
						break;
					}
					dc = dc.getOutflow();
					visited.add(dc);
				}
			}
		}
	}
	
	public float getRainfallAt(int x, int y) {
		int rx = cellSizeX * x + rng.nextInt(cellSizeX);
		int ry = cellSizeY * y + rng.nextInt(cellSizeY);
		
		rx = Math.min(Math.max(rx, 0), cells.length);
		ry = Math.min(Math.max(ry, 0), cells[0].length);
		if(RiverLayoutGenerator.getRainfall() == null) return 1;
		float rf =RiverLayoutGenerator.getRainfall().getValueAt(rx, ry);
		if(rf < Biome.DRY[1]) return 0f; //SHOULD BE ZERO
		if(rf < Biome.MODERATE[1]) return 1;
		if(rf < Biome.WET[1]) return 2;
		return 0;
	}
	
	public BufferedImage asImage(int cellSizeX, int cellSizeY, int xSize, int ySize) {
		BufferedImage img = new BufferedImage(cellPos.length, cellPos[0].length, BufferedImage.TYPE_INT_RGB);
		BufferedImage riveronly = new BufferedImage(cellPos.length, cellPos[0].length, BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = img.getGraphics();
		for(int i = 0; i < cellPos.length; i++) {
			for(int j = 0; j < cellPos[i].length; j++) {
				g.setColor(cells[cellPos[i][j][0]][cellPos[i][j][1]].getMapColor());
				if(cells[cellPos[i][j][0]][cellPos[i][j][1]].isSpringCell()) g.setColor(Color.BLUE);
				if(cells[cellPos[i][j][0]][cellPos[i][j][1]].isLake()) g.setColor(new Color(100, 160, 255));
				g.fillRect(i, j, 1, 1);
			}
		}
		
		g.setColor(Color.BLACK);
		Graphics r = riveronly.getGraphics();
		r.setColor(Color.WHITE);
		
		for(int i = 0; i < cells.length; i++) {
			for(int j = 0; j < cells[i].length; j++) {
				DrainageCell d = cells[i][j];
				//System.out.println(i + "/" + j + "  " + d.getNeighbours());
				HashSet<DrainageCell> nbs = d.getNeighbours();
				int x = d.getxCenter();
				int y = d.getyCenter();
				g.fillRect(x, y, 1, 1);
				
				//Iterator it = nbs.iterator();
				//while(it.hasNext()) {
					//DrainageCell nb = (DrainageCell) it.next();
					//g.drawLine(x, y, nb.getxCenter(), nb.getyCenter());
				//}
				
				if(d.getOutflow() != null && d.getFlow() >= 1) {
					g.drawLine(x, y, d.getOutflow().getxCenter(), d.getOutflow().getyCenter());
					r.drawLine(x, y, d.getOutflow().getxCenter(), d.getOutflow().getyCenter());
				}
			}
		}
		
		for(int i = 0; i < cells.length; i++) {
			//g.drawLine(i * cellSizeX, 0, i*cellSizeX, 1023);
		}
		
		for(int i = 0; i < cells.length; i++) {
			//g.drawLine(0, i*cellSizeY, 1023, i*cellSizeY);
		}
		ImageIO.saveImage(riveronly, "riverOnlyCells");
		
		return img;
	}
}
