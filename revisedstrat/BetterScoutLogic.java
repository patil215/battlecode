package revisedstrat;

import battlecode.common.*;

/**
 * Created by patil215 on 1/18/17.
 */
public class BetterScoutLogic extends RobotLogic {

	Direction moveDir;

	public BetterScoutLogic(RobotController rc) {
		super(rc);
		moveDir = Utils.randomDirection();
	}

	@Override
	public void run() {

		while (true) {

			try {

				RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
				RobotInfo closestEconUnit = findClosestEconUnit(enemyRobots);

				if (closestEconUnit != null) {
					// Attack econ
					attackEconUnit(closestEconUnit);
					endTurn();
					continue;
				}

				if (enemyRobots.length > 0) {
					// Attack enemy?
					attackEnemies(enemyRobots);
					endTurn();
					continue;
				}

				moveRecon();
				endTurn();
				continue;

			} catch (Exception e) {
				e.printStackTrace();
				;
			}

		}
	}

	private void moveRecon() throws GameActionException {
		// Move towards enemies
		MapLocation enemyLocation = BroadcastManager.getRecentLocation(rc,
				BroadcastManager.LocationInfoType.ENEMY);
		if (enemyLocation != null) {
			moveTowardsCombat(enemyLocation, BroadcastManager.LocationInfoType.ENEMY);
			return;
		}

		// Move randomly
		moveDir = moveWithRandomBounce(moveDir);
		endTurn();
	}

	private boolean moveTowardsCombat(MapLocation combatLocation, BroadcastManager.LocationInfoType type)
			throws GameActionException {

		// Invalidate broadcast location if no enemies
		if (combatLocation != null && closeToLocationAndNoEnemies(rc, combatLocation)) {
			BroadcastManager.invalidateLocation(rc, type);
			return false;
		}

		// Move with intelligent pathfinding
		Direction directionToMove = getDirectionTowards(combatLocation);
		if (directionToMove != null) {
			move(directionToMove);
			return true;
		}
		return false;
	}

	private RobotInfo findClosestEconUnit(RobotInfo[] enemyRobots) {
        RobotInfo closestEconUnit = null;
        float closestDistance = Float.MAX_VALUE;

        for(RobotInfo enemyRobot : enemyRobots) {
            float dist = rc.getLocation().distanceTo(enemyRobot.getLocation());
            if(enemyRobot.getType() == RobotType.GARDENER && dist < closestDistance) {
                closestDistance = dist;
                closestEconUnit = enemyRobot;
            }
        }
        return closestEconUnit;
    }

	private void attackEconUnit(RobotInfo econUnit) throws GameActionException {
		BroadcastManager.saveLocation(rc, econUnit.getLocation(), BroadcastManager.LocationInfoType.ENEMY);

		BulletInfo[] surroundingBullets = rc.senseNearbyBullets();

        move(getDirectionTowards(econUnit.getLocation()));

        if(rc.canFireSingleShot()) {
            rc.fireSingleShot(rc.getLocation().directionTo(econUnit.location));
        }
		/*if (rc.getLocation().distanceTo(econUnit.location) <= type.bodyRadius + econUnit.getType().bodyRadius
				+ GameConstants.BULLET_SPAWN_OFFSET && rc.canFireSingleShot()) {
			rc.fireSingleShot(rc.getLocation().directionTo(econUnit.location));
			if (willGetHitByABullet(rc.getLocation(), surroundingBullets)) {
				// This might not work well because canMove() is only checked at
				// the end of the checks
				// TODO replace with move perpendicularly
				moveAndDodge(econUnit.getLocation(), surroundingBullets);
			}
		} else {
			moveAndDodge(econUnit.getLocation(), surroundingBullets);
		}*/
	}

	private void attackEnemies(RobotInfo[] enemyRobots) throws GameActionException {
		RobotInfo target = getHighestPriorityTarget(enemyRobots, false);

		BulletInfo[] surroundingBullets = rc.senseNearbyBullets();

		if (target != null) {
			if (target.getType() != RobotType.ARCHON) {
				BroadcastManager.saveLocation(rc, target.getLocation(), BroadcastManager.LocationInfoType.ENEMY);
			}

			// Fire a bullet
			if (rc.canFireSingleShot()) {
				rc.fireSingleShot(rc.getLocation().directionTo(target.getLocation()));
			}

			// If we're in sensing radius, "run away"
			float targetDistance = rc.getLocation().distanceTo(target.getLocation());
			if (targetDistance < target.getType().sensorRadius) {
				MapLocation awayLocation = rc.getLocation()
						.add(rc.getLocation().directionTo(target.getLocation()).opposite());
				moveAndDodge(awayLocation, surroundingBullets);
			} else {
				dodge(surroundingBullets);
			}
		} else {
			if (enemyRobots[0] != null && enemyRobots[0].getType() != RobotType.ARCHON) {
				BroadcastManager.saveLocation(rc, enemyRobots[0].getLocation(),
						BroadcastManager.LocationInfoType.ENEMY);
			}
			moveRecon();
		}
	}

}
