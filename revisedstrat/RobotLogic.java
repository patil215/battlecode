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


}
