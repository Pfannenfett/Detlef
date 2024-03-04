package generator.height;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import data.World;
import data.WorldSettings;
import data.biomes.Biome;
import data.map.HeightMap;
import data.map.Mask;
import data.map.RegionMap;
import data.map.layout.CellularLayoutMap;
import data.util.MapUtils;
import generator.erosion.ErosionGenerator;
import generator.erosion.ThermalWeatheringGenerator;
import generator.noise.NoiseGenerator;
import generator.water.rivers.River;
import generator.water.rivers.RiverInfo;
import io.ImageIO;

public class TerrainGeneratorMountains implements ITerrainGenerator {
	private static final String identifier = "Mountains";
	
	@Override
	public boolean isLowerTerrain() {
		return false;
	}
	
	@Override
	public HeightMap generateTerrain(CellularLayoutMap clm, HeightMap layout, World world, long seed) {
		boolean switcha = true;
		if(switcha) {
			return new HeightMap(layout.getXSize() * WorldSettings.layoutRatio, layout.getYSize() * WorldSettings.layoutRatio);
		}
		ArrayList<Biome> biomes = clm.getBiomesForTerrainGenerator(this);
		
		Mask terraGenMask = clm.getMaskForBiomes(biomes);
		
		HeightMap tempMap = terraGenMask.toHeightMap(1f, 0f);
		MapUtils.repeatedConvSmooth(tempMap, 6, 2);
		
		Mask smallTerraGenMask = tempMap.above(0.97f);
		Mask nonSourceRivers = new Mask(layout.getXSize(), layout.getYSize());
		RiverInfo rinf = clm.getRiverInfo();
		ArrayList<int[]> exitPoints = new ArrayList<>();
		HashSet<River> rivers = rinf.getRivers();
		ArrayList<River> passingRivers = new ArrayList<>();
		Iterator it = rivers.iterator();
		while(it.hasNext()) {
			River r = (River) it.next();
			
			ArrayList<int[]> rp = r.getRiverpoints();
			int[] sp = rp.get(0);
			
			if(!terraGenMask.getValueAt(sp[0], sp[1]) || passingRivers.contains(r)) {
				if(passingRivers.contains(r)) {
					passingRivers.remove(r);
				}
				for(int i = 0; i < rp.size(); i++) {
					int[] p = rp.get(i);
					nonSourceRivers.setValueAt(p[0], p[1], true);
				}
				int[] ep = rp.get(rp.size() - 1);
				if(terraGenMask.getValueAt(ep[0], ep[1])) {
					passingRivers.add(r.getChild());
				}
				continue;
			}
			
			int i = 0;
			int x = r.getStartPoint()[0];
			int ox = -1;
			int oy = -1;
			int y = r.getStartPoint()[1];
			while(terraGenMask.getValueAt(x, y)) {
				ox = x;
				oy = y;
				i += 1;
				if(i >= rp.size()) break;
				int[] np = rp.get(i);
				x = np[0];
				y = np[1];
			}
			if(i >= rp.size()) continue;
			if(i > 0 && ox != -1 && oy != -1) {
				int[] c = {ox, oy};
				exitPoints.add(c);
			}
		}
		
		for(int i = 0; i < passingRivers.size(); i++) {
			River r = passingRivers.get(i);
			ArrayList<int[]> rp = r.getRiverpoints();
			
			for(int j = 0; j < rp.size(); j++) {
				int[] p = rp.get(j);
				nonSourceRivers.setValueAt(p[0], p[1], true);
			}
			int[] ep = rp.get(rp.size() - 1);
			if(terraGenMask.getValueAt(ep[0], ep[1])) {
				passingRivers.add(r.getChild());
			}
			continue;
		}
		
		Mask ePM = new Mask(layout.getXSize(), layout.getYSize());
		for(int i = 0; i < exitPoints.size(); i++) {
			int[] c = exitPoints.get(i);
			ePM.setValueAt(c[0], c[1], true);
		}
		
		
		RegionMap rm = RegionMap.voronoi(exitPoints, layout.getXSize(), layout.getYSize());
		ImageIO.saveAsImage(rm, "Regions");
		ImageIO.saveAsGreyImage(ePM, "endpoints");
		ImageIO.saveAsGreyImage(nonSourceRivers, "Nonsourcerivers");
		
		HeightMap nsr = nonSourceRivers.toHeightMap(1f, 0f);
		MapUtils.convSmooth(nsr, 3);
		nonSourceRivers = nsr.above(0.01f);
		nonSourceRivers.invert();
		terraGenMask.and(nonSourceRivers);
		
		
		Random rng = new Random(seed);
		
		int spacing = 12;
		
		ArrayList<ArrayList<int[]>> points = new ArrayList<>();
		int numberOfStartPoints = layout.getXSize() * layout.getYSize() / (spacing * spacing);
		for(int i = 0; i < numberOfStartPoints; i++) {
			int x = rng.nextInt(spacing) + spacing * (i % (layout.getXSize() / spacing));
			int y = rng.nextInt(spacing) + spacing * (i / (layout.getXSize() / spacing));
			if(!smallTerraGenMask.getValueAt(x, y)) continue;
			
			int[] target = rm.getValueAt(x, y);
			ArrayList<int[]> path = new ArrayList<>();
			int[] sp = {x, y};
			path.add(sp);
			
			boolean shortcut = false;
			boolean targetChange = false;
			
			while(x != target[0] || y != target[1]) {
				
				if(!terraGenMask.getValueAt(x, y)) break;
				int xd = target[0] - x;
				int yd = target[1] - y;
				
				byte xs = (byte) (xd < 0 ? -1 : 1);
				byte ys = (byte) (yd < 0 ? -1 : 1);
				
				xd *= xs;
				yd *= ys;
				
				int dcm = rng.nextInt(xd + yd);
				if(dcm < xd) {
					x += xs;
				} else {
					y += ys;
				}
				
				if(!shortcut) {
					int scanrange = 6;
					int mds = 2 * scanrange * scanrange;
					for(int rx = -scanrange; rx < scanrange; rx++) {
						for(int ry = -scanrange; ry < scanrange; ry++) {
							if(ePM.getValueAt(x + rx, y + ry) && rm.getValueAt(x + rx, y + ry).equals(target)) {
								int ds = rx * rx + ry * ry;
								if(ds < mds) {
									targetChange |= !(target[0] ==  x + rx && target[1] == y + ry);
									target[0] = x + rx;
									target[1] = y + ry;
									mds = ds;
								}
								shortcut = true;
							}
						}
					}
				}
				
				int[] p = {x, y};
				if(ePM.getValueAt(x, y)) break;
				path.add(p);
				
			}
			
			
			
			for(int j = 0; j < path.size(); j++) {
				int[] p = path.get(j);
				ePM.setValueAt(p[0], p[1], terraGenMask.getValueAt(p[0], p[1]));
			}
			if(!targetChange) {
				path.add(target);
			}
			if(path.size() > 0) points.add(path);
		}
		
		ImageIO.saveAsGreyImage(ePM, "valleyMask");
		
		HeightMap geoDist = MapUtils.geoDistAboveHeight(terraGenMask.toHeightMap(1f, 0f), 0f, 64);
		HeightMap maxGeoDist = MapUtils.geoDistAboveHeight(terraGenMask.toHeightMap(1f, 0f), 0f, 32);
		HeightMap maxValleyBase = NoiseGenerator.createHillsNoiseMap(layout.getXSize(), layout.getYSize(), seed + 2, 4);
		maxValleyBase = new HeightMap(MapUtils.cappedMultiply(maxValleyBase, geoDist));
		ImageIO.saveAsGreyImage(maxValleyBase, "maxValleyBase");
		
		HeightMap valleyBase = new HeightMap(layout.getXSize(), layout.getYSize());
		boolean[] finishedIndexes = new boolean[points.size()];
		int performedFirstValleyPass = 0;
		for(int i = 0; i < points.size(); i++) {
			ArrayList<int[]> path = points.get(i);
			int[] ep = path.get(path.size() - 1);
			if(!exitPoints.contains(ep)) {
				continue;
			}
			
			float h = 0.01f;
			for(int j = path.size() - 1; j >= 0; j--) {
				int[] p = path.get(j);
				h = Math.max(h, maxValleyBase.getValueAt(p[0], p[1]));
				h = Math.min(h, maxGeoDist.getValueAt(p[0], p[1]) + 0.01f);
				valleyBase.setValueAt(p[0], p[1], h);
			}
			
			performedFirstValleyPass++;
			finishedIndexes[i] = true;
		}
		
		ImageIO.saveAsGreyImage(valleyBase, "ValleyBaseLines1");
		System.out.println("Did first Valleyheight Pass on " + performedFirstValleyPass + " Valleys");
		
		boolean skipped = true;
		
		
		while(skipped) {
			skipped = false;
			for(int i = 0; i < points.size(); i++) {
				ArrayList<int[]> path = points.get(i);
				int[] ep = path.get(path.size() - 1);
				if(exitPoints.contains(ep) || finishedIndexes[i]) continue;
				
				boolean[] nb = ePM.get8NB(ep[0], ep[1]);
				int nbs = 0;
				float h = 0f;
				
				for(byte p = 0; p < 8; p++) {
					if(nb[p]) {
						nbs++;
						byte[] bt = MapUtils.decomposeDirection(p);
						h = Math.max(h, valleyBase.getValueAt(ep[0] + bt[0], ep[1] + bt[1]));
					}
				}
				if(h < 0.002f && nbs > 0) {
					skipped = true;
					continue;
				}
				
				
				if(nbs > 3) {
					float f = rng.nextFloat();
					f *= geoDist.getValueAt(ep[0], ep[1]) - h;
					h += f;
				}
				
				for(int j = path.size() - 1; j >= 0; j--) {
					int[] p = path.get(j);
					h = Math.max(h, maxValleyBase.getValueAt(p[0], p[1]));
					h = Math.min(h, maxGeoDist.getValueAt(p[0], p[1]));
					valleyBase.setValueAt(p[0], p[1], h);
					
				}
				finishedIndexes[i] = true;
			}
		}
		
		for(int k = 0; k < 1; k++) {
			for(int i = 0; i < points.size(); i++) {
				ArrayList<int[]> path = points.get(i);
				
				float h = 1f;
				for(int j = 0; j < path.size(); j++) {
					int[] p = path.get(j);
					h = Math.min(h, valleyBase.getValueAt(p[0], p[1]));
					//valleyBase.setValueAt(p[0], p[1], h);
				}
			}
		}
		
		ImageIO.saveAsGreyImage(valleyBase, "ValleyBaseLines");
		
		
		for(int i = 0; i < valleyBase.getXSize(); i++) {
			for(int j = 0; j < valleyBase.getYSize(); j++) {
				if(!terraGenMask.getValueAt(i, j)) {
					valleyBase.setValueAt(i, j, 0f);
				} else if(valleyBase.getValueAt(i, j) == 0f) {
					valleyBase.setUncappedValueAt(i, j, -1f);
				}
			}
		}
		
		HeightMap temp1 = new HeightMap(layout.getXSize(), layout.getYSize());
		for(int i = 0; i < valleyBase.getXSize(); i++) {
			for(int j = 0; j < valleyBase.getYSize(); j++) {
				if(valleyBase.getValueAt(i, j) == -1f) {
					float[] nb = valleyBase.get4Nb(i, j);
					float total = 0f;
					byte found = 0;
					for(float pnb : nb) {
						if(pnb >= 0f){
							total = Math.max(total, pnb);
							found++;
						}
						
					}
					if(found != 0) {
						//temp.setUncappedValueAt(i, j, Math.min((total / found), maxValleyBase.getValueAt(i, j)) + 1f);
						temp1.setUncappedValueAt(i, j, total + 1f);
					}
				}
			}
		}
		
		for(int i = 0; i < valleyBase.getXSize(); i++) {
			for(int j = 0; j < valleyBase.getYSize(); j++) {
				valleyBase.setUncappedValueAt(i, j, valleyBase.getValueAt(i, j) + temp1.getValueAt(i, j));
			}
		}
		
		
		int counter = 0;
		boolean finished = false;
		while(!finished) {
			finished = true;
			HeightMap temp = new HeightMap(layout.getXSize(), layout.getYSize());
			for(int i = 0; i < valleyBase.getXSize(); i++) {
				for(int j = 0; j < valleyBase.getYSize(); j++) {
					if(valleyBase.getValueAt(i, j) == -1f) {
						float[] nb = valleyBase.get4Nb(i, j);
						float total = 0f;
						byte found = 0;
						for(float pnb : nb) {
							if(pnb >= 0f){
								total = Math.max(total, pnb);
								found++;
							}
							
						}
						if(found == 0) {
							finished = false;
						} else {
							//temp.setUncappedValueAt(i, j, Math.min((total / found), maxValleyBase.getValueAt(i, j)) + 1f);
							total = Math.min(total, maxGeoDist.getValueAt(i, j));
							temp.setUncappedValueAt(i, j, total + 1f);
						}
					}
				}
			}
			
			for(int i = 0; i < valleyBase.getXSize(); i++) {
				for(int j = 0; j < valleyBase.getYSize(); j++) {
					valleyBase.setUncappedValueAt(i, j, valleyBase.getValueAt(i, j) + temp.getValueAt(i, j));
				}
			}
			System.out.print("-");
			counter++;
			if(counter > 1024) break;
		}
		System.out.println();
		
		
		ImageIO.saveAsGreyImage(valleyBase, "ValleyBase");
		ImageIO.saveAsGreyImage(terraGenMask, "TerraGenMask");
		
		Mask done = new Mask(layout.getXSize(), layout.getYSize());
		ArrayList<int[]> submapcoords = new ArrayList<>();
		ArrayList<Mask> submapmasks = new ArrayList<>();
		
		int boundary = 16;
		
		for(int i = 0; i < layout.getXSize(); i++) {
			for(int j = 0; j < layout.getYSize(); j++) {
				if(terraGenMask.getValueAt(i, j) && !done.getValueAt(i, j)) {
					
					int maxX = i;
					int maxY = j;
					int minX = i;
					int minY = j;
					
					ArrayList<int[]> todo = new ArrayList<>();
					int[] sp = {i, j};
					todo.add(sp);
					done.setValueAt(sp[0], sp[1], true);
					Mask smm = new Mask(layout.getXSize(), layout.getYSize());
					smm.setValueAt(sp[0], sp[1], true);
					ArrayList<int[]> tempdo = new ArrayList<>();
					while(!todo.isEmpty()) {
						for(int t = 0; t < todo.size(); t++) {
							int[] p = todo.get(t);
							
							if(p[0] < minX) minX = p[0];
							if(p[1] < minY) minY = p[1];
							if(p[0] > maxX) maxX = p[0];
							if(p[1] > maxY) maxY = p[1];
							
							
							int[] xp = {p[0] + 1, p[1]};
							int[] yp = {p[0], p[1] + 1};
							int[] xm = {p[0] - 1, p[1]};
							int[] ym = {p[0], p[1] - 1};
							if(terraGenMask.getValueAt(xp[0], xp[1]) && !done.getValueAt(xp[0], xp[1])) {
								tempdo.add(xp);
								done.setValueAt(xp[0], xp[1], true);
								smm.setValueAt(xp[0], xp[1], true);
							}
							if(terraGenMask.getValueAt(yp[0], yp[1]) && !done.getValueAt(yp[0], yp[1])) {
								tempdo.add(yp);
								done.setValueAt(yp[0], yp[1], true);
								smm.setValueAt(yp[0], yp[1], true);
							}
							if(terraGenMask.getValueAt(xm[0], xm[1]) && !done.getValueAt(xm[0], xm[1])) {
								tempdo.add(xm);
								done.setValueAt(xm[0], xm[1], true);
								smm.setValueAt(xm[0], xm[1], true);
							}
							if(terraGenMask.getValueAt(ym[0], ym[1]) && !done.getValueAt(ym[0], ym[1])) {
								tempdo.add(ym);
								done.setValueAt(ym[0], ym[1], true);
								smm.setValueAt(ym[0], ym[1], true);
							}
						}
						
						todo = tempdo;
						tempdo = new ArrayList<>();
					}
					
					int[] smc = {minX - boundary, minY - boundary, maxX + boundary, maxY + boundary};
					submapcoords.add(smc);
					submapmasks.add(smm);
				}
			}
		}
		
		
		System.out.println("Starting Mountain Terrain Generation");
		ArrayList<HeightMap> submaps = new ArrayList<>();
		for(int i = 0; i < submapcoords.size(); i++) {
			int[] smc = submapcoords.get(i);
			if(smc[2] - smc[0] == 0 || smc[3] - smc[1] == 0) {
				submaps.add(null);
				continue;
			}
			Mask smm = MapUtils.submap(submapmasks.get(i), smc[0], smc[1], smc[2] - smc[0], smc[3] - smc[1]);
			Mask smtgm = MapUtils.submap(terraGenMask, smc[0], smc[1], smc[2] - smc[0], smc[3] - smc[1]);
			Mask smepm = MapUtils.submap(ePM, smc[0], smc[1], smc[2] - smc[0], smc[3] - smc[1]);
			HeightMap smvb = new HeightMap(MapUtils.submap(valleyBase, smc[0], smc[1], smc[2] - smc[0], smc[3] - smc[1]));
			
			smtgm.and(smm);
			smepm.and(smm);
			smvb.applyMask(smm);
			
			HeightMap submap = createMountainSubMap(clm, seed, smtgm, smepm, smvb);
			submaps.add(submap);
			ImageIO.saveAsGreyImage(submap, "Mountain Submap " + i);
		}
		
		HeightMap ret = new HeightMap(layout.getXSize() * WorldSettings.layoutRatio, layout.getYSize() * WorldSettings.layoutRatio);
		
		for(int i = 0; i < submaps.size(); i++) {
			HeightMap h = submaps.get(i);
			int[] smc = submapcoords.get(i);
			if(h == null) continue;
			h.shift(smc[0] * WorldSettings.layoutRatio, smc[1] * WorldSettings.layoutRatio);
			ret = ret.max(h);
			h.delete();
		}
		
		HeightMap bl = clm.getTerracedLayout();
		MapUtils.biggen(bl, WorldSettings.layoutRatio);
		MapUtils.repeatedConvSmooth(bl, 4, 4);
		//ImageIO.saveAsGreyImage(bl, "Mountain baselevel");
		ret.mult(1f - bl.getMax());
		for(int i = 0; i < ret.getXSize(); i++) {
			for(int j = 0; j < ret.getYSize(); j++) {
				bl.setValueAt(i, j, ret.getValueAt(i, j) + bl.getValueAt(i, j));
			}
		}
		bl.cap();
		ret.delete();
		valleyBase = terraGenMask.toHeightMap(1f, 0f);
		MapUtils.convSmooth(valleyBase, 4);
		terraGenMask = valleyBase.above(0.01f);
		valleyBase = terraGenMask.toHeightMap(1f, 0f);
		MapUtils.convSmooth(valleyBase, 4);
		MapUtils.smoothBiggen(valleyBase, WorldSettings.layoutRatio);
		
		bl = new HeightMap(MapUtils.cappedMultiply(bl, valleyBase));
		//ImageIO.saveAsGreyImage(bl, "Mountains");
		
		return bl;
	}
	
