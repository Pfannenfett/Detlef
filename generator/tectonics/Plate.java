package generator.tectonics;

import java.awt.Color;
import java.util.Random;

public class Plate {
	
	private boolean isOceanPlate;
	private double subductId;
	private Color color;
	private float upMovement;
	private float rightMovement;
	private float elapsedYM;
	private float elapsedXM;
	private Random rng;
	
	public Plate(long seed, float oceanPlateChance) {
		rng = new Random(seed);
		isOceanPlate = rng.nextFloat() < oceanPlateChance;
		upMovement = (float) (rng.nextFloat() - 0.5) * 2;
		rightMovement = (float) (rng.nextFloat() - 0.5) * 2;
		subductId = rng.nextFloat();
		if(isOceanPlate) {
			color = new Color(0, 0, 255);
		} else {
			color = new Color(0, 255, 0);
		}
	}
	
	public void setOceanPlate(boolean isOceanPlate) {
		this.isOceanPlate = isOceanPlate;
		if(isOceanPlate) {
			color = new Color(0, 0, 255);
		} else {
			color = new Color(0, 255, 0);
		}
	}
	
	public boolean isOceanPlate() {
		return isOceanPlate;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public float getUpMovement() {
		return upMovement;
	}
	
	public float getRightMovement() {
		return rightMovement;
	}
	
	public int getNextXStep() {
		elapsedXM += rightMovement;
		if(elapsedXM >= 1) {
			elapsedXM -= 1;
			return 1;
		} else if(elapsedXM <= -1) {
			elapsedXM += 1;
			return -1;
		}
		return 0;
	}
	
	public int getNextYStep() {
		elapsedYM += upMovement;
		if(elapsedYM >= 1) {
			elapsedYM -= 1;
			return 1;
		} else if(elapsedYM <= -1) {
			elapsedYM += 1;
			return -1;
		}
		return 0;
	}
	
	public double getSubductId() {
		return isOceanPlate ? subductId : subductId + 1;
	}
}