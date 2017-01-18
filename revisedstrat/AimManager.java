package revisedstrat;

import java.util.ArrayList;

import battlecode.common.BodyInfo;
import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TreeInfo;

public class AimManager {

	private RobotController rc;
	
	private float[] distances;
	private BodyInfo[] targets;
	
	IntervalStorer intervals;
	
	public AimManager(RobotController rc){
		this.rc = rc;
		
		TreeInfo[] trees = rc.senseNearbyTrees();
		RobotInfo[] robots = rc.senseNearbyRobots();
		
		targets = new BodyInfo[trees.length + robots.length];
		distances = new float[targets.length];
		
		for(int index = 0; index < trees.length; index++){
			targets[index] = trees[index];
			distances[index] = rc.getLocation().distanceTo(targets[index].getLocation());
		}
		
		for(int index = 0; index < robots.length; index++){
			targets[index + trees.length] = robots[index];
			distances[index + trees.length] = rc.getLocation().distanceTo(targets[index + trees.length].getLocation());
		}
		
		intervals = new IntervalStorer();
		
		for(int index = 0; index < targets.length; index++){
			float[] interval = getInterval(index);
			intervals.addInterval(new Interval(interval[0], interval[1], index, distances[index]));
		}
	}
	
	/*
	 * returns the target that will be hit by a shot fired at the
	 * given angle.
	 * 
	 * Precondition: the angle is between 0 and 2pi radians.
	 */
	private BodyInfo getTarget(float angle){
		return targets[intervals.getIndex(angle)];
	}
	
	/*
	 * returns the angle interval produced by the target at the given index
	 */
	private float[] getInterval(int index){
		float center = rc.getLocation().directionTo(targets[index].getLocation()).radians;
		
		float lowerAngle = center - (float)Math.atan(targets[index].getRadius()/distances[index]);
		lowerAngle += 2 * Math.PI;
		lowerAngle %= 2 * Math.PI;
		
		float upperAngle = center + (float)Math.atan(targets[index].getRadius()/distances[index]);
		upperAngle %= 2 * Math.PI;
		
		return new float[]{lowerAngle, upperAngle};
	}
	
	/*
	 * This method returns the target that would be hit first
	 * if a single shot is fired in the passed direction.
	 * 
	 * null is returned if no target is found.
	 */
	public BodyInfo getTargetSingleShot(Direction direction){
		return getTarget(direction.radians);
	}
	
	/*
	 * This method returns the targets that would be hit first
	 * if a triple shot is fired in the passed direction.
	 * 
	 * null is returned if no target is found.
	 */
	public BodyInfo[] getTargetTripleShot(Direction direction){
		float tripleOffset = (float)Math.PI / 9; //20 degrees
		float angle = direction.radians;
		return new BodyInfo[]{getTarget(angle - tripleOffset), getTarget(angle), getTarget(angle + tripleOffset)};
	}
	
	/*
	 * This method returns the targets that would be hit first
	 * if a pentad shot is fired in the passed direction.
	 * 
	 * null is returned if no target is found.
	 */
	public BodyInfo[] getTargetPentadShot(Direction direction){
		float pentadOffset = (float)Math.PI / 12; //15 degrees
		float angle = direction.radians;
		return new BodyInfo[]{getTarget(angle - 2 * pentadOffset), getTarget(angle - pentadOffset), 
								getTarget(angle), getTarget(angle + pentadOffset), getTarget(angle + 2 * pentadOffset)};
	}
}

/*
 * Represents a half-open interval
 */
class Interval{
	float start;
	float end;
	int bodyIndex;
	float distance;
	
	public Interval(float start, float end, int bodyIndex, float distance){
		this.start = start;
		this.end = end;
		this.bodyIndex = bodyIndex;
		this.distance = distance;
	}
	
	public boolean contains(float value){
		return start <= value && value < end;
	}
	
	public String toString(){
		return "["+start+", "+end+"): "+bodyIndex+" "+distance;
	}
}

class IntervalStorer{
	ArrayList<Interval> intervals;
	
	public IntervalStorer(){
		intervals = new ArrayList<Interval>();
		intervals.add(new Interval(0, 2 * (float)Math.PI , -1, Float.POSITIVE_INFINITY));
	}
	
	public void addInterval(Interval toAdd){
		boolean addingInterval = false;
		int startIndex = -1;
		
		for(int index = 0; index < intervals.size(); index++, index %= intervals.size()){
			Interval currentInterval = intervals.get(index);
			if(!addingInterval && currentInterval.contains(toAdd.start)){
				if(toAdd.distance < currentInterval.distance){
					intervals.add(index + 1, new Interval(toAdd.start, currentInterval.end, toAdd.bodyIndex, toAdd.distance));
					currentInterval.end = toAdd.start;
					
					startIndex = index + 1;
				}
				addingInterval = true;
				continue;
			}
			
			if(addingInterval && currentInterval.contains(toAdd.end)){
				if(index == startIndex){
					Interval previousInterval = intervals.get(index - 1);
					intervals.add(index + 1, new Interval(toAdd.end, currentInterval.end, previousInterval.bodyIndex, previousInterval.distance));
				
					currentInterval.end = toAdd.end;
					currentInterval.bodyIndex = toAdd.bodyIndex;
					currentInterval.distance = toAdd.distance;
				}
				else{
					if(toAdd.distance < currentInterval.distance){
						intervals.add(index + 1, new Interval(toAdd.end, currentInterval.end, currentInterval.bodyIndex, currentInterval.distance));
						
						currentInterval.end = toAdd.end;
						currentInterval.bodyIndex = toAdd.bodyIndex;
						currentInterval.distance = toAdd.distance;
					}
				}
				
				return;
			}
			
			if(addingInterval){
				if(toAdd.distance < currentInterval.distance){
					currentInterval.bodyIndex = toAdd.bodyIndex;
					currentInterval.distance = toAdd.distance;
				}
			}
		}
	}
	
	/*
	 * Gets the index of the body at this angle
	 * 
	 * TODO: Make a binary search
	 */
	public int getIndex(float angle){
		for(int index = 0; index < intervals.size(); index++){
			Interval interval = intervals.get(index);
			if(interval.contains(angle)){
				return interval.bodyIndex;
			}
		}
		
		return -1;
	}
}