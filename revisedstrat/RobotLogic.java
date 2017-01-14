package revisedstrat;

import battlecode.common.*;

import static revisedstrat.Utils.randomDirection;

/**
 * Created by patil215 on 1/12/17.
 */
public abstract class RobotLogic {

	public RobotController rc;

	public RobotLogic(RobotController rc) {
		this.rc = rc;
	}

	public abstract void run();

	public Team getEnemyTeam() {
		return rc.getTeam().opponent();
	}

	public MapLocation getRandomEnemyInitialArchonLocation() {
		MapLocation[] enemyLocs = rc.getInitialArchonLocations(getEnemyTeam());
		return enemyLocs[(int) (enemyLocs.length * (Math.random()))];
	}

	// Returns the direction that was moved
	public Direction moveWithRandomBounce(Direction move) throws GameActionException {
		if (rc.canMove(move)) {
			rc.move(move);
		} else {
			for (int count = 0; count < 20 && !rc.canMove(move); count++) {
				move = randomDirection();
			}
			if (rc.canMove(move)) {
				rc.move(move);
			}
		}
		return move;
	}
	
	// If it is possible to move towards the target, then this method returns
	// the best angle to do so with. Otherwise, null is returned.
	public Direction moveTowards(MapLocation destination) {
		Direction toMove = rc.getLocation().directionTo(destination);
		if (rc.canMove(toMove)) {
			return toMove;
		} else {
			for (int deltaAngle = 0; deltaAngle < 180; deltaAngle += 10) {
				if (rc.canMove(toMove.rotateLeftDegrees(deltaAngle))) {
					return toMove.rotateLeftDegrees(deltaAngle);
				} else if (rc.canMove(toMove.rotateRightDegrees(deltaAngle))) {
					return toMove.rotateRightDegrees(deltaAngle);
				}
			}
		}
		return null;
	}
	
	protected void tryAndShakeATree() throws GameActionException {
		if (rc.canShake()) {
			TreeInfo[] trees = rc.senseNearbyTrees();
			for (TreeInfo t : trees) {
				if (rc.canShake(t.ID)) {
					rc.shake(t.ID);
					break;
				}
			}
		}
	}

	/*
	 * This method takes the location and direction of a bullet.
	 * 
	 * It returns the team of the first object that it will hit. If no object will be hit,
	 * this method returns NEUTRAL.
	 */
	public Team getFirstHitTeam(MapLocation location, Direction direction){
		
		//Detect tree collisions.
		TreeInfo[] trees = rc.senseNearbyTrees();
		
		float minTreeDistance = -1;
		TreeInfo hitTree = null;
		
		for(TreeInfo tree : trees){
			float distance = getIntersectionDistance(location, direction, tree);
			
			if(distance < minTreeDistance && distance >= 0){
				hitTree = tree;
				minTreeDistance = distance;
			}
		}
		
		//Detect robot collisions 
		RobotInfo[] robots = rc.senseNearbyRobots();
		
		float minRobotDistance = -1;
		RobotInfo hitRobot = null;
		
		for(RobotInfo robot : robots){
			float distance = getIntersectionDistance(location, direction, robot);
			
			if(distance < minRobotDistance && distance >= 0){
				hitRobot = robot;
				minRobotDistance = distance;
			}
		}
		
		//If nothing is hit, return neutral
		if(hitTree == null && hitRobot == null){
			return Team.NEUTRAL;
		}
		
		//If only robots are intersected, return the nearest robot's team
		else if(hitTree == null && hitRobot != null){
			return hitRobot.getTeam();
		}
		
		//If only trees are intersected, return the nearest tree's team
		else if(hitTree != null && hitRobot == null){
			return hitTree.getTeam();
		}
		
		//If both are intersected, return the team of whichever is closer
		else{
			if(minTreeDistance < minRobotDistance){
				return hitTree.getTeam();
			}
			else{
				return hitRobot.getTeam();
			}
		}
	}
	
	
	/*
	 * This method takes the location and direction of a bullet, as well as
	 * the BodyInfo of the target to be intersected.
	 * 
	 * The method returns the distance at which the target will be intersected. If the target
	 * is never intersected, this method returns -1.
	 */
	private float getIntersectionDistance(MapLocation location, Direction direction, BodyInfo target){
		
		float targetRadius = target.getRadius();
		
		//The x and y coordinates of the center of the target.
		float xTarget = target.getLocation().x;
		float yTarget = target.getLocation().y;
		
		//The x and y coordinates of the bullet's starting point.
		float xStart = location.x;
		float yStart = location.y;
		
		//Compute the shortest distance between the bullet and the center of the target
		float angle = direction.radians;
		float dist = (float)Math.abs(Math.sin(angle) * (xTarget - xStart) - Math.cos(angle) * (yTarget - yStart));
		
		//If the shortest distance is too large, the bullet won't ever intersect the target
		if(dist > targetRadius){
			return -1;
		}
		
		//Compute the distance the bullet travels to get to the point of closest approach
		float lengthToClosestApproach = (float)Math.sqrt((xTarget - xStart)*(xTarget - xStart) + (yTarget - yStart)*(yTarget - yStart) - dist*dist);
		
		//Compute the distance the bullet travels from the intersection point to the closest approach
		float excessDistance = (float)Math.sqrt(targetRadius*targetRadius - dist*dist);
		
		return lengthToClosestApproach - excessDistance;
	}
}
