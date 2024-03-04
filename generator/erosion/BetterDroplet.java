package generator.erosion;

import java.util.HashSet;

import data.map.HeightMap;
import data.util.MapUtils;
import io.ImageIO;

public class BetterDroplet {
	private float momentum;
	private int xPos, yPos;
	private float waterAmount;
	private float sedimentAmount = 0;
	private byte direction;
	private float evaporation;
	private float lastSlope = 0;
	private int radius = 4;
	private float fillSpeed = 0.08F;
	private float dropSpeed = 0.1F;
	private float[][] mask;
	private float maskVolume;
	private float maskExponent;
	
	private HeightMap world;
	private float[][] change;
	private int index;
	private int tickIndex;
	private int[][] trail;
	private int lifespan;
	
	public BetterDroplet(HeightMap w, int radius) {
		world = w;
		this.radius = radius;
	}
	
	public void setMaskExponent(float maskExpponent) {
		this.maskExponent = maskExpponent;
	}
	
	public float getMaskExponent() {
		return maskExponent;
	}
	
	public void printMask() {
		HeightMap w = new HeightMap(mask);
		ImageIO.saveAsGreyImage(w, "Mask");
	}
	
	private void updateMask() {
		mask = new float[2 * radius + 1][2*radius + 1];
		float f = 0;
		for(int i = -radius; i <= radius; i++) {
			for(int j = -radius; j <= radius; j++) {
				float t = i * i + j * j;
				if(radius > 0) t /= radius * radius;
				t = 1 - t;
				t = Math.max(t, 0);
				f += t;
				mask[i + radius][j + radius] = (float) Math.pow(t, maskExponent);
			}
		}
		for(int i = -radius; i <= radius; i++) {
			for(int j = -radius; j <= radius; j++) {
				mask[i + radius][j + radius] /= f;
			}
		}
		maskVolume = f;
	}
	
	public void reset(int lifespan, float[][] change, int radius, float maxChange) {
		momentum = 0F;
		sedimentAmount = 0F;
		lastSlope = 0F;
		this.lifespan = lifespan;
		this.waterAmount = (2 * radius + 1) * (2 * radius + 1) * maxChange;
		evaporation = (waterAmount / lifespan) * 2;
		this.change = change;
		this.radius = radius;
		updateMask();
		index = 0;
		tickIndex = 0;
		trail = new int[lifespan][2];
	}
	
	public void relocate(int x, int y) {
		xPos = x;
		yPos = y;
	}
	
	
	public boolean doLivingTick() {
		//long time = System.currentTimeMillis();
		//System.out.print("Droplet living tick: waterAmount " + waterAmount + " | radius: " + radius + "    ||   ");
		if(!world.isInBounds(xPos, yPos)) return false;
		for(int i = 0; i < tickIndex; i++) {
			if(trail[i][0] == xPos && trail[i][1] == yPos) return false;
		}
		trail[tickIndex][0] = xPos;
		trail[tickIndex][1] = yPos;
		float height = world.getValueAt(xPos, yPos);
		float[] nbs = world.get8Nb(xPos, yPos);
		byte newDir = world.getLowestNBbySlope(xPos, yPos);
		float slope = height - nbs[newDir];
		if(slope > -0.00001F && slope < 0.00001F) newDir = direction;
		float[][] area = MapUtils.submap(world, xPos - radius, yPos - radius, 2 * radius + 1, 2 * radius + 1).getContent();
		//momentum *= 0.8F;
		momentum += lastSlope * 16;
		momentum = Math.max(momentum, 0);
		momentum = Math.min(momentum, 1);
		waterAmount -= (evaporation * (1 - momentum));
		waterAmount = Math.max(waterAmount, 0);
		
		float capacity = waterAmount * momentum;
		float delta = capacity - sedimentAmount;
		//delta = -Math.min(sedimentAmount, -delta);
		delta *= delta > 0 ? fillSpeed : dropSpeed;
		
		
		//System.out.print(System.currentTimeMillis() - time + " ms     | ");
		//time = System.currentTimeMillis();
		
		if(delta >= 0) {
			delta = Math.min(slope * maskVolume, delta);
			delta = Math.max(delta, 0);
			float nd = 0f;
			for(int i = -radius; i <= radius; i++) {
				for(int j = -radius; j <= radius; j++) {
					float fc = area[i + radius][j + radius] >= height ? -delta * mask[i + radius][j + radius] : 0f;
					float[] f = {xPos + i, yPos + j, fc};
					nd -= fc;
					change[index] = f;
					index++;
				}
			}
			delta = nd;
		} else {
			float mh = 1F;
			for(int i = -radius; i <= radius; i++) {
				mh = Math.min(area[0][radius + i], mh);
				mh = Math.min(area[2 * radius][radius + i], mh);
				mh = Math.min(area[radius + i][0], mh);
				mh = Math.min(area[radius + i][2 * radius], mh);
			}
			float[][] dropMask = new float[2*radius+1][2*radius+1];
			float sum = 0;
			for(int i = -radius; i <= radius; i++) {
				for(int j = -radius; j <= radius; j++) {
					dropMask[i + radius][j + radius] = world.isInBounds(xPos + i, yPos + j) ? Math.max(0, mh - area[i + radius][j + radius]) : 0;
					sum += dropMask[i + radius][j + radius];
				}
			}
			if(-delta > sum) {
				float overflow = -delta - sum;
				for(int i = -radius; i <= radius; i++) {
					for(int j = -radius; j <= radius; j++) {
						float[] f = {xPos + i, yPos + j, overflow * mask[i + radius][j + radius] + dropMask[i + radius][j + radius]};
						change[index] = f;
						index++;
					}
				}
			} else {
				for(int i = -radius; i <= radius; i++) {
					for(int j = -radius; j <= radius; j++) {
						dropMask[i + radius][j + radius] *= mask[i + radius][j + radius] * maskVolume;
					}
				}
				for(int i = -radius; i <= radius; i++) {
					for(int j = -radius; j <= radius; j++) {
						float[] f = {xPos + i, yPos + j, delta / sum * dropMask[i + radius][j + radius]};
						change[index] = f;
						index++;
					}
				}
			}
			
		}
		
		
		
		//System.out.print(System.currentTimeMillis() - time + " ms    | ");
		//time = System.currentTimeMillis();
		
		sedimentAmount += delta;
		if(sedimentAmount < 0) System.out.println("Alarm" + sedimentAmount + " / " + delta + " / " + capacity + " / " + momentum + " / " + waterAmount);
		direction = newDir;
		byte[] ddir = MapUtils.decomposeDirection(direction);
		xPos += ddir[0];
		yPos += ddir[1];
		lastSlope = slope;
		tickIndex++;
		//System.out.println(System.currentTimeMillis() - time + " ms");
		return world.isInBounds(xPos, yPos) && waterAmount > -evaporation;
	}
	
	public void drop() {
		sedimentAmount *= dropSpeed;
		if(sedimentAmount <= 0.001f) return;
		float[][] area = MapUtils.submap(world, xPos - radius, yPos - radius, 2 * radius + 1, 2 * radius + 1).getContent();
		float mh = 1F;
		for(int i = -radius; i <= radius; i++) {
			mh = Math.min(area[0][radius + i], mh);
			mh = Math.min(area[2 * radius][radius + i], mh);
			mh = Math.min(area[radius + i][0], mh);
			mh = Math.min(area[radius + i][2 * radius], mh);
		}
		float[][] dropMask = new float[2*radius+1][2*radius+1];
		float sum = 0;
		for(int i = -radius; i <= radius; i++) {
			for(int j = -radius; j <= radius; j++) {
				dropMask[i + radius][j + radius] = world.isInBounds(xPos + i, yPos + j) ? Math.max(0, mh - area[i + radius][j + radius]) : 0;
				sum += dropMask[i + radius][j + radius];
			}
		}
		if(sedimentAmount > sum) {
			float overflow = sedimentAmount - sum;
			for(int i = -radius; i <= radius; i++) {
				for(int j = -radius; j <= radius; j++) {
					float[] f = {xPos + i, yPos + j, overflow * mask[i + radius][j + radius] + dropMask[i + radius][j + radius]};
					change[index] = f;
					index++;
				}
			}
		} else {
			for(int i = -radius; i <= radius; i++) {
				for(int j = -radius; j <= radius; j++) {
					dropMask[i + radius][j + radius] *= mask[i + radius][j + radius] * maskVolume;
				}
			}
			for(int i = -radius; i <= radius; i++) {
				for(int j = -radius; j <= radius; j++) {
					float[] f = {xPos + i, yPos + j, sedimentAmount / sum * dropMask[i + radius][j + radius]};
					change[index] = f;
					index++;
				}
			}
		}
	}
	
	private float calcChange(float delta, float height) {
		if(delta > 0) {
			for(int i = -radius; i <= radius; i++) {
				for(int j = -radius; j <= radius; j++) {
					float[] f = {xPos + i, yPos + j, -delta * mask[i + radius][j + radius]};
					change[index] = f;
					index++;
				}
			}
			return delta;
		} else {
			for(int i = -radius; i <= radius; i++) {
				for(int j = -radius; j <= radius; j++) {
					float[] f = {xPos + i, yPos + j, -delta * mask[i + radius][j + radius]};
					change[index] = f;
					index++;
				}
			}
			return delta;
		}
	}
}
