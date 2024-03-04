package generator.water.rivers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import data.WorldSettings;
import data.map.HeightMap;
import data.map.Mask;
import io.ImageIO;

public class RiverUtils {
	
	public static void bendRiver(HashSet<River> rivers, int scaling) {
		Iterator i = rivers.iterator();
		while(i.hasNext()) {
			River r = (River) i.next();
			bendRiver(r, scaling);
		}
	}
	
	public static void bendRiver(River r, int scaling) {
		float falloff = 64F;
		int edgeDetectDist = 32;
		ArrayList<int[]> riverPoints = r.getRiverpoints();
		if(riverPoints.size() < 8) return;
		float size = r.getSize() * scaling * r.getSize();
		size = Math.min(size, 32);
		size = Math.max(size, 4);
		edgeDetectDist = (int) size;
		ArrayList<int[]> samples = new ArrayList<>();
		samples.add(riverPoints.get(0));
		for(int i = 1; i < riverPoints.size() - 1; i++) {
			int[] p = riverPoints.get(i);
			
			int[] pp = riverPoints.get(i - 1);
			int[] np = riverPoints.get(i + 1);
			
			float allowed = (riverPoints.size() / 2) - Math.abs((riverPoints.size() / 2) - i);
			allowed /= falloff;
			allowed = Math.min(falloff, 1);
			allowed = Math.max(falloff, 0);
			
			if(i < riverPoints.size() - edgeDetectDist) {
				int[] ep = riverPoints.get(i + edgeDetectDist);
				int[] ev = {ep[0] - p[0], ep[1] - p[1]};
				int[] nv = {np[0] - p[0], np[1] - p[1]};
				
				int e = ev[0] * nv[0] + ev[1] * nv[1];
				float edgeMod = edgeDetectDist - e;
				edgeMod /= edgeDetectDist;
				edgeMod = 1 - edgeMod;
				allowed *= edgeMod;
			}
			allowed = Math.min(allowed, 1);
			allowed = Math.max(allowed, 0);
			float b1 = (float) (riverPoints.size() * Math.PI / size);
			b1 = size;
			float b2 = b1 * 0.05F * scaling;
			float offset = (float) (Math.sin(i / b1) * b2);
			offset *= allowed;
			int[] vp = {np[0] - pp[0], np[1] - pp[1]};
			int[] m = {vp[0] / 2, vp[1] / 2};
			m[0] += pp[0];
			m[1] += pp[1];
			float[] n = {-vp[1], vp[0]};
			float absn = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1]);
			n[0] /= absn;
			n[1] /= absn;
			n[0] *= offset;
			n[1] *= offset;
			
			int[] result = {(int) (m[0] + n[0]), (int) (m[1] + n[1])};
			samples.add(result);
		}
		samples.add(riverPoints.get(riverPoints.size() - 1));
		ArrayList<int[]> newP = new ArrayList<>();
		for(int i = 0; i < samples.size() - 1; i++) {
			int[] p1 = samples.get(i);
			int[] p2 = samples.get(i + 1);
			newP.add(p1);
			//newP.addAll(connectToPoint(p1[0], p1[1], p2[0], p2[1]));
		}
		newP.add(samples.get(samples.size() - 1));
		newP.removeAll(riverPoints);
		r.setRiverpoints(newP);
	}
	
	public static void smoothRivers(HashSet<River> rivers) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			smoothRiver(r);
		}
	}
	
	public static void smoothRiver(River r) {
		ArrayList<int[]> rp = r.getRiverpoints();
		float size = r.getSize();
		int[] ep = r.getEndPoint();
		Iterator it = rp.iterator();
		if(rp.size() < 7) return;
		for(int i = 3; i < rp.size() - 3; i+=2) {
			int[] p = rp.get(i);
			int[] pp = rp.get(i-1);
			int nx = 2 * p[0] - pp[0];
			int ny = 2 * p[1] - pp[1];
			int np[] = rp.get(i + 1);
			nx += np[0];
			ny += np[1];
			nx /= 2;
			ny /= 2;
			np[0] = nx;
			np[1] = ny;
		}
		fillHoles(r);
	}
	
	public static void naturalize(HashSet<River> rivers, int xSize, int ySize) {
		fixChildren(rivers, xSize, ySize);
		HashSet<River> startRivers = new HashSet<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			startRivers.add(r);
		}
		it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			startRivers.remove(r.getChild());
		}
		Mask m = riverMaskFromSet(startRivers, xSize, ySize);
		ImageIO.saveAsGreyImage(m, "Start Rivers");
	}
	
	public static void fixChildren(HashSet<River> rivers, int xSize, int ySize) {
		removeOutOfBounds(rivers, xSize, ySize);
		River[][] rlu = toRiverLookUp(rivers, xSize, ySize);
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			River rc = rlu[r.getEndPoint()[0]][r.getEndPoint()[1]];
			if(rc != null && rc != r.getChild()) {
				r.setChild(rc);
			}
		}
	}
	
	public static void removeOutOfBounds(HashSet<River> rivers, int xSize, int ySize) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			ArrayList<int[]> rp = r.getRiverpoints();
			ArrayList<int[]> toRemove = new ArrayList<>();
			Iterator ite = rp.iterator();
			while(ite.hasNext()) {
				int[] p = (int[]) ite.next();
				if(p[0] < 0 || p[0] >= xSize || p[1] < 0 || p[1] >= ySize) {
					toRemove.add(p);
				}
			}
			rp.removeAll(toRemove);
			r.setRiverpoints(rp);
		}
	}
	
	public static Mask swampDetect(HashSet<River> rivers, HeightMap world, float waterHeight) {
		return null;
	}
	
	public static Mask deltaDetect(HashSet<River> rivers, HeightMap world, float waterHeight, float minHeight, float maxHeight) {
		Mask deltaMask = new Mask(world.getXSize(), world.getYSize());
		
		HashSet<River> doneCs = new HashSet<>();
		HashSet<River> rwd = new HashSet<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			if(doneCs.contains(r)) continue;
			ArrayList<int[]> rp = r.getRiverpoints();
			int[] deltaStart = null;
			for(int i = 0; i < rp.size(); i++) {
				int[] c = rp.get(i);
				if(world.getValueAt(c[0], c[1]) < waterHeight -0.005f) {
					deltaStart = c;
					rwd.add(r);
					break;
				}
			}
			if(deltaStart != null) {
				int size = (int) r.getSize();
				size *= 4;
				int cappedSize = Math.min(world.getXSize() / 64, size);
				size = cappedSize + (size - cappedSize) / 64;
				for(int i = 0; i < 2 * size + 1; i++) {
					for(int j = 0; j < 2 * size + 1; j++) {
						int s = i - size;
						int t = j - size;
						
						if(s * s + t * t < size * size) {
							int x = (int) (deltaStart[0] - size + i);
							int y = (int) (deltaStart[1] - size + j);
							if(world.getValueAt(x, y) < maxHeight && world.getValueAt(x, y) > minHeight) {
								deltaMask.setValueAt(x, y, true);
							}
						}
					}
				}
			}
			//doneCs.add(r.getChild());
		}
		
		ImageIO.saveAsGreyImage(deltaMask, "deltaAreas");
		System.out.println("Deltadetection complete");
		
		HashMap<River, Integer> deltaStarts = new HashMap<>();
		it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			int startIndex = -1;
			ArrayList<int[]> rp = r.getRiverpoints();
			for(int t = 0; t < rp.size(); t++) {
				int[] c = rp.get(t);
				if(deltaMask.getValueAt(c[0], c[1])) {
					startIndex = t;
					break;
				}
			}
			if(startIndex != -1) {
				deltaStarts.put(r, startIndex);
			}
		}
		
		System.out.println("starting Deltashaping");
		
		Mask finalDeltaMask = new Mask(world.getXSize(), world.getYSize());
		it = deltaStarts.keySet().iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			ArrayList<int[]> rp = r.getRiverpoints();
			int startIndex = deltaStarts.get(r);
			//System.out.println(startIndex + "/" + rp.size() + "  " + r.getSize());
			if(startIndex != -1) {
				int deltaLength = rp.size() - startIndex;
				int size = (int) r.getSize();
				size *= 8;
				size += 32;
				size *= 4;
				size = Math.min(800, size);
				
				float angle = size;
				angle /= deltaLength;
				angle =  Math.min(angle, 0.8F);
				for(int w = startIndex; w < rp.size(); w+=4) {
					float offExact = angle * (w - startIndex);
					int off = (int) offExact;
					off = Math.min(off, (w - startIndex) / 2);
					int[] c = rp.get(w);
					
					for(int i = 0; i < 2 * off + 1; i++) {
						for(int j = 0; j < 2 * off + 1; j++) {
							int s = i - off;
							int t = j - off;
							
							if(s * s + t * t < off * off) {
								int x = (int) (c[0] - off + i);
								int y = (int) (c[1] - off + j);
								if(world.getValueAt(x, y) < maxHeight && world.getValueAt(x, y) > minHeight) {
									finalDeltaMask.setValueAt(x, y, true);
								}
							}
						}
					}
					
				}
			} else {
				System.out.println("Something is completely broken");
			}
		}
		System.out.println("finished deltaShaping");
		finalDeltaMask.and(deltaMask);
		return finalDeltaMask;
	}
	
	public static void removeZeros(HashSet<River> rivers) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			removeZeros(r);
		}
	}
	
	
	public static void removeZeros(River r) {
		ArrayList<int[]> rp = r.getRiverpoints();
		ArrayList<Integer> remove = new ArrayList<>();
		for(int i = 0; i < rp.size(); i++) {
			int[] p = rp.get(i);
			if(p[0] == 0 && p[1] == 0) {
				remove.add(i);
			}
		}
		for(int i = 0; i < remove.size(); i++) {
			int p = remove.get(i) - i;
			rp.remove(p);
		}
	}
	
	public static void removeDoubles(HashSet<River> rivers) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			removeDoubles(r);
		}
	}
	
	
	public static void removeDoubles(River r) {
		ArrayList<int[]> rp = r.getRiverpoints();
		ArrayList<Integer> remove = new ArrayList<>();
		for(int i = 0; i < rp.size() - 1; i++) {
			int[] p = rp.get(i);
			int[] np = rp.get(i + 1);
			if(p[0] == np[0] && p[1] == np[1]) {
				remove.add(i);
			}
		}
		for(int i = 0; i < remove.size(); i++) {
			int p = remove.get(i) - i;
			rp.remove(p);
		}
	}
	
	public static Mask riverMaskFromSet(HashSet<River> rivers, int xSize, int ySize) {
		Mask ret = new Mask(xSize, ySize);
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			for(int i = 0; i < r.getRiverpoints().size(); i++) {
				int[] c = r.getRiverpoints().get(i);
				ret.setValueAt(c[0], c[1], true);
			}
		}
		return ret;
	}
	
	public static float[][] createFlowMap(HashSet<River> rivers, int xSize, int ySize) {
		float[][] ret = new float[xSize][ySize];
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			Iterator ite = r.getRiverpoints().iterator();
			while(ite.hasNext()) {
				int[] coords = (int[]) ite.next();
				if(isInBounds(coords, xSize, ySize)) {
					ret[coords[0]][coords[1]] += r.getSize();
				}
			}
		}
		return ret;
	}
	
	public static boolean isInBounds(int[] c, int xSize, int ySize) {
		return c[0] >= 0 && c[1] >= 0 && c[0] < xSize && c[1] < ySize;
	}
	
	public static void fillHoles(HashSet<River> rivers) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			fillHoles(r);
		}
	}
	
	public static void fillHoles(River river) {
		if(river.getRiverpoints().size() <= 1) return;
		ArrayList<int[]> riverPoints = river.getRiverpoints();
		ArrayList<int[]> newPoints = new ArrayList<>();
		for(int i = 0; i < riverPoints.size() - 1; i++) {
			int[] p = riverPoints.get(i);
			int[] np = riverPoints.get(i + 1);
			newPoints.addAll(connectToPoint(p[0], p[1], np[0], np[1]));
		}
		river.setRiverpoints(newPoints);
	}
	
	public static void snapRepeat(HashSet<River> rivers, int xSize, int ySize, int magR, int iterations) {
		for(int i = 0;  i< iterations; i++) {
			snap(rivers, xSize, ySize, magR);
		}
	}
	
	public static void biggenRivers(HashSet<River> rivers, int factor) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			biggenRiver(r, factor);
		}
	}
	
	public static void biggenRiver(River r, int factor) {
		if(r.getRiverpoints().size() > 1) {
			ArrayList<int[]> riverPoints = r.getRiverpoints();
			ArrayList<int[]> ret = new ArrayList<>();
			for(int i = 0; i < riverPoints.size() - 1; i++) {
				int[] p = riverPoints.get(i);
				int[] np = riverPoints.get(i + 1);
				
				int[] p1 = new int[2];
				int[] np1 = new int[2];
				
				p1[0] = p[0] * factor;
				p1[1] = p[1] * factor;
				np1[0] = np[0] * factor;
				np1[1] = np[1] * factor;
				
				ret.add(p1);
				
				ret.addAll(connectToPoint(p1[0], p1[1], np1[0], np1[1]));
			}
			//System.out.print(ret.size() + "/" + riverPoints.size() + "\t");
			int[] last = riverPoints.get(riverPoints.size() - 1);
			last[0] *= factor;
			last[1] *= factor;
			ret.add(last);
			r.setRiverpoints(ret);
		} else if(r.getRiverpoints().size() == 1) {
			int[] p = r.getRiverpoints().get(0);
			p[0] *= factor;
			p[1] *= factor;
			r.getRiverpoints().remove(0);
			r.getRiverpoints().add(p);
		} else {
			return;
		}
	}
	
	public static void fixRiverConnection(HashSet<River> rivers) {
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			fixRiverConnection(r);
		}
	}
	
	public static void fixRiverConnection(River r) {
		if(r.getChild() != null) {
			int index = Math.min(2, r.getChild().getRiverpoints().size() - 1);
			int[] c = r.getChild().getRiverpoints().get(index);
			int[] p = r.getRiverpoints().get(r.getRiverpoints().size() - 1);
			if(c[0] < 2048) {
				//System.out.println("ähem");
			}
			r.getRiverpoints().addAll(connectToPoint(p[0], p[1], c[0], c[1]));
		}
	}
	
	public static void polate(River r, int factor) {
		ArrayList<int[]> rp = r.getRiverpoints();
		ArrayList<int[]> nrp = new ArrayList<>();
		for(int i = 0; i < rp.size(); i+=factor) {
			nrp.add(rp.get(i));
		}
		nrp.add(rp.get(rp.size() - 1));
		r.setRiverpoints(nrp);
		fillHoles(r);
	}
	
	public static Mask toBiggerMask(HashSet<River> rivers, int xSize, int ySize, int magnification) {
		Mask ret = new Mask(xSize, ySize);
		boolean pointsOnly = false;
		BufferedImage buimg = new BufferedImage(ret.getXSize(), ret.getYSize(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = buimg.createGraphics();
		g.setBackground(Color.BLACK);
		g.setColor(Color.WHITE);
		
		int offset = (int) (magnification * 0.5F);
		System.out.println("Biggen " + rivers.size() + " rivers");
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River river = (River) it.next();
			ArrayList<int[]> coords = river.getRiverpoints();
			int prevX = -1;
			int prevY = -1;
			
			Iterator ite = coords.iterator();
			while(ite.hasNext()) {
				int[] c = (int[]) ite.next();
				int x = c[0];
				int y = c[1];
				x *= magnification; 
				y *= magnification;
				x += offset;
				y += offset;
				
				if(prevX != -1) {
					if(pointsOnly) {
						g.drawRect(prevX, prevY, 1, 1);
					} else {
						g.drawLine(prevX, prevY, x, y);
					}
				}
				
				prevX = x;
				prevY = y;
			}
			
			River child = river.getChild();
			if(child != null && prevX != -1) {
				int[] start = child.getStartPoint();
				int cx = start[0] * magnification + offset;
				int cy = start[1] * magnification + offset;
				g.drawLine(prevX, prevY, cx, cy);
			}
			//System.out.println(river.getSize() + " / " + coords.size());
		}
		
		for(int i = 0; i < ret.getXSize(); i++) {
			for(int j = 0; j < ret.getXSize(); j++) {
				Color c = new Color(buimg.getRGB(i, j));
				if(c.getBlue() > 127) {
					ret.setValueAt(i, j, true);
				}
			}
		}
		return ret;
	}
	
	public static void snap(HashSet<River> rivers, int xSize, int ySize, int magR) {
		HashSet<River> newRivers = new HashSet<>();
		River[][] rlu = toRiverLookUp(rivers, xSize, ySize);
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			River foundRiver = null;
			ArrayList<int[]> rp = r.getRiverpoints();
			for(int i = 0; i < rp.size(); i++) {
				int[] p = rp.get(i);
				boolean rf = false;
				int xf = -1;
				int yf = -1;
				for(int sq = 1; sq <= magR; sq++) {
					for(int t = 0; t < 8*sq; t++) { 
						int x = 0;
						int y = 0;
						if(t < 2*sq) {
							x = t - sq;
							y = -sq;
						} else if(t < 4*sq) {
							x = sq;
							y = (t % 2*sq) - sq;
						} else if(t < 6*sq) {
							x = sq - (t % 2*sq);
							y = sq;
						} else if(t < 8*sq) {
							x = -sq;
							y = sq - (t % 2*sq);
						}
						int px = p[0] + x;
						int py = p[1] + y;
						River riv = px >= 0 && py >= 0 && px < rlu.length && py < rlu[0].length ? rlu[px][py] : null;
						if(riv == null) continue;
						if(riv != r) {
							if(!riv.isAbove(r)) {
								rf = true;
								//System.out.println("Found another River at " + px + "/" + py);
								xf = px;
								yf = py;
								foundRiver = riv;
								break;
							}
						}
						if(rf) break;
					}
					if(rf) break;
				}
				if(rf) {
					ArrayList<int[]> newRp = new ArrayList<>();
					newRp.addAll(rp.subList(0, i));
					newRp.addAll(connectToPoint(p[0], p[1], xf, yf));
					if(r.getChild() != null) r.getChild().addFlow(-r.getSize());
					River fc = foundRiver.split(xf, yf, r.getSize());
					r.setChild(fc);
					r.setRiverpoints(newRp);
					if(fc != foundRiver) newRivers.add(fc);
					break;
				}
			}
		}
		rivers.addAll(newRivers);
	}
	
	public static void checkRivers(HashSet<River> rivers, HeightMap hm) {
		HashSet<River> remove = new HashSet<River>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			if(r.getChild() == null && hm.getValueAt(r.getEndPoint()[0], r.getEndPoint()[1]) > WorldSettings.waterHeightUp - 0.05f) {
				remove.add(r);
			}
		}
		rivers.removeAll(remove);
	}
	
	public static void shorten(HashSet<River> rivers, int xSize, int ySize, int magR) {
		HashSet<River> newRivers = new HashSet<>();
		Iterator it = rivers.iterator();
		int counter = 0;
		while(it.hasNext()) {
			River r = (River) it.next();
			River foundRiver = null;
			ArrayList<int[]> rp = r.getRiverpoints();
			for(int i = 0; i < rp.size(); i++) {
				int[] p = rp.get(i);
				boolean rf = false;
				boolean sf = false;
				int hitIndex = -1;
				int xf = -1;
				int yf = -1;
				int sq = magR;
				for(int t = 0; t < 8*sq; t++) { 
					int x = 0;
					int y = 0;
					if(t < 2*sq) {
						x = t - sq;
						y = -sq;
					} else if(t < 4*sq) {
						x = sq;
						y = (t % 2*sq) - sq;
					} else if(t < 6*sq) {
						x = sq - (t % 2*sq);
						y = sq;
					} else if(t < 8*sq) {
						x = -sq;
						y = sq - (t % 2*sq);
					}
					int px = p[0] + x;
					int py = p[1] + y;
					 
					int ni = r.indexOf(px, py);
					if(ni > -1 && (ni - i) > (2 * sq + 1)) {
						System.out.println("Found myself at " + px + "/" + py + "  shorting " + (ni - i) + " steps    " + counter + " / " + rivers.size());
						sf = true;
						hitIndex = ni;
						xf = px;
						yf = py;
						break;
					}
					if(sf) break;
				}
				if(sf) break;
				if(sf) {
					ArrayList<int[]> newRp = new ArrayList<>();
					newRp.addAll(rp.subList(0, i));
					newRp.addAll(connectToPoint(p[0], p[1], xf, yf));
					newRp.addAll(rp.subList(hitIndex, rp.size()));
					r.setRiverpoints(newRp);
					i = hitIndex;
					rp = newRp;
				}
			}
			counter++;
		}
		rivers.addAll(newRivers);
	}
	
	public static ArrayList<int[]> connectToPoint(int px, int py, int x1, int y1) {
		ArrayList<int[]> ret = new ArrayList<>();
		int[] p = {px, py};
		int[] np = {x1, y1};
		int[] pnp = {np[0] - p[0], np[1] - p[1]};
		//if(px < 2048 || x1 < 2048) System.out.println("connecting " + px + "/" + py + "  with  " + x1 + "/" + y1);
		float l = Math.max(Math.abs(pnp[0]), Math.abs(pnp[1]));
		float[] nV = {pnp[0] / l, pnp[1] / l};
		for(int i = 0; i <= l; i++) {
			float x = i * nV[0];
			float y = i * nV[1];
			int ix = (int) x;
			int iy = (int) y;
			int[] c = {p[0] + ix, p[1] + iy};
			ret.add(c);
		}
		return ret;
	}
	
	public static HashSet<River> sliceNDice(HashSet<River> rivers, int xSize, int ySize) {
		River[][] rlu = toRiverLookUp(rivers, xSize, ySize);
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			ArrayList<int[]> rp = r.getRiverpoints();
		}
		//TODO
		return null;
	}
	
	public static HashSet<River> compactify(HashSet<River> rivers, int iterations) {
		for(int i = 0; i < iterations; i++) {
			rivers = compactify(rivers);
		}
		return rivers;
	}
	
	public static HashSet<River> compactify(HashSet<River> rivers) {
		HashMap<River, Integer> tributairies= new HashMap<>(2 * rivers.size());
		HashSet<River> r2 = new HashSet<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			r2.add((River) it.next());
		}
		it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			int hits = 0;
			Iterator ite = r2.iterator();
			while(ite.hasNext()) {
				River ri = (River) ite.next();
				if(ri.getChild() == r) {
					hits += 1;
				}
			}
			tributairies.put(r, hits);
		}
		it = rivers.iterator();
		HashSet<River> toRemove = new HashSet<>();
		HashSet<River> toAdd = new HashSet<>();
		while(it.hasNext()) {
			River r = (River) it.next();
			if(tributairies.get(r) == 1 && !toRemove.contains(r)) {
				River tributairy = null;
				River mergedRiver = null;
				Iterator ite = rivers.iterator();
				while(ite.hasNext()) {
					River ri = (River) ite.next();
					if(ri.getChild() == r) {
						tributairy = ri;
						toRemove.add(ri);
						toRemove.add(r);
						mergedRiver = mergeRivers(ri, r);
						toAdd.add(mergedRiver);
						break;
					}
				}
				ite = rivers.iterator();
				while(ite.hasNext()) {
					River ri = (River) ite.next();
					if(ri.getChild() == tributairy && tributairy != null) {
						ri.setChild(mergedRiver);
					}
				}
			}
		}
		rivers.removeAll(toRemove);
		rivers.addAll(toAdd);
		return rivers;
	}
	
	
	//A flows into B
	public static River mergeRivers(River a, River b) {
		ArrayList<int[]> rp = new ArrayList<>();
		Iterator it = a.getRiverpoints().iterator();
		while(it.hasNext()) {
			int[] p = (int[]) it.next();
			int[] t = {p[0], p[1]};
			rp.add(t);
		}
		it = b.getRiverpoints().iterator();
		while(it.hasNext()) {
			int[] p = (int[]) it.next();
			int[] t = {p[0], p[1]};
			rp.add(t);
		}
		River ret = new River(b.getSize(), rp.get(0)[0], rp.get(0)[1], rp.get(rp.size() - 1)[0], rp.get(rp.size() - 1)[1]);
		ret.setRiverpoints(rp);
		ret.setChild(b.getChild());
		return ret;
	}
	
	
	//Fast inverse sqrt https://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
	public static float invSqrt(float x) {
	    float xhalf = 0.5f * x;
	    int i = Float.floatToIntBits(x);
	    i = 0x5f3759df - (i >> 1);
	    x = Float.intBitsToFloat(i);
	    x *= (1.5f - xhalf * x * x);
	    return x;
	}
	
	public static River[][] toRiverLookUp(HashSet<River> rivers, int xSize, int ySize) {
		River[][] ret = new River[xSize][ySize];
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			for(int i = 0; i < r.getRiverpoints().size(); i++) {
				int[] c = r.getRiverpoints().get(i);
				try {
					ret[c[0]][c[1]] = r;
				} catch(ArrayIndexOutOfBoundsException e) {
					
				}
			}
		}
		return ret;
	}
	
	public static void saveRiverImage(HashSet<River> rivers, int xSize, int ySize, String name) {
		River[][] rlu = toRiverLookUp(rivers, xSize, ySize);
		HashMap<River, Color> clt = new HashMap<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			Color c = new Color((float) Math.random(), (float) Math.random(), (float) Math.random());
			clt.put(r, c);
		}
		BufferedImage img = new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_BGR);
		Graphics g = img.getGraphics();
		for(int i = 0; i < xSize; i++) {
			for(int j = 0; j < ySize; j++) {
				if(rlu[i][j] == null) {
					g.setColor(Color.BLACK);
				} else {
					g.setColor(clt.get(rlu[i][j]));
				}
				g.drawRect(i, j, 1, 1);
			}
		}
		ImageIO.saveImage(img, name);
	}
}
