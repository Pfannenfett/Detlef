package data.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import data.World;
import data.WorldSettings;
import data.map.HeightMap;
import data.map.MapBase;
import data.map.Mask;
import io.ImageIO;

public class MapUtils {
	
	public static void fillHoles(Mask m, int maxHoleSize) {
		Mask checked = new Mask(m.getXSize(), m.getYSize());
		Mask voidArea = new Mask(m.getXSize(), m.getYSize());
		
		for(int i = 0; i < m.getXSize(); i++) {
			for(int j = 0; j < m.getYSize(); j++) {
				if(!m.getValueAt(i, j) && !checked.getValueAt(i, j)) {
					Mask temp = new Mask(m.getXSize(), m.getYSize());
					
					int holeSize = 0;
					
					HashSet<int[]> check = new HashSet<>();
					HashSet<int[]> toCheck = new HashSet<>();
					
					int[] sp = {i, j};
					check.add(sp);
					checked.setValueAt(i, j, true);
					while(!check.isEmpty()) {
						Iterator it = check.iterator();
						while(it.hasNext()) {
							int[] p = (int[]) it.next();
							int x = p[0];
							int y = p[1];

							holeSize += 1;
							temp.setValueAt(x, y, true);
							
							if(voidArea.getValueAt(x + 1, y) || voidArea.getValueAt(x - 1, y) || voidArea.getValueAt(x, y + 1) || voidArea.getValueAt(x, y - 1)) {
								holeSize = maxHoleSize + 2;
								break;
							}
							
							if(m.isInBounds(x + 1, y) && !m.getValueAt(x + 1, y) && !checked.getValueAt(x + 1, y)) {
								int[] np = {x + 1, y};
								checked.setValueAt(x + 1, y, true);
								toCheck.add(np);
							}
							if(m.isInBounds(x - 1, y) && !m.getValueAt(x - 1, y) && !checked.getValueAt(x - 1, y)) {
								int[] np = {x - 1, y};
								checked.setValueAt(x - 1, y, true);
								toCheck.add(np);
							}
							if(m.isInBounds(x, y + 1) && !m.getValueAt(x, y + 1) && !checked.getValueAt(x, y + 1)) {
								int[] np = {x, y + 1};
								checked.setValueAt(x, y + 1, true);
								toCheck.add(np);
							}
							if(m.isInBounds(x, y - 1) && !m.getValueAt(x, y - 1) && !checked.getValueAt(x, y - 1)) {
								int[] np = {x, y - 1};
								checked.setValueAt(x, y - 1, true);
								toCheck.add(np);
							}
							
						}
						
						check = new HashSet<>();
						check.addAll(toCheck);
						toCheck = new HashSet<>();
					}
					
					if(holeSize <= maxHoleSize) {
						m.or(temp);
					} else {
						voidArea.or(temp);
					}
				}
			}
		}
	}
	
	public static HeightMap createBlendingSquare(int size, int borderSize) {
		float[][] f = new float[size][size];
		
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				float v1 = Math.min(i, size - i) / size;
				float v2 = Math.min(j, size - j) / size;
				
				float m = size;
				m /= borderSize;
				
				f[i][j] = Math.min(m * Math.min(v1, v2), 1f);
			}
		}
		
		return new HeightMap(f);
	}
	
	public static MapBase assimilateUp(MapBase map) {
		MapBase ret = new HeightMap(map.getXSize(), map.getYSize());
		for(int i = 0; i < map.getXSize(); i++) {
			for(int j = 0; j < map.getYSize(); j++) {
				float temp = map.getValueAt(i, j);
				for(float f:map.get4Nb(i, j)) {
					temp = Math.max(temp, f);
				}
				ret.setValueAt(i, j, temp);
			}
		}
		return ret;
	}
	
	public static MapBase repAssimilateUp(MapBase map, int iterations) {
		MapBase ret = map;
		for(int i = 0; i < iterations; i++) {
			ret = assimilateUp(ret);
		}
		return ret;
	}
	
	public static MapBase crystalize(MapBase map, Mask startPoints) {
		MapBase ret = new HeightMap(map.clone());
		ret.applyMask(startPoints);
		
		Mask done = startPoints.clone();
		
		HashSet<int[]> points = new HashSet<>();
		for(int x = 0; x < map.getXSize(); x++) {
			for(int y = 0; y < map.getYSize(); y++) {
				if(startPoints.getValueAt(x, y)) {
					int[] uc = {x, y - 1};
					int[] ur = {x + 1, y - 1};
					int[] rc = {x + 1, y};
					int[] dr = {x + 1, y + 1};
					int[] dc = {x, y + 1};
					int[] dl = {x - 1, y + 1};
					int[] lc = {x - 1, y};
					int[] ul = {x - 1, y - 1};
					
					if(!done.getValueAt(uc[0], uc[1]) && !points.contains(uc)) points.add(uc);
					if(!done.getValueAt(ur[0], ur[1]) && !points.contains(ur)) points.add(ur);
					if(!done.getValueAt(rc[0], rc[1]) && !points.contains(rc)) points.add(rc);
					if(!done.getValueAt(dr[0], dr[1]) && !points.contains(dr)) points.add(dr);
					if(!done.getValueAt(dc[0], dc[1]) && !points.contains(dc)) points.add(dc);
					if(!done.getValueAt(dl[0], dl[1]) && !points.contains(dl)) points.add(dl);
					if(!done.getValueAt(lc[0], lc[1]) && !points.contains(lc)) points.add(lc);
					if(!done.getValueAt(ul[0], ul[1]) && !points.contains(ul)) points.add(ul);
				}
			}
		}
		
		HashSet<int[]> temp = new HashSet<>();
		System.out.println("Starting Crystalization");
		Mask marker = done.clone();
		while(!points.isEmpty()) {
			Mask tempDone = done.clone();
			HeightMap tempMap = new HeightMap(ret.clone());
			Iterator it = points.iterator();
			while(it.hasNext()) {
				int[] p = (int[]) it.next();
				if(done.getValueAt(p[0], p[1]) || !done.isInBounds(p[0], p[1])) continue;
				int x = p[0];
				int y = p[1];
				
				float total = 0f;
				int count = 0;
				float[] nb = ret.get8Nb(x, y);
				for(byte i = 0; i < nb.length; i++) {
					byte[] np = decomposeDirection(i);
					if(done.getValueAt(x + np[0], y + np[1])) {
						total += nb[i];
						count += 1;
					}
				}
				
				total /= count;
				tempMap.setValueAt(x, y, total);
				tempDone.setValueAt(x, y, true);
				
				int[] uc = {x, y - 1};
				int[] ur = {x + 1, y - 1};
				int[] rc = {x + 1, y};
				int[] dr = {x + 1, y + 1};
				int[] dc = {x, y + 1};
				int[] dl = {x - 1, y + 1};
				int[] lc = {x - 1, y};
				int[] ul = {x - 1, y - 1};
				
				if(!tempDone.getValueAt(uc[0], uc[1]) && !marker.getValueAt(uc[0], uc[1])) {
					temp.add(uc);
					marker.setValueAt(uc[0], uc[1], true);
				}
				if(!tempDone.getValueAt(ur[0], ur[1]) && !marker.getValueAt(ur[0], ur[1])){
					temp.add(ur);
					marker.setValueAt(ur[0], ur[1], true);
				}
				if(!tempDone.getValueAt(rc[0], rc[1]) && !marker.getValueAt(rc[0], rc[1])) {
					temp.add(rc);
					marker.setValueAt(rc[0], rc[1], true);
				}
				if(!tempDone.getValueAt(dr[0], dr[1]) && !marker.getValueAt(dr[0], dr[1])) {
					temp.add(dr);
					marker.setValueAt(dr[0], dr[1], true);
				}
				if(!tempDone.getValueAt(dc[0], dc[1]) && !marker.getValueAt(dc[0], dc[1])) {
					temp.add(dc);
					marker.setValueAt(dc[0], dc[1], true);
				}
				if(!tempDone.getValueAt(dl[0], dl[1]) && !marker.getValueAt(dl[0], dl[1])) {
					temp.add(dl);
					marker.setValueAt(dl[0], dl[1], true);
				}
				if(!tempDone.getValueAt(lc[0], lc[1]) && !marker.getValueAt(lc[0], lc[1])) {
					temp.add(lc);
					marker.setValueAt(lc[0], lc[1], true);
				}
				if(!tempDone.getValueAt(ul[0], ul[1]) && !marker.getValueAt(ul[0], ul[1])) {
					temp.add(ul);
					marker.setValueAt(ul[0], ul[1], true);
				}
			}
			points = temp;
			temp = new HashSet<>();
			ret = tempMap;
			done = tempDone;
			
			//ImageIO.saveAsGreyImage(ret, "CrystalProgress");
		}
		return ret;
	}
	
	
	public static Mask widen(Mask m, int factor) {
		Mask ma = m;
		for(int i = 0; i < factor; i++) {
			HeightMap h = asHeightMap(ma);
			smooth(h, 1);
			ma = h.above(0.1f);
		}
		return ma;
	}
	
	
	public static Mask createSquareMask(int size, int borderSize) {
		Mask ret = new Mask(size, size);
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				if(i < borderSize || i > size - borderSize || j < borderSize || j > size - borderSize) {
					ret.setValueAt(i, j, true);
				}
			}
		}
		ret.invert();
		return ret;
	}
	
	public static byte[][] toByteMap(MapBase map) {
		float nh = 256f;
		
		byte[][] ret = new byte[map.getXSize()][map.getYSize()];
		for(int i = 0; i < map.getXSize(); i++) {
			for(int j = 0; j < map.getYSize(); j++) {
				float h = map.getValueAt(i, j);
				h *= nh;
				h = Math.min(h, 255);
				h = Math.max(h, 0);
				h -= 128;
				byte b = (byte) h;
				ret[i][j] = b;
			}
		}
		return ret;
	}
	
	public static HeightMap toHeightMap(byte[][] map) {
		float nh = 1f;
		nh /= 255f;
		HeightMap ret = new HeightMap(map.length, map[0].length);
		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[0].length; j++) {
				float h = map[i][j];
				h += 128;
				h *= nh;
				h = Math.min(h, 1f);
				h = Math.max(h, 0f);
				ret.setValueAt(i, j, h);
				
			}
		}
		return ret;
	}
	
	public static Mask findMaskBorder(Mask m) {
		Mask ret = new Mask(m.getXSize(), m.getYSize());
		
		for(int i = 1; i < m.getXSize() - 1; i++) {
			for(int j = 1; j < m.getYSize() - 1; j++) {
				boolean b = m.getValueAt(i, j);
				boolean r = true;
				
				r &= b == m.getValueAt(i + 1, j);
				r &= b == m.getValueAt(i, j + 1);
				r &= b == m.getValueAt(i - 1, j);
				r &= b == m.getValueAt(i, j - 1);
				
				ret.setValueAt(i, j, !r);
			}
		}
		return ret;
	}
	
	//*a is for 1f in decider, b for 0f*/
	public static HeightMap lincomb(HeightMap a, HeightMap b, HeightMap decider) {
		HeightMap ret = new HeightMap(decider.getXSize(), decider.getYSize());
		for(int i = 0; i < decider.getXSize(); i++) {
			for(int j = 0; j < decider.getYSize(); j++) {
				float ah = a.getValueAt(i, j);
				float bh = b.getValueAt(i, j);
				float t = decider.getValueAt(i, j);
				ret.setValueAt(i, j, ah * t + (1f - t) * bh);
			}
		}
		return ret;
	}
	
	public static Mask minTrueSize(Mask m, int blobSize) {
		Mask ret = m.clone();
		Mask done = new Mask(m.getXSize(), m.getYSize());
		
		
		for(int i = 0; i < m.getXSize(); i++) {
			for(int j = 0; j < m.getYSize(); j++) {
				if(m.getValueAt(i, j) && !done.getValueAt(i, j)) {
					Mask marker = new Mask(m.getXSize(), m.getYSize());
					
					for(int k = 0; k < m.getXSize(); k++) {
						for(int l = 0; l < m.getYSize(); l++) {
							marker.setValueAt(k, l, true);
						}
					}
					
					ArrayList<int[]> todo = new ArrayList<>();
					int[] sp = {i, j};
					todo.add(sp);
					done.setValueAt(sp[0], sp[1], true);
					marker.setValueAt(sp[0], sp[1], false);
					int counter = 0;
					ArrayList<int[]> tempdo = new ArrayList<>();
					while(!todo.isEmpty() && counter < blobSize) {
						for(int t = 0; t < todo.size(); t++) {
							int[] p = todo.get(t);
							
							
							
							int[] xp = {p[0] + 1, p[1]};
							int[] yp = {p[0], p[1] + 1};
							int[] xm = {p[0] - 1, p[1]};
							int[] ym = {p[0], p[1] - 1};
							if(m.getValueAt(xp[0], xp[1]) && !done.getValueAt(xp[0], xp[1])) {
								tempdo.add(xp);
								done.setValueAt(xp[0], xp[1], true);
								marker.setValueAt(xp[0], xp[1], false);
							}
							if(m.getValueAt(yp[0], yp[1]) && !done.getValueAt(yp[0], yp[1])) {
								tempdo.add(yp);
								done.setValueAt(yp[0], yp[1], true);
								marker.setValueAt(yp[0], yp[1], false);
							}
							if(m.getValueAt(xm[0], xm[1]) && !done.getValueAt(xm[0], xm[1])) {
								tempdo.add(xm);
								done.setValueAt(xm[0], xm[1], true);
								marker.setValueAt(xm[0], xm[1], false);
							}
							if(m.getValueAt(ym[0], ym[1]) && !done.getValueAt(ym[0], ym[1])) {
								tempdo.add(ym);
								done.setValueAt(ym[0], ym[1], true);
								marker.setValueAt(ym[0], ym[1], false);
							}
						}
						
						counter++;
						todo = tempdo;
						tempdo = new ArrayList<>();
					}
					
					if(counter < blobSize) {
						ret.and(marker);
					}
				}
			}
		}
		
		return ret;
	}
	
	
	public static byte[] decomposeDirection(byte dir) {
		byte sx = 0, sy = 0;
		switch(dir) {
		case 0: sy = 1; break;
		case 1: sx = 1; break;
		case 2: sy = -1; break;
		case 3: sx = -1; break;
		case 4: sy = 1; sx = 1; break;
		case 5: sx = 1; sy = -1; break;
		case 6: sy = -1; sx = -1; break;
		case 7: sx = -1; sy = 1; break;
		}
		byte[] ret = {sx, sy};
		return ret;
	}
	
	public static void remapHeight(MapBase a, float oldHeight, float newHeight) {
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				if(a.getValueAt(i, j) == oldHeight) {
					a.setValueAt(i, j, newHeight);
				}
			}
		}
	}
	
	public static MapBase normMultiply(MapBase a, MapBase b) {
		normalize(a);
		normalize(b);
		MapBase ret = new HeightMap(a.getContent());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setUncappedValueAt(i, j, a.getValueAt(i, j) * b.getValueAt(i, j));
			}
		}
		normalize(ret);
		return ret;
	}
	
	public static MapBase cappedMultiply(MapBase a, MapBase b) {
		MapBase ret = new HeightMap(a.clone());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setValueAt(i, j, a.getValueAt(i, j) * b.getValueAt(i, j));
			}
		}
		return ret;
	}
	
	public static void distToHeight(MapBase a, float h) {
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				float f = a.getValueAt(i, j);
				f -= h;
				f = Math.abs(f);
				f /= 1.00000001 - f;
				a.setValueAt(i, j, f);
			}
		}
	}
	
	public static HeightMap terraces(HeightMap h, float[] terraceHeights) {
		HeightMap ret = new HeightMap(h.getXSize(), h.getYSize());
		for(int i = 0; i < h.getXSize(); i++) {
			for(int j = 0; j < h.getYSize(); j++) {
				float f = h.getValueAt(i, j);
				float max = 0f;
				for(int k = 0; k < terraceHeights.length; k++) {
					if(terraceHeights[k] < f && terraceHeights[k] > max) {
						max = terraceHeights[k];
					}
				}
				ret.setValueAt(i, j, max);
			}
		}
		return ret;
	}
	
	public static void squish(MapBase a) {
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				float f = a.getValueAt(i, j);
				f = (float) (1/(1+Math.pow(Math.E, f)));
				a.setValueAt(i, j, f);
			}
		}
	}
	
	public static MapBase curve(MapBase a) {
		MapBase ret = new HeightMap(a.getContent());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setValueAt(i, j, a.getValueAt(i, j) * a.getValueAt(i, j));
			}
		}
		return ret;
	}
	
	public static MapBase curveAbove(MapBase a, float f) {
		MapBase ret = new HeightMap(a.getContent());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				float t = a.getValueAt(i, j);
				if(t > f) {
					t -= f;
					t *= 1 / (1 - f);
					t *= t;
					t += f;
				}
				ret.setValueAt(i, j, t);
			}
		}
		return ret;
	}
	
	public static HeightMap geoDistAboveHeight(HeightMap h, float f, int maxDist) {
		
		float[][] map = new float[h.getXSize()][h.getYSize()];
		for(int i = 0; i < h.getXSize(); i++) {
			for(int j = 0; j < h.getYSize(); j++) {
				map[i][j] = -1f;
			}
		}
		for(int i = 0; i < h.getXSize(); i++) {
			for(int j = 0; j < h.getYSize(); j++) {
				float t = h.getValueAt(i, j);
				if(t <= f) map[i][j] = 0f;
			}
		}
		
		for(int iter = 0; iter < maxDist; iter++) {
			float[][] tempMap = new float[h.getXSize()][h.getYSize()];
			for(int i = 0; i < h.getXSize(); i++) {
				for(int j = 0; j < h.getYSize(); j++) {
					tempMap[i][j] = -1f;
				}
			}
			float nh = iter;
			nh /= maxDist;
			for(int i = 0; i < h.getXSize(); i++) {
				for(int j = 0; j < h.getYSize(); j++) {
					if(map[i][j] != -1f) continue;
					boolean flag = false;
					try{ flag |= map[i - 1][j] > -1f; } catch(Exception e) {}
					try{ flag |= map[i + 1][j] > -1f; } catch(Exception e) {}
					try{ flag |= map[i][j - 1] > -1f; } catch(Exception e) {}
					try{ flag |= map[i][j + 1] > -1f; } catch(Exception e) {}
					
					if(flag) tempMap[i][j] = nh;
				}
			}
			for(int i = 0; i < h.getXSize(); i++) {
				for(int j = 0; j < h.getYSize(); j++) {
					map[i][j] = Math.max(tempMap[i][j], map[i][j]);
				}
			}
		}
		for(int i = 0; i < h.getXSize(); i++) {
			for(int j = 0; j < h.getYSize(); j++) {
				if(map[i][j] == -1f) map[i][j] = 1f;
			}
		}
		
		
		return new HeightMap(map);
	}
	
	//public static HeightMap geoDistAboveHeight(HeightMap h, float f, int maxDist) {
		
	//}
	
	
	public static MapBase submap(MapBase mb, int x, int y, int xSize, int ySize) {
		MapBase ret = new HeightMap(xSize, ySize);
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				ret.setValueAt(i, j, mb.getValueAt(i + x, j + y));
			}
		}
		return ret;
	}
	
	public static Mask submap(Mask mb, int x, int y, int xSize, int ySize) {
		Mask ret = new Mask(xSize, ySize);
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				ret.setValueAt(i, j, mb.getValueAt(i + x, j + y));
			}
		}
		return ret;
	}
	
	public static HeightMap convertMapType(HeightMap h, boolean toUpperMap) {
		HeightMap ret = new HeightMap(h.clone());
		if(h.isUpperMap()) {
			if(!toUpperMap) {
				Mask notNullMask = h.above(0.001f);
				ret.add(WorldSettings.waterHeightDown - WorldSettings.waterHeightUp);
				ret.cap();
				ret.applyMask(notNullMask);
				ret.setUpperMap(false);
			}
		} else {
			if(toUpperMap) {
				ret.add(WorldSettings.waterHeightUp - WorldSettings.waterHeightDown);
				ret.cap();
				ret.setUpperMap(true);
			}
		}
		
		return ret;
	}
	
	public static HeightMap slopes(MapBase a) {
		HeightMap ret = new HeightMap(a.getXSize(), a.getYSize());
		for(int i = 1; i < a.getXSize() - 1; i++) {
			for(int j = 1; j < a.getYSize() - 1; j++) {
				float xDiff = a.getValueAt(i - 1, j) - a.getValueAt(i + 1, j);
				float yDiff = a.getValueAt(i, j - 1) - a.getValueAt(i, j + 1);
				xDiff /= 2;
				yDiff /= 2;
				float f = Math.max(Math.abs(xDiff), Math.abs(yDiff));
				ret.setValueAt(i, j, f);
			}
		}
		return ret;
	}
	
	public static float addDimin(float a, float b) {
		return a + (1 - a) * b;
	}
	
	public static float substractDimin(float a, float b) {
		return a - a * b;
	}
	
	public static MapBase addDiminMap(MapBase a, MapBase b) {
		MapBase ret = new HeightMap(a.clone());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setValueAt(i, j, addDimin(a.getValueAt(i, j), b.getValueAt(i, j)));
			}
		}
		return ret;
	}
	
	public static MapBase average(MapBase a, MapBase b) {
		MapBase ret = new HeightMap(a.getContent().clone());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setValueAt(i, j,( a.getValueAt(i, j) + b.getValueAt(i, j) / 2));
			}
		}
		return ret;
	}
	
	public static MapBase substractDiminMap(MapBase a, MapBase b) {
		MapBase ret = new HeightMap(a.getContent().clone());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setValueAt(i, j, substractDimin(a.getValueAt(i, j), b.getValueAt(i, j)));
			}
		}
		return ret;
	}
	
	public static void addDiminMapOn(MapBase base, MapBase onTop) {
		for(int i = 0; i < base.getXSize(); i++) {
			for(int j = 0; j < base.getYSize(); j++) {
				base.setValueAt(i, j, addDimin(base.getValueAt(i, j), onTop.getValueAt(i, j)));
			}
		}
	}
	
	public static MapBase addNormalizedMap(MapBase a, MapBase b) {
		MapBase ret = new HeightMap(a.getContent());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setUncappedValueAt(i, j, a.getValueAt(i, j) + b.getValueAt(i, j));
			}
		}
		setPositive(ret);
		normalize(ret);
		return ret;
	}
	
	public static MapBase addCappedMap(MapBase a, MapBase b) {
		MapBase ret = new HeightMap(a.getContent());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setUncappedValueAt(i, j, a.getSafeValueAt(i, j) + b.getSafeValueAt(i, j));
			}
		}
		ret.cap();
		return ret;
	}
	
	public static MapBase normalizeTo(MapBase a, float max) {
		float[][] f = a.getContent();
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				f[i][j] *= max;
			}
		}
		a.setContent(f);
		return a;
	}
	
	public static MapBase absoluteinvert(MapBase a) {
		float[][] f = a.getContent();
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				f[i][j] -= 0.5;
				f[i][j] *= 2;
				f[i][j] *= f[i][j];
			}
		}
		a.setContent(f);
		return a;
	}
	
	public static MapBase invert(MapBase a) {
		float[][] f = a.getContent();
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				f[i][j] = 1 - f[i][j];
			}
		}
		a.setContent(f);
		return a;
	}
	
	public static void centerValues(MapBase a, float offset) {
		float[][] f = a.getContent();
		float t = 1 - 2 * offset;
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				f[i][j] *= t;
				f[i][j] += offset;
			}
		}
		a.setContent(f);
	}
	
	public static void shrink(MapBase a, int factor) {
		int nx = a.getXSize()  / factor;
		int ny = a.getYSize()  / factor;
		MapBase ret = new HeightMap(nx, ny);
		float[][] f = ret.getContent();
		float[][] c = a.getContent();
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				float t = 0;
				for(int x = 0; x < factor; x++) {
					for(int y = 0; y < factor; y++) {
						int ax = i * factor + x;
						int ay = j * factor + y;
						t += c[ax][ay];
					}
				}
				t /= (factor * factor);
				f[i][j] = t;
			}
		}
		a.setContent(ret.getContent());
	}
	
	public static void shrinkMax(MapBase a, int factor) {
		int nx = a.getXSize()  / factor;
		int ny = a.getYSize()  / factor;
		MapBase ret = new HeightMap(nx, ny);
		float[][] f = ret.getContent();
		float[][] c = a.getContent();
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				float t = 0;
				for(int x = 0; x < factor; x++) {
					for(int y = 0; y < factor; y++) {
						int ax = i * factor + x;
						int ay = j * factor + y;
						t = Math.max(c[ax][ay], t);
					}
				}
				f[i][j] = t;
			}
		}
		a.setContent(ret.getContent());
	}
	
	/*public static void createBeaches(World w) {
		int beachHeight = 8064;
		short[][] lhs = w.getHeightmap();
		
		for(int i = 0; i < lhs.length; i++) {
			for(int j = 0; j < lhs[i].length; j++) {
				if(lhs[i][j] < beachHeight - 256) {
					lhs[i][j] += 256;
				} else if(lhs[i][j] < beachHeight) {
					lhs[i][j] = (short) beachHeight;
				}
			}
		}
	}
	
	public static void invert(World w) {
		for(int i = 0; i < w.getBounds().height; i++) {
			for(int j = 0; j < w.getBounds().width; j++) {
				w.setHeightAt(i, j, (short) (32767 - w.getHeightAt(i, j))); 
			}
		}
	}
	
	

	
	
	
	public static void maskUp(World w, World mask) {
		for(int i = 0; i < w.getBounds().width; i++) {
			for(int j = 0; j < w.getBounds().height; j++) {
				double h = w.getHeightAt(i, j);
				double m = mask.getHeightAt(i, j);
				
				h = Math.min(h, m);
				w.setHeightAt(i, j, (short) h); 
			}
		}
	}
	
	public static void fillUp(World w, short height) {
		for(int i = 0; i < w.getBounds().width; i++) {
			for(int j = 0; j < w.getBounds().height; j++) {
				double h = w.getHeightAt(i, j);
				
				h = Math.max(h, height);
				w.setHeightAt(i, j, (short) h); 
			}
		}
	}
	
	
	public static void shift(World w, short amount, short minHeight, short maxHeight) {
		for(int i = 0; i < w.getBounds().width; i++) {
			for(int j = 0; j < w.getBounds().height; j++) {
				if(w.getHeightAt(i, j) <= maxHeight && w.getHeightAt(i, j) >= minHeight) {
					int t = w.getHeightAt(i, j) + amount;
					t = Math.min(t, Short.MAX_VALUE);
					t = Math.max(t, 0);
					w.setHeightAt(i, j, (short) t);
				}
			}
		}
	}
	
	public static void average(World w1, World w2) {
		if(!w1.getBounds().equals(w2.getBounds())) {
			System.out.println(w1.getBounds().height + " != " + w2.getBounds().height + "!!!!!!!!!!!!");
		}
		
		for(int i = 0; i < w1.getBounds().height; i++) {
			for(int j = 0; j < w1.getBounds().width; j++) {
				int h = w1.getHeightAt(i, j);
				h += w2.getHeightAt(i, j);
				h /= 2;
				h = Math.min(h, 32767);
				w1.setHeightAt(i, j, (short) h);
			}
		}
	}
	
	public static void clamp(World w) {
		short whs[][] = w.getHeightmap();
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				if(whs[i][j] < -8192) whs[i][j] = Short.MAX_VALUE - 1;
				whs[i][j] = (short) Math.max(4096, whs[i][j]);
				whs[i][j] = (short) Math.min(32760, whs[i][j]);
			}
		}
	}
	
	public static void strengthen(World w, short midVal) {
		short whs[][] = w.getHeightmap();
		short a = midVal;
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				int h = whs[i][j] - a;
				h *= 1.5;
				int r = h + a;
				
				if(r < 0) {
					r = 0;
				}
				if(r > Short.MAX_VALUE) {
					r = (short) (Short.MAX_VALUE) - 1;
				}
				whs[i][j] = (short) r;
			}
		}
	}
	
	public static void zoom(World w, int factor) {
		short whs[][] = w.getHeightmap();
		short[][] temp = new short[whs.length * factor][whs[0].length * factor];
		
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				short value = whs[i][j];
				
				for(int k = 0; k < factor; k++) {
					for(int m = 0; m < factor; m++) {
						int x = i * factor + k;
						int y = j * factor + m;
						temp[x][y] = value;
					}
				}
			}
		}
		w.setHeightmap(temp);
					
	}

	
	public static void translate(World w, short value) {
		short whs[][] = w.getHeightmap();
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				w.setHeightAt(i, j, (short) Math.min(32767, Math.max(0, w.getHeightAt(i, j) + value)));
			}
		}
	}  */
	
	public static HeightMap asHeightMap(Mask m) {
		float[][] temp = new float[m.getXSize()][m.getYSize()];
		for(int i = 0; i < m.getXSize(); i++) {
			for(int j = 0; j < m.getYSize(); j++) {
				temp[i][j] = m.getValueAt(i, j) ? 1.0F : 0.0F;
			}
		}
		return new HeightMap(temp);
	}
	
	public static MapBase zoom(MapBase w, int factor) {
		float whs[][] = w.getContent();
		float[][] temp = new float[whs.length * factor][whs[0].length * factor];
		
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				float value = whs[i][j];
				
				for(int k = 0; k < factor; k++) {
					for(int m = 0; m < factor; m++) {
						int x = i * factor + k;
						int y = j * factor + m;
						temp[x][y] = value;
					}
				}
			}
		}
		w.setContent(temp);		
		return w;
	}
	
	public static MapBase addMax(MapBase a, MapBase b) {
		MapBase ret = new HeightMap(a.getContent());
		for(int i = 0; i < a.getXSize(); i++) {
			for(int j = 0; j < a.getYSize(); j++) {
				ret.setValueAt(i, j, Math.max(a.getValueAt(i, j), b.getValueAt(i, j)));
			}
		}
		return ret;
	}
	
	public static void addMaxOn(MapBase base, MapBase onTop) {
		for(int i = 0; i < base.getXSize(); i++) {
			for(int j = 0; j < base.getYSize(); j++) {
				base.setValueAt(i, j, Math.max(base.getValueAt(i, j), onTop.getValueAt(i, j)));
			}
		}
	}
	
	public static MapBase cut(MapBase a, float lower, float higher) {
		HeightMap ret = new HeightMap(a.getContent());
		for(int i = 0; i < ret.getXSize(); i++) {
			for(int j = 0; j < ret.getYSize(); j++) {
				if(ret.getValueAt(i, j) < lower) ret.setValueAt(i, j, 0);
				if(ret.getValueAt(i, j) > higher) ret.setValueAt(i, j, 1F);
			}
		}
		return ret;
	}
	
	public static MapBase smoothNCut(MapBase w, float cutLevel, int iterations) {
		MapBase ret = w;
		for(int iter = 0; iter < iterations; iter++) {
			smooth(ret, 1);
			ret = cut(ret, cutLevel, 1F);
		}
		return ret;
	}
	
	
	public static void smooth(MapBase w, int iterations) {
		smoothAt(w, iterations, Short.MIN_VALUE, Short.MAX_VALUE );
	}
	
	public static void smoothAt(MapBase w, int iterations, short minHeight, int maxHeight) {
		float fs[][] = w.getContent();
		int[][] whs = new int[fs.length][fs[0].length];
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				whs[i][j] = (int) (fs[i][j] * Short.MAX_VALUE);
			}
		}
		for(int p = 0; p < iterations; p++) {
			for(int i = 0; i < whs.length; i++) {
				for(int j = 0; j < whs[i].length; j++) {
					int c = whs[i][j];
					if(c > minHeight && c < maxHeight) {
						if(i < whs.length - 1 && j < whs[0].length - 1 && i > 0 && j > 0) {
							int u = whs[i - 1][j]; 
							int r = whs[i][j + 1];
							int d = whs[i + 1][j];
							int l = whs[i][j - 1];
							
							int ur = (int) (whs[i - 1][j + 1] * 0.75);
							int rd = (int) (whs[i + 1][j + 1] * 0.75);
							int dl = (int) (whs[i + 1][j - 1] * 0.75);
							int lu = (int) (whs[i - 1][j - 1] * 0.75);
							
							int sum = c + u + r + d + l + ur + rd + dl + lu;
							sum = (int) (sum / 8);
							
							whs[i][j] = (short) sum;
						} else {
							int pi = 0;
							int pj = 0;
							if(i >= whs.length - 1) pi++;
							if(j >= whs[0].length - 1) pj++;
							if(i <= 0) pi++;
							if(j <= 0) pj++;
							
							int pb = pi + pj;
							
							
							int u = c;
							int r = c;
							int d = c;
							int l = c;
							
							int ur = (int) (c * 0.75);
							int rd = (int) (c * 0.75);
							int dl = (int) (c * 0.75);
							int lu = (int) (c * 0.75);
							
							if(i > 0) u = whs[i - 1][j]; 
							if(j < whs[0].length - 1) r = whs[i][j + 1];
							if(i < whs.length - 1) d = whs[i + 1][j];
							if(j > 0) l = whs[i][j - 1];
							
							if(j < whs[0].length - 1 && i > 0) ur = (int) (whs[i - 1][j + 1] * 0.75);
							if(i < whs.length - 1 && j < whs[0].length - 1) d = (int) (whs[i + 1][j + 1] * 0.75);
							if(i < whs.length - 1 && j > 0) d = (int) (whs[i + 1][j - 1] * 0.75);
							if(i > 0 && j > 0) lu = (int) (whs[i - 1][j - 1] * 0.75);
							
							int sum = c + u + r + d + l + ur + rd + dl + lu;
							int div = 8;
							if(pb == 1) div = 6;
							if(pb == 2) div = 4;
							sum = (int) (sum / 8);
							
							whs[i][j] = (short) sum;
						}
					}
				}
			}
		}
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				fs[i][j] = whs[i][j];
				fs[i][j] = fs[i][j]  / Short.MAX_VALUE;
			}
		}
		w.setContent(fs);
	}
	
	public static void smoothBiggen(MapBase w, int factor) {
		while(factor > 1) {
			biggen(w, 2);
			smooth(w, 1);
			factor /= 2;
		}
	}
	
	public static void noClampBiggen(MapBase w, int factor) {
		float[][] whs = w.getContent();
		float[][] temp = new float[whs.length * factor][whs[0].length * factor];
		
		System.out.println("biggen from " + w.getXSize() + " to " + w.getXSize() * factor);
		
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				float value = whs[i][j];
				
				float u = value;
				float r = value;
				float d = value;
				float l = value;
				
				try { u = whs[i - 1][j]; } catch(ArrayIndexOutOfBoundsException a) {}
				try { r = whs[i][j + 1]; } catch(ArrayIndexOutOfBoundsException a) {}
				try { d = whs[i + 1][j]; } catch(ArrayIndexOutOfBoundsException a) {}
				try { l = whs[i][j - 1]; } catch(ArrayIndexOutOfBoundsException a) {}
				
				
				for(int k = 0; k < factor; k++) {
					for(int m = 0; m < factor; m++) {
						
						float dU = -(value - u);
						float dR = -(value - r);
						float dD = -(value - d);
						float dL = -(value - l);
						
						dU /= factor / 2;
						dR /= factor / 2;
						dD /= factor / 2;
						dL /= factor / 2;
						
						float uf = (factor / 2);
						float rf = -(factor / 2);
						float df = -(factor / 2);
						float lf = (factor / 2);
						
						uf -= k;
						rf += m;
						df += k;
						lf -= m;
						
						//uf = -uf;
						//rf = -rf;
						//df = -df;
						//lf = -lf;
						
						uf = Math.max(0, uf);
						rf = Math.max(0, rf);
						df = Math.max(0, df);
						lf = Math.max(0, lf);
						
						dU *= uf;
						dR *= rf;
						dD *= df;
						dL *= lf;
						
						float h = dU + dR + dD + dL;
						h = h / 2;
						h += value;
						//h = Math.min(h, 1);
						
						float height = h;
						
						int x = i * factor + k;
						int y = j * factor + m;
						temp[x][y] = height;
					}
				}
			}
		}
		w.setContent(temp);
	}
	
	public static void biggen(MapBase w, int factor) {
		float[][] whs = w.getContent();
		float[][] temp = new float[whs.length * factor][whs[0].length * factor];
		
		//System.out.println("biggen from " + w.getXSize() + " to " + w.getXSize() * factor);
		
		for(int i = 0; i < whs.length; i++) {
			for(int j = 0; j < whs[i].length; j++) {
				float value = whs[i][j];
				
				float u = value;
				float r = value;
				float d = value;
				float l = value;
				
				try { u = whs[i - 1][j]; } catch(ArrayIndexOutOfBoundsException a) {}
				try { r = whs[i][j + 1]; } catch(ArrayIndexOutOfBoundsException a) {}
				try { d = whs[i + 1][j]; } catch(ArrayIndexOutOfBoundsException a) {}
				try { l = whs[i][j - 1]; } catch(ArrayIndexOutOfBoundsException a) {}
				
				
				for(int k = 0; k < factor; k++) {
					for(int m = 0; m < factor; m++) {
						
						float dU = -(value - u);
						float dR = -(value - r);
						float dD = -(value - d);
						float dL = -(value - l);
						
						dU /= factor / 2;
						dR /= factor / 2;
						dD /= factor / 2;
						dL /= factor / 2;
						
						float uf = (factor / 2);
						float rf = -(factor / 2);
						float df = -(factor / 2);
						float lf = (factor / 2);
						
						uf -= k;
						rf += m;
						df += k;
						lf -= m;
						
						//uf = -uf;
						//rf = -rf;
						//df = -df;
						//lf = -lf;
						
						uf = Math.max(0, uf);
						rf = Math.max(0, rf);
						df = Math.max(0, df);
						lf = Math.max(0, lf);
						
						dU *= uf;
						dR *= rf;
						dD *= df;
						dL *= lf;
						
						float h = dU + dR + dD + dL;
						h = h / 2;
						h += value;
						h = Math.min(h, 1);
						
						float height = h;
						
						int x = i * factor + k;
						int y = j * factor + m;
						temp[x][y] = height;
					}
				}
			}
		}
		w.setContent(temp);
	}
	
	public static void normalize(MapBase w) {
		float min = w.getMin();
		float max = w.getMax();
		float[][] content = w.getContent();
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] -= min;
				content[i][j] /= (max - min);
			}
		}
		w.setContent(content);
	}
	
	public static HeightMap difference(MapBase a, MapBase b) {
		if(a.getXSize() != b.getXSize() || a.getYSize() != b.getYSize()) throw new IllegalArgumentException("Maps should be the same Size");
		float[][] ret = new float[a.getXSize()][a.getYSize()];
		float[][] content = a.getContent();
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				ret[i][j] = 0.5f + 0.5f * (a.getValueAt(i, j) - b.getValueAt(i, j));
			}
		}
		return new HeightMap(ret);
	}
	
	public static void setPositive(MapBase w) {
		float[][] content = w.getContent();
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] = Math.max(content[i][j], 0);
			}
		}
	}
	
	public static void flipRandom(MapBase w, long seed) {
		Random rng = new Random(seed);
		if(rng.nextBoolean()) flip(w, false);
		if(rng.nextBoolean()) flip(w, true);
	}
	
	public static void flip(MapBase w, boolean b) {
		int xSize = w.getXSize();
		int ySize = w.getYSize();
		float[][] temp = new float[xSize][ySize];
		if(b) {
			for(int i = 0; i < xSize; i++) {
				for(int j = 0; j < ySize; j++) {
					temp[xSize - i - 1][j] = w.getValueAt(i, j);
				}
			}
		} else {
			for(int i = 0; i < xSize; i++) {
				for(int j = 0; j < xSize; j++) {
					temp[i][ySize - j - 1] = w.getValueAt(i, j);
				}
			}
		}
		w.setContent(temp);
	}
	
	public static float[][] convolute(HeightMap h, float[][] conv) {
		return convolute(h.getContent(), conv, 0f);
	}
	
	public static float[][] convolute(HeightMap h, float[][] conv, float edgeValue) {
		return convolute(h.getContent(), conv, edgeValue);
	}
	
	public static float[][] convolute(float[][] c, float[][] conv) {
		return convolute(c, conv, 0f);
	}
	
	public static float[][] convolute(float[][] c, float[][] conv, float edgeValue) {
		int n = (conv.length - 1) / 2;
		
		float[][] cbuffer = new float[c.length + 2 * n][c[0].length + 2 * n];
		
		for(int i = 0; i < cbuffer.length; i++) {
			for(int j = 0; j < cbuffer[0].length; j++) {
				cbuffer[i][j] = edgeValue;
			}
		}
		
		for(int i = n; i < cbuffer.length - n; i++) {
			for(int j = n; j < cbuffer[0].length - n; j++) {
				cbuffer[i][j] = c[i-n][j-n];
			}
		}
		
		//float[][] ret = c.clone();
		float[][] ret = new float[c.length][c[0].length];
		
		for(int i = n; i < cbuffer.length - n; i++) {
			for(int j = n; j < cbuffer[0].length - n; j++) {
				float temp = 0;
				for(int x = 0; x < conv.length; x++) {
					for(int y = 0; y < conv[0].length; y++) {
						temp += conv[x][y] * cbuffer[i + x - n][j + y - n];
					}
				}
				temp /= (conv.length * conv[0].length);
				ret[i-n][j-n] = temp;
			}
		}
		return ret;
	}
	
	public static void repeatedConvSmooth(MapBase h, int smoothSize, int iterations, float edgeValue) {
		for(int i = 0; i < iterations; i++) {
			convSmooth(h, smoothSize, edgeValue);
		}
	}
	
	public static void repeatedConvSmooth(MapBase h, int smoothSize, int iterations) {
		repeatedConvSmooth(h, smoothSize, iterations, 0f);
	}
	
	public static void convSmooth(MapBase h, int smoothSize) {
		convSmooth(h, smoothSize, 0f);
	}
	
	public static void convSmooth(MapBase h, int smoothSize, float edgeValue) {
		convSmooth(h, smoothSize, edgeValue, 1f);
	}
	
	public static void convSmooth(MapBase h, int smoothSize, float edgeValue, float midWeightMultiplier) {
		float[][] lens = new float[2*smoothSize + 1][2 * smoothSize + 1];
		float total = 0;
		for(int x = -smoothSize; x <= smoothSize; x++) {
			for(int y = -smoothSize; y <= smoothSize; y++) {
				lens[x + smoothSize][y + smoothSize] = 1 - (x^2 + y^2)/(2*smoothSize*smoothSize);
				if(x == 0 && y == 0) lens[smoothSize][smoothSize] *= midWeightMultiplier;
				total += lens[x + smoothSize][y + smoothSize];
			}
		}
		float f = (2*smoothSize + 1)*(2*smoothSize + 1) / total;
		for(int x = -smoothSize; x <= smoothSize; x++) {
			for(int y = -smoothSize; y <= smoothSize; y++) {
				lens[x + smoothSize][y + smoothSize] *= f;
			}
		}
		h.setContent(convolute(h.getContent(), lens, edgeValue));
	}
	
	public static void convSmooth(MapBase h, int smoothSize, float edgeValue, float midWeightMultiplier, Mask m) {
		float[][] lens = new float[2*smoothSize + 1][2 * smoothSize + 1];
		float total = 0;
		for(int x = -smoothSize; x <= smoothSize; x++) {
			for(int y = -smoothSize; y <= smoothSize; y++) {
				lens[x + smoothSize][y + smoothSize] = 1 - (x^2 + y^2)/(2*smoothSize*smoothSize);
				if(x == 0 && y == 0) lens[smoothSize][smoothSize] *= midWeightMultiplier;
				total += lens[x + smoothSize][y + smoothSize];
			}
		}
		float f = (2*smoothSize + 1)*(2*smoothSize + 1) / total;
		for(int x = -smoothSize; x <= smoothSize; x++) {
			for(int y = -smoothSize; y <= smoothSize; y++) {
				lens[x + smoothSize][y + smoothSize] *= f;
			}
		}
		
		int n = (lens.length - 1) / 2;
		
		float[][] cbuffer = new float[h.getContent().length + 2 * n][h.getContent()[0].length + 2 * n];
		
		for(int i = 0; i < cbuffer.length; i++) {
			for(int j = 0; j < cbuffer[0].length; j++) {
				cbuffer[i][j] = edgeValue;
			}
		}
		
		for(int i = n; i < cbuffer.length - n; i++) {
			for(int j = n; j < cbuffer[0].length - n; j++) {
				cbuffer[i][j] = h.getContent()[i-n][j-n];
			}
		}
		
		//float[][] ret = c.clone();
		float[][] ret = new float[h.getContent().length][h.getContent()[0].length];
		
		for(int i = n; i < cbuffer.length - n; i++) {
			for(int j = n; j < cbuffer[0].length - n; j++) {
				if(!m.getValueAt(i - n, j - n)) {
					ret[i-n][j-n] = cbuffer[i][j];
					continue;
				}
				
				float temp = 0;
				for(int x = 0; x < lens.length; x++) {
					for(int y = 0; y < lens[0].length; y++) {
						temp += lens[x][y] * cbuffer[i + x - n][j + y - n];
					}
				}
				temp /= (lens.length * lens[0].length);
				ret[i-n][j-n] = temp;
			}
		}
		
		h.setContent(ret);
	}
	
	public static float getMin(float[][] f) {
		float min = f[0][0];
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				min = Math.min(min, f[i][j]);
			}
		}
		return min;
	}
	
	public static float getMax(float[][] f) {
		float max = f[0][0];
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				max = Math.max(max, f[i][j]);
			}
		}
		return max;
	}
	
	public static float[][] addEverywhere(float[][] f, float a) {
		float[][] ret = f.clone();
		for(int i = 0; i < f.length; i++) {
			for(int j = 0; j < f[0].length; j++) {
				ret[i][j] += a;
			}
		}
		return ret;
	}
	
	public static float[][] widenExtremes(float[][] f, int iterations) {
		HeightMap h = new HeightMap(f.clone());
		float average = (h.getMax() + h.getMin())/2;
		System.out.println(average);
		
		for(int i = 0; i < iterations; i++) {
			MapUtils.convSmooth(h, 2, 0.5f);
			Mask mask = h.above(average);
			Mask invMask = h.below(average);
			HeightMap h1 = h.max(new HeightMap(f.clone()));
			HeightMap h2 = h.min(new HeightMap(f.clone()));
			h2.applyMask(invMask, average);
			h1.applyMask(mask, 0f);
			h = h1.max(h2);
		}
		
		return h.getContent();
	}
	
	public static HeightMap createGradientWorld(int xSize, int ySize){
		float f = 1f;
		f /= xSize;
		HeightMap ret = new HeightMap(xSize, ySize);
		for(int i = 0; i < xSize; i++) {
			float t = i;
			t *= f;
			for(int j = 0; j < ySize; j++) {
				
				ret.setValueAt(i, j, t);
			}
		}
		return ret;
	}
	
	public static HeightMap getSteepness(HeightMap h) {
		float[][] diffs = new float[h.getXSize()][h.getYSize()];
		float maxDiff = 0f;
		
		for(int i = 1; i < diffs.length - 1; i++) {
			for(int j = 1; j < diffs[0].length - 1; j++) {
				float[] nb = h.get4Nb(i, j);
				float dx = nb[0] - nb[2];
				float dy = nb[1] - nb[3];
				diffs[i][j] = Math.max(Math.abs(dx), Math.abs(dy));
				if(diffs[i][j] > maxDiff) maxDiff = diffs[i][j];
			}
		}
		for(int i = 1; i < diffs.length - 1; i++) {
			for(int j = 1; j < diffs[0].length - 1; j++) {
				diffs[i][j] /= maxDiff;
			}
		}
		return new HeightMap(diffs);
	}
}
