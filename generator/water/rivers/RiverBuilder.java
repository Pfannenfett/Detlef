package generator.water.rivers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import data.map.DirectionMap;
import data.map.HeightMap;
import data.map.MapBase;
import data.map.Mask;
import data.util.MapUtils;
import generator.noise.NoiseGenerator;
import io.ImageIO;

public class RiverBuilder {
	private static ArrayList<Boolean> lastFlowingXdir;
	private static Mask riverValleys;
	
	public static HeightMap finishLowRivers(RiverInfo rinf, HeightMap hm, int scaling, float lowHeight, float targetHeight, long seed) {
		Mask riverMask = new Mask(hm.getXSize(), hm.getYSize());
		float[][] flow = new float[hm.getXSize()][hm.getYSize()];
		
		
		rinf.cleanUpSmallRivers();
		
		HashSet<River> rivers = rinf.getRivers();
		RiverUtils.removeZeros(rinf.getRivers());
		RiverUtils.removeDoubles(rinf.getRivers());
		
		
		RiverUtils.saveRiverImage(rivers, hm.getXSize(), hm.getYSize(), "Rivers 1");
		
		RiverUtils.compactify(rinf.getRivers(), 24);
		RiverUtils.naturalize(rivers, hm.getXSize(), hm.getYSize());
		RiverUtils.fixChildren(rivers, hm.getXSize(), hm.getYSize());
		
		RiverUtils.saveRiverImage(rivers, hm.getXSize(), hm.getYSize(), "Rivers 2");
		
		
		RiverUtils.saveRiverImage(rivers, hm.getXSize(), hm.getYSize(), "Rivers 3");
		
		rinf.updateMask();
		
		System.out.println("THere are currently " + rivers.size() + " Rivers overall there");
		HashSet<River> bigRivers = rivers;
		System.out.println("THere are currently " + bigRivers.size() + " big Rivers there");
		
		long time = System.currentTimeMillis();
		RiverUtils.shorten(bigRivers, hm.getXSize(), hm.getYSize(), 4);
		RiverUtils.fillHoles(rinf.getRivers());
		System.out.println("snapped " + (System.currentTimeMillis() - time) + "ms");
		
		RiverUtils.fixRiverConnection(rivers);
		RiverUtils.checkRivers(rivers, hm);
		
		
		
		
		
		time = System.currentTimeMillis();
		rinf.updateMask();
		riverMask = rinf.getRiverMask(2, 200);
		flow = rinf.createFlowMap();
		
		System.out.println(flow.length);
		
		HeightMap flowMap = new HeightMap(flow);
		
		//MapUtils.normalize(flowMap);
		
		flow = flowMap.getContent();
		
		//ImageIO.saveAsGreyImage(riverMask, "rivers");
		
		
		
		HeightMap deltaRivers = createBreadthMap(flow);
		Mask t = deltaRivers.above(0.01F);
		deltaRivers = MapUtils.asHeightMap(t);
		MapUtils.smooth(deltaRivers, 16);
		ImageIO.saveAsGreyImage(deltaRivers, "Rivers before deltas");
		System.out.println("created Breadthmap " + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		
		Mask deltaMask = RiverUtils.deltaDetect(bigRivers, hm, 0.24F, 0.235f, 0.25f);
		
		System.out.println("finished delta detection " + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		
		HeightMap deltaMult = MapUtils.asHeightMap(deltaMask);
		deltaMask = null;
		MapUtils.shrink(deltaMult, 16);
		ImageIO.saveAsGreyImage(deltaMult, "deltaMask");
		ImageIO.saveAsGreyImage(riverValleys, "valleyMask");
		deltaMult.applyMask(riverValleys);
		ImageIO.saveAsGreyImage(deltaMult, "deltaMaskFinal");
		MapUtils.smooth(deltaMult, 1);
		MapUtils.smoothBiggen(deltaMult, 4);
		
		System.out.println("created deltamask " + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		
		HeightMap deltaNoise = NoiseGenerator.createPerlinNoiseMap(hm.getXSize() / 4, hm.getYSize() / 4, seed, 2, true);
		deltaNoise.add(-0.4F);
		deltaNoise.cap();
		deltaNoise.mult(2);
		MapUtils.normalize(deltaNoise);
		
		System.out.println("created deltaNoise " + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		
		deltaNoise = new HeightMap(MapUtils.cappedMultiply(deltaNoise, deltaMult));
		deltaMult = null;
		MapUtils.smoothBiggen(deltaNoise, 4);
		
		//ImageIO.saveAsGreyImage(deltaNoise, "deltas");
		
		MapUtils.addDiminMapOn(deltaRivers, deltaNoise);
		ImageIO.saveAsGreyImage(deltaRivers, "riverBeds");
		float riverDepth = 0.02F;
		targetHeight -= riverDepth;
		
		System.out.println("created Riverbedmap " + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		
		for(int i = 0; i < riverMask.getXSize(); i++) {
			for(int j = 0; j < riverMask.getYSize(); j++) {
				if(deltaRivers.getValueAt(i, j) > 0 && hm.getValueAt(i, j) < lowHeight && hm.getValueAt(i, j) > targetHeight - riverDepth) {
					float depth = hm.getValueAt(i, j) - targetHeight;
					depth *= deltaRivers.getValueAt(i, j);
					depth = hm.getValueAt(i, j) - depth;
					depth = Math.min(hm.getValueAt(i, j), depth);
					hm.setValueAt(i, j, depth);
				}
			}
		}
		return hm;
	}
	
	
	private static HeightMap createBreadthMap(float[][] flow) {
		HeightMap h = new HeightMap(flow.length, flow[0].length);
		for(int i = 0; i < flow.length; i++) {
			for(int j = 0; j < flow[0].length; j++) {
				if(flow[i][j] > 0) {
					int size = (int) flow[i][j];
					size = Math.max(size, 3);
					size = Math.min(size, 32);
					
					size = 1;
					
					float[][] brush = brush(size);
					for(int x = -size; x <= size; x++) {
						for(int y = -size; y <= size; y++) {
							float f = h.getValueAt(i + x, j + y);
							h.setValueAt(i + x, j + y, Math.max(f, 1 - brush[x + size][y+size]));
						}
					}
				}
			}
		}
		return h;
	}
	
	public static void setRiverValleys(Mask riverValleys) {
		RiverBuilder.riverValleys = riverValleys;
	}
	
	public static HeightMap carveRiverBeds(float[][] flow, HeightMap hm, float waterHeight) {
		float[][] change = new float[hm.getXSize()][hm.getYSize()];
		Mask riverMask = new HeightMap(flow).above(0.01F);
		
		ImageIO.saveAsGreyImage(riverMask, "riverCarvingMask");
		
		for(int i = 0; i < hm.getXSize(); i++) {
			for(int j = 0; j < hm.getYSize(); j++) {
				change[i][j] = 1F;
			}
		}
		
		
		for(int i = 0; i < hm.getXSize(); i++) {
			for(int j = 0; j < hm.getYSize(); j++) {
				if(riverMask.getValueAt(i, j)) {
					float s = flow[i][j];
					s = (float) Math.log(s);
					float[][] brush = brush(s);
					s = Math.max(s, 1);
					int size = (int) Math.ceil(s);
					if(size > 1) {
						for(int x = 0; x < brush.length; x++) {
							for(int y = 0; y < brush[0].length; y++) {
								float f = brush[x][y];
								try {change[i + x - size][j + y - size] = Math.min(change[i + x - size][j + y - size], Math.min(f, 1F));}
								catch(Exception e) {}
							}
						}
					}
				}
			}
		}
		
		for(int i = 0; i < hm.getXSize(); i++) {
			for(int j = 0; j < hm.getYSize(); j++) {
				float hmv = hm.getValueAt(i, j);
				hmv -= waterHeight;
				if(hmv > 0) {
					hmv *= change[i][j];
					hmv += waterHeight;
					if(change[i][j] < 0.8F) hm.setValueAt(i, j, waterHeight);
				}
			}
		}
		return hm;
	}
	
	public static HeightMap createRiverCarvingMult(float[][] flow, float minFlow, HeightMap hm, float waterHeight, float multiplier) {
		float[][] change = new float[hm.getXSize()][hm.getYSize()];
		HeightMap flowMap = new HeightMap(flow.clone());
		Mask riverMask = flowMap.above(0.01F);
		
		
		ImageIO.saveAsGreyImage(riverMask, "riverCarvingMask");
		
		
		for(int i = 0; i < hm.getXSize(); i++) {
			for(int j = 0; j < hm.getYSize(); j++) {
				change[i][j] = 1F;
			}
		}
		
		
		for(int i = 0; i < hm.getXSize(); i++) {
			for(int j = 0; j < hm.getYSize(); j++) {
				if(riverMask.getValueAt(i, j)) {
					float s = flow[i][j];
					if(s < minFlow) continue;
					s = Math.max(s, 1);
					s *= multiplier;
					s = Math.min(hm.getXSize() / 8, s);
					float[][] brush = brush(s);
					int size = (int) Math.ceil(s);
					for(int x = 0; x < brush.length; x++) {
						for(int y = 0; y < brush[0].length; y++) {
							float f = 1.2F * brush[x][y] - 0.2F;
							f = Math.max(f, 0);
							try {
								f = Math.min(change[i + x - size][j + y - size], Math.min(f, 1F));
								change[i + x - size][j + y - size] = f;
							} catch(Exception e) {}
						}
					}
				}
			}
		}
		
		for(int i = 0; i < flowMap.getXSize(); i++) {
			for(int j = 0; j < flowMap.getYSize(); j++) {
				float f = flowMap.getValueAt(i, j);
				f = (float) Math.log(f);
				f /= 10;
				f = Math.min(f, 1);
				f = Math.max(f, 0);
				flowMap.setValueAt(i, j, f);
			}
		}
		ImageIO.saveAsGreyImage(flowMap, "flow");
		
		HeightMap ret = new HeightMap(change);
		ImageIO.saveAsGreyImage(ret, "change");
		return ret;
	}
	
	
	private static float[][] brush(float s) {
		float size = 2 * s + 1;
		float[][] brush = new float[(int) Math.ceil(size)][(int) Math.ceil(size)];
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				float x = i - s;
				float y = j - s;
				float dist = (float) Math.sqrt(x * x + y * y);
				dist /= s;
				dist *= dist;
				dist = Math.max(dist, 0);
				dist = Math.min(dist, 1);
				brush[i][j] = dist;
			}
		}
		return brush;
	}
	
	private static Mask riverFlow(Mask area, Mask river, float[][] flow, float chance, long seed) {
		Random rng = new Random(seed);
		
		for(int o = 0; o < 16; o++) {
			boolean[][] change = new boolean[river.getXSize()][river.getYSize()];
			for(int i = 0; i < river.getXSize(); i++) {
				for(int j = 0; j < river.getYSize(); j++) {
					if(river.getValueAt(i, j)) {
						boolean[] nb = river.get8NB(i, j);
						for(int t = 0; t < nb.length; t++) {
							if(rng.nextFloat() < localChance(t, i, j)) {
								byte dx = 0;
								byte dy = 0;
								switch(t) {
								case 0: dy = -1; break;
								case 1: dx = 1; break;
								case 2: dy = 1; break;
								case 3: dx = -1; break;
								case 4: dx = 1;dy = -1; break;
								case 5: dx = 1;dy = 1; break;
								case 6: dx = -1;dy = 1; break;
								case 7: dx = -1;dy = -1; break;
								}
								change[i+dx][j + dy] = true;
							}
						}
					}
				}
			}
			for(int i = 0; i < river.getXSize(); i++) {
				for(int j = 0; j < river.getYSize(); j++) {
					if(area.getValueAt(i, j)) river.setValueAt(i, j, river.getValueAt(i, j) || change[i][j]);
				}
			}
			HeightMap h = MapUtils.asHeightMap(river);
			MapUtils.smooth(h, 2);
			river = h.above(0.2F);
		}
		
		BufferedImage img = new BufferedImage(river.getXSize(), river.getYSize(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = img.getGraphics();
		
		return river;
	}
	
	private static float localChance(int dir, int x, int y) {
		float factor = 1.0F;
		float nx = x / factor;
		float ny = y / factor;
		
		float xc = (float) Math.sin(nx);
		float yc = (float) Math.sin(ny);
		
		byte dx = 0;
		byte dy = 0;
		switch(dir) {
		case 0: dy = -1; break;
		case 1: dx = 1; break;
		case 2: dy = 1; break;
		case 3: dx = -1; break;
		case 4: dx = 1;dy = -1; break;
		case 5: dx = 1;dy = 1; break;
		case 6: dx = -1;dy = 1; break;
		case 7: dx = -1;dy = -1; break;
		}
		
		xc = (dx - xc) * (dx - xc);
		yc = (dy - yc) * (dy - yc);
		float f = xc * yc;
		f = (float) (1/(1+Math.pow(Math.E, -f)));
		return f;
	}
	
	
	public static HeightMap waterFlow(HeightMap layout) {
		DirectionMap d = downwards(layout);
		int[][] flow = new int[layout.getXSize()][layout.getYSize()];
		int[][] water = new int[layout.getXSize()][layout.getYSize()];
		int[][] deltaWater = new int[layout.getXSize()][layout.getYSize()];
		
		for(int i = 0; i < layout.getXSize(); i++) {
			for(int j = 0; j < layout.getYSize(); j++) {
				if(layout.getValueAt(i, j) > 0.3F) {
					water[i][j] = 1;
				}
			}
		}
		
		for(int t = 0; t < 2; t++) {
			for(int i = 1; i < layout.getXSize() - 1; i++) {
				for(int j = 1; j < layout.getYSize() - 1; j++) {
					byte[] dir = d.getValueAt(i, j);
					int w = water[i][j];
					water[i][j] = 0;
					deltaWater[i + dir[0]][j + dir[1]] += w;
				}
			}
			
			for(int i = 0; i < layout.getXSize(); i++) {
				for(int j = 0; j < layout.getYSize(); j++) {
					water[i][j] += deltaWater[i][j];
					flow[i][j] += deltaWater[i][j];
					deltaWater[i][j] = 0;
				}
			}
			System.out.println(t + " / 512");
		}
		HeightMap ret = new HeightMap(layout.getXSize(), layout.getYSize());
		for(int i = 0; i < layout.getXSize(); i++) {
			for(int j = 0; j < layout.getYSize(); j++) {
				ret.setUncappedValueAt(i, j, flow[i][j]);
			}
		}
		int max = (int) ret.getMax();
		for(int i = 0; i < layout.getXSize(); i++) {
			for(int j = 0; j < layout.getYSize(); j++) {
				float f = flow[i][j];
				f = f / 12;
				ret.setValueAt(i, j, f);
			}
		}
		return ret;
	}
	
	public static DirectionMap downwards(HeightMap h) {
		DirectionMap dm = new DirectionMap(h.getXSize(), h.getYSize());
		float f;
		float fd;
		float[] nbs;
		for(int i = 0; i < h.getXSize(); i++) {
			for(int j = 0; j < h.getYSize(); j++) {
				nbs = h.get8Nb(i, j);
				f = Math.min(Math.min(nbs[0], nbs[1]), Math.min(nbs[2], nbs[3]));
				if(f == nbs[0] && f == nbs[1] && f == nbs[2] && f == nbs[3]) {
					dm.setValueAt(i, j, 0, 0);
					continue;
				}
				fd = Math.min(Math.min(nbs[4], nbs[5]), Math.min(nbs[6], nbs[7]));
				float l = fd - h.getValueAt(i, j);
				float m = f - h.getValueAt(i, j);
				l *= 0.7;
				if(l < m) {
					f = fd;
				}
				for(int t = 0; t < 8; t++) {
					if(nbs[t] == f) {
						switch(t) {
						case 0: dm.setValueAt(i, j, 0, -1);
						case 1: dm.setValueAt(i, j, 1, 0);
						case 2: dm.setValueAt(i, j, 0, 1);
						case 3: dm.setValueAt(i, j, -1, 0);
						case 4: dm.setValueAt(i, j, 1, -1);
						case 5: dm.setValueAt(i, j, 1, 1);
						case 6: dm.setValueAt(i, j, -1, 1);
						case 7: dm.setValueAt(i, j, -1, -1);
						}
					}
				}
			}
		}
		return dm;
	}
	
	public static HeightMap ridgeDetect(HeightMap h) {
		HeightMap ret = new HeightMap(h.getXSize(), h.getYSize());
		float f;
		float[] nbs;
		float t = 0;
		for(int i = 0; i < h.getXSize(); i++) {
			for(int j = 0; j < h.getYSize(); j++) {
				nbs = h.get4Nb(i, j);
				f = h.getValueAt(i, j);
				for(float g : nbs) {
					if(g < f && f > 0.1F) t += 0.25F;
				}
				if(t > 0.4F) ret.setValueAt(i, j, t);
				t = 0;
			}
		}
		return ret;
	}
	
	public static RiverInfo riverMask(HeightMap layout, long seed, float maxHeight, float startHeight, float endHeight) {
		//float startHeight = 0.75F;
		//float endHeight = 0.45F;
		
		HashSet<int[]> startPoints = new HashSet<>();
		HashSet<int[]> endPoints = new HashSet<>();
		
		Random rng = new Random(seed);
		float chance = 0.2F;
		int lt = 4096;
		
		for(int i = 0; i < layout.getXSize(); i++) {
			for(int j = 0; j < layout.getYSize(); j++) {
				if(layout.getValueAt(i, j) < startHeight + 0.01F && layout.getValueAt(i, j) > startHeight - 0.01F) {
					if(rng.nextFloat() < chance) {
						int[] c = {i, j};
						startPoints.add(c);
					}
				}
			}
		}
		
		HashSet<int[]> riverStartPoints = new HashSet<>();
		HashSet<River> rivers = new HashSet<>();
		
		boolean[][] isRiverThere = new boolean[layout.getXSize()][layout.getYSize()];
		boolean[][] amIThere = new boolean[layout.getXSize()][layout.getYSize()];
		byte[][] flowDir;
		
		int detail = 4096;
		
		Iterator it = startPoints.iterator();
		while(it.hasNext()) {
			int[] p = (int[]) it.next();
			int x = p[0];
			int y = p[1];
			int[] nbs;
			float[] rnbs;
			int h;
			byte d = 0;
			byte dx = 0;
			byte dy = 0;
			byte ld = 0;
			boolean success = false;
			boolean fr = false;
			boolean fs = false;
			int depth = 4;
			int sx = 0;
			int sy = 0;
			int fx = -1;
			int fy = -1;
			byte[] dts;
			amIThere = new boolean[layout.getXSize()][layout.getYSize()];
			flowDir = new byte[layout.getXSize()][layout.getYSize()];
			ArrayList<int[]> riverPoints = new ArrayList<>();
			
			for(int i = 0; i < lt; i++) {
				try {
					if(isRiverThere[x][y]) {
						success = true;
						fr = true;
						fx = x;
						fy = y;
						break;
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					success = true;
					break;
				}
				
				
				
				if(amIThere[x][y]) break;
				
				rnbs = layout.get4Nb(x, y);
				h = detail;
				h *= layout.getValueAt(x, y);
				nbs = new int[4];
				if(h < endHeight * detail) {
					success = true;
					break;
				}
				for(byte b = 0; b < nbs.length; b++) {
					float fl = rnbs[b];
					fl *= detail;
					nbs[b] = (int) (fl);
				}
				
				d = ld;
								
				sx = 0;
				sy = 0;
				fs = false;
				dts = new byte[4];
				
				for(byte nd = 0; nd < 4; nd++) {
					if((nd - ld) % 4 != 2) {
						sx = 0;
						sy = 0;
						
						dts[nd] = (byte) (depth + 1);
						
						switch(nd) {
						case 0: sy = 1; break;
						case 1: sx = 1; break;
						case 2: sy = -1; break;
						case 3: sx = -1; break;
						}
						
						for(byte di = 1; di <= depth; di++) {
							if(x + di * sx < layout.getXSize() && x + di * sx > 0) {
								if(y + di * sy < layout.getYSize() && y + di * sy > 0) {
									if(isRiverThere[x + di * sx][y + di * sy]) {
										fs = true;
										dts[nd] = di;
										break;
									}
								}
							}
						}
					}
				}
				
				if(fs) {
					byte b = 127;
					for(byte bb:dts) {
						b = (byte) Math.min(bb, b);
					}
					for(int temp3 = 0; temp3 < dts.length; temp3++) {
						if(dts[temp3] == b) {
							d = (byte) temp3;
							break;
						}
					}
					
				} else {
					d = calculateD(rng, ld, h, rnbs, detail);
				}
				
				dx = 0;
				dy = 0;
				
				if((d - ld) % 4 == 2) {
					d = ld;
				}
				
				switch(d) {
				case 0: dy = 1; break;
				case 1: dx = 1; break;
				case 2: dy = -1; break;
				case 3: dx = -1; break;
				}
				
				if(nbs[d] > h + 0.001F * detail) {
					d = calculateD(rng, ld, h, rnbs, detail);
				}
				
				flowDir[x][y] = (byte) (d + 1);
				
				amIThere[x][y] = true;
				int[] ia = {x, y};
				riverPoints.add(ia);
				
				if(nextToRiverOrEdge(x, y, isRiverThere)) {
					success = true;
					fr = true;
					byte b = -1;
					try {
						if(isRiverThere[x][y + 1]) {
							b = 0;
							fx = x;
							fy = y + 1;
						}
					} catch(Exception e) {
						b = 0;
					}
					try {
						if(isRiverThere[x + 1][y]) {
							b = 1;
							fx = x + 1;
							fy = y;
						}
					} catch(Exception e) {
						b = 1;
					}
					try {
						if(isRiverThere[x - 1][y]) {
							b = 3;
							fx = x - 1;
							fy = y;
						}
					} catch(Exception e) {
						b = 3;
					}
					try {
						if(isRiverThere[x][y - 1]) {
							b = 2;
							fx = x;
							fy = y - 1;
						}
					} catch(Exception e) {
						b = 2;
					}	
					
					flowDir[x][y] = (byte) (b + 1);
					break;
				}
				
				x += dx;
				y += dy;
				ld = d;
				
			}
			if(success && riverPoints.size() > 0) {
				int[] t = {x, y};
				endPoints.add(t);
				riverStartPoints.add(p);
				River river = new River(1, p[0], p[1], x, y);
				river.setRiverpoints(riverPoints);
				rivers.add(river);
				if(fx != -1 && fy != -1) {
					River r = getRiverAt(rivers, fx, fy);
					if(r != null) {
						River newRiver = r.split(fx, fy, river.getSize());
						river.setChild(newRiver);
						rivers.add(newRiver);
					}
				}
			}
		}
		Mask m = new Mask(isRiverThere);
		RiverInfo ri = new RiverInfo(m.getXSize(), m.getYSize());
		ri.setEndPoints(endPoints);
		ri.setStartPoints(riverStartPoints);
		ri.setRivers(rivers);
		return ri;
	}
	
	private static boolean nextToRiverOrEdge(int x, int y, boolean[][] isRiverThere) {
		boolean ret = false;
		try { ret |= isRiverThere[x + 1][y]; } catch(Exception e) { ret = true; }
		try { ret |= isRiverThere[x - 1][y]; } catch(Exception e) { ret = true; }
		try { ret |= isRiverThere[x][y - 1]; } catch(Exception e) { ret = true; }
		try { ret |= isRiverThere[x][y + 1]; } catch(Exception e) { ret = true; }
		
		return ret;
	}
	
	private static byte calculateD(Random rng, byte ld, int h, float[] rnbs, int detail) {
		int pb = 0;
		int[] nbs = new int[4];
		for(int j = 0; j < nbs.length; j++) {
			nbs[j] = Math.max(0, (int) (h - rnbs[j] * detail));
			pb += nbs[j];
		}
		int temp = 0;
		int temp2 = 0;
		for(int j = 0; j < nbs.length; j++) {
			temp2 = nbs[j];
			nbs[j] += temp;
			temp += temp2;
		}
		int r = (int) (rng.nextFloat() * pb);
		byte d = ld;
		for(byte j = 0; j < 4; j++) {
			if(r < nbs[j]) {
				d = j;
				break;
			}
		}
		if(pb < (0.001F * detail)) {
			d = ld;
			if(rng.nextFloat() < 0.1F) {
				d += rng.nextBoolean() ? 1 : -1;
				d = (byte) ((d+8) % 4);
			}
		}
		return d;
	}
	
	public static River getRiverAt(HashSet<River> rivers, int x, int y) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			if(r != null && r.getRiverpoints() != null) {
				for(int i = 0; i < r.getRiverpoints().size(); i++) {
					int[] t = r.getRiverpoints().get(i);
					if(t[0] == x && t[1] == y) {
						return r;
					}
				}
			} else {
				it.remove();
			}
		}
		return null;
	}
}
