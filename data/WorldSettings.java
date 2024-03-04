package data;

public class WorldSettings {
	int previewSize;
	long size;
	
	
	float ClimateBoundary1;
	float ClimateBoundary2;
	float medianTemperature;
	boolean clockwiseRotation;
	
	int maxPlateSize;
	int minPlateSize;
	int bigPlateCount;
	int smallPlateCount;
	int plateOctaves;
	long age;
	
	
	double oceanPlatePercentage;
	
	public static final float XYRATIO = 256;
	public static final float layoutSeaLevel = 0.5f;
	public static final float layoutForestMaxLevel = 0.8f;
	public static final long seed = 9575997L;
	public static final int layoutRatio = 16;
	public static final float waterHeightUp = 0.0f;
	public static final float waterHeightDown = 0.5f;
	
	private WorldSettings() {
		
	}
}