	public static HeightMap createMountainSubMap(CellularLayoutMap clm, long seed, Mask terraGenMask, Mask ePM, HeightMap valleyBase) {
		
		System.out.println("Creating Mountain Submap of Size " + terraGenMask.getXSize() + "/" + terraGenMask.getYSize());
		HeightMap ret = terraGenMask.toHeightMap(0.65f, 0f);
		System.out.println("  created Mask 1/3");
		
		Mask riverMaskSmall = ePM;
		HeightMap hrm = riverMaskSmall.toHeightMap(1f, 0f);
		MapUtils.smoothBiggen(hrm, WorldSettings.layoutRatio);
		Mask riverMaskLarge = hrm.above(0.2f);
		hrm = null;
		MapUtils.widen(riverMaskLarge, 6);
		System.out.println("  created Mask 2/3  " + riverMaskLarge.getXSize());
		
		MapUtils.smoothBiggen(ret, WorldSettings.layoutRatio);
		riverMaskLarge.invert();
		ret.applyMask(riverMaskLarge, 0.004f);
		Mask peaks = riverMaskLarge.clone();
		System.out.println("  created Mask 3/3");
		
		HeightMap tgm = terraGenMask.toHeightMap(1f, 0f);
		MapUtils.smoothBiggen(tgm, WorldSettings.layoutRatio);
		Mask terraGenMaskLarge = tgm.above(0.5f);
		tgm = null;
		peaks.and(terraGenMaskLarge);
		System.out.println("  created Mountain Base");
		
		HeightMap MountainNoise = NoiseGenerator.createMountainNoiseMap(terraGenMask.getXSize() * WorldSettings.layoutRatio / 4, terraGenMask.getYSize() * WorldSettings.layoutRatio / 4, seed + 13);
		MountainNoise.add(-0.6f);
		MountainNoise.cap();
		MapUtils.smoothBiggen(MountainNoise, 4);
		MountainNoise.mult(2.5f);
		MountainNoise.applyMask(peaks, 0f);
		//ImageIO.saveAsGreyImage(MountainNoise, "MountainNoise 1");
		ret = new HeightMap(MapUtils.addDiminMap(ret, MountainNoise));
		MountainNoise = null;
		
		MountainNoise = NoiseGenerator.createMountainNoiseMap(terraGenMask.getXSize() * WorldSettings.layoutRatio / 2, terraGenMask.getYSize() * WorldSettings.layoutRatio / 2, seed + 13);
		MountainNoise.add(-0.2f);
		MountainNoise.cap();
		MapUtils.smoothBiggen(MountainNoise, 2);
		MountainNoise.mult(1.25f);
		MountainNoise.applyMask(peaks, 0f);
		//ImageIO.saveAsGreyImage(MountainNoise, "MountainNoise 1");
		ret = new HeightMap(MapUtils.addDiminMap(ret, MountainNoise));
		MountainNoise = null;
		
		System.out.println("  created Mountain Peaks");
		
		riverMaskLarge.invert();
		terraGenMaskLarge.invert();
		riverMaskLarge.or(terraGenMaskLarge);
		terraGenMaskLarge.invert();
		
		HeightMap rgml = riverMaskLarge.toHeightMap(0f, 1f);
		MapUtils.repeatedConvSmooth(rgml, 8, 2);
		riverMaskLarge = rgml.above(0.1f);
		rgml = riverMaskLarge.toHeightMap(1f, 0f);
		riverMaskLarge = null;
		HeightMap distMul = MapUtils.geoDistAboveHeight(rgml, 0f, 224);
		ImageIO.saveAsGreyImage(rgml, "rgml");
		rgml = null;
		System.out.println("Made mountain carving shape");
		distMul.pow(1.4f);
		HeightMap hn = NoiseGenerator.createMountainNoiseMap(terraGenMask.getXSize() * WorldSettings.layoutRatio / 2, terraGenMask.getYSize() * WorldSettings.layoutRatio / 2, seed + 1441);
		HeightMap hn2 = NoiseGenerator.createMountainNoiseMap(terraGenMask.getXSize() * WorldSettings.layoutRatio / 2, terraGenMask.getYSize() * WorldSettings.layoutRatio / 2, seed + 17641);
		HeightMap hn3 = NoiseGenerator.createMountainNoiseMap(terraGenMask.getXSize() * WorldSettings.layoutRatio, terraGenMask.getYSize() * WorldSettings.layoutRatio, seed + 17641);
		hn.add(-0.5f);
		hn.cap();
		MapUtils.smoothBiggen(hn, 2);
		MapUtils.smoothBiggen(hn2, 2);
		hn.mult(2f);
		hn = hn.max(hn2).max(hn3);
		hn.applyMask(terraGenMaskLarge);
		ImageIO.saveAsGreyImage(hn, "MountainNoise 2");
		System.out.println("Made mountain noise shape");
		distMul.mult(0.5f);
		hn = new HeightMap(MapUtils.addNormalizedMap(hn, distMul));
		distMul.mult(2f);
		distMul = new HeightMap(MapUtils.cappedMultiply(distMul, hn));
		hn = null;
		ret = new HeightMap(MapUtils.cappedMultiply(ret, distMul));
		distMul = null;
		rgml = terraGenMaskLarge.toHeightMap(0.004f, 0f);
		terraGenMaskLarge = null;
		ret = new HeightMap(MapUtils.addDiminMap(ret, rgml));
		MapUtils.smoothBiggen(valleyBase, WorldSettings.layoutRatio);
		ret = new HeightMap(MapUtils.addDiminMap(ret, valleyBase));
		valleyBase = null;
		
		boolean cosmetics = false;
		
		if(cosmetics) {
			System.out.println("Starting first Erosionpass");
			ErosionGenerator.boringErosion(ret, 1, 0.1f, 16, 128, 0.003f, 0.02f, 0.0004f, 0.3f, 4f);
			ErosionGenerator.boringErosion(ret, 2, 0.4f, 4, 64, 0.03f, 0.02f, 0.0002f, 0.2f);
			ErosionGenerator.boringErosion(ret, 2, 0.1f, 4, 32, 0.03f, 0.02f, 0.0008f, 0.4f);
			ThermalWeatheringGenerator.weather(ret, 0.2f, 3f, 32, 0.018f, seed);
			ThermalWeatheringGenerator.weather(ret, 0.5f, 3f, 64, 0.008f, seed);
			MapUtils.smoothAt(ret, 1, (short) 0, 6000);
			MapUtils.smoothAt(ret, 1, (short) 0, 12000);
		}
		
		HeightMap diffuse = new HeightMap(ret.clone());
		MapUtils.repeatedConvSmooth(diffuse, 8, 4);
		ret = ret.max(diffuse);
		
		
		
		
		
		if(cosmetics) {
			ErosionGenerator.boringErosion(ret, 2, 0.2f, 4, 32, 0.03f, 0.02f, 0.0004f, 0.4f, 4f);
			ErosionGenerator.boringErosion(ret, 2, 0.4f, 2, 32, 0.03f, 0.02f, 0.0004f, 0.4f);
			ThermalWeatheringGenerator.weather(ret, 0.5f, 10f, 64, 0.004f, seed);
			ThermalWeatheringGenerator.weather(ret, 0.5f, 1f, 64, 0.002f, seed);
			MapUtils.convSmooth(ret, 1, 0f, 12);
			
			ThermalWeatheringGenerator.weather(ret, 0.5f, 10f, 64, 0.004f, seed);
			ThermalWeatheringGenerator.weather(ret, 0.5f, 1f, 64, 0.002f, seed);
			ErosionGenerator.boringErosion(ret, 8, 0.4f, 4, 32, 0.04f, 0.02f, 0.00006f, 0.2f, 8f);
			MapUtils.convSmooth(ret, 1, 0f, 16);
			
			
		}
		
		HeightMap slopes = MapUtils.slopes(ret);
		Mask cliffs = slopes.above(0.004f);
		ImageIO.saveAsGreyImage(cliffs, "cliffs");
		HeightMap cliffMult = MapUtils.geoDistAboveHeight(cliffs.toHeightMap(1f, 0f), 0f, 16);
		
		System.out.println("Finished Mountain Terrain");
		return ret;
	}
	
	@Override
	public String identify() {
		return identifier;
	}
}
