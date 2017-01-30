package revisedstrat;

import java.time.Clock;

import battlecode.common.*;

public class BulletCollector extends RobotLogic {

	Direction moveDir;
	CombatUnitLogic combatMode;
	
	public BulletCollector(RobotController rc) {
		super(rc);
		combatMode = new CombatUnitLogic(rc);
		moveDir = Utils.randomDirection();
	}

	@Override
	public void run() {
		while (true) {
			try {
				TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				MapLocation goodTreeLocation = this.getLocationOfClosestTreeWithBullets(nearbyTrees);
				if (goodTreeLocation != null) {
					Direction toMove = rc.getLocation().directionTo(goodTreeLocation);
					toMove = getDirectionTowards(toMove);
					move(toMove);
				} else if(rc.getRoundNum()<500){
					moveDir=this.moveWithRandomBounce(moveDir);
				} else{
					combatMode.run();
				}
				endTurn();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private MapLocation getLocationOfClosestTreeWithBullets(TreeInfo[] nearbyTrees) throws GameActionException {
		double value = 0;
		TreeInfo toReturn = null;
		for (TreeInfo t : nearbyTrees) {
			double currentValue = t.containedBullets / rc.getLocation().distanceTo(t.location);
			if (currentValue > value) {
				value = currentValue;
				toReturn = t;
			}
		}
		if (toReturn == null) {
			System.out.println("No target found");
			return null;
		} else {
			rc.setIndicatorDot(toReturn.location, 37, 14, 200);
		}
		return toReturn.location;
	}

}
