package revisedstrat;

import battlecode.common.*;

/**
 * Created by patil215 on 1/17/17.
 */
public class CombatUnitLogic extends RobotLogic {

	private final int SOLDIER_UNIT_COUNT_ATTACK_THRESHOLD = 30;

	private MapLocation[] enemyArchonLocations;
	private int archonVisitedIndex;

	public CombatUnitLogic(RobotController rc) {
		super(rc);
		enemyArchonLocations = rc.getInitialArchonLocations(getEnemyTeam());
		archonVisitedIndex = 0;
	}

	@Override
	public void run() {
		while (true) {

			try {
				// Check if visited archon location and invalidate it
				checkVisitedArchonLocation();

				// Combat mode
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, getEnemyTeam());
				if (enemyRobots.length > 0) {
					executeCombat(enemyRobots);
					continue;
				}

				// Defense mode
				// Try to help gardeners
				MapLocation gardenerHelpLocation = BroadcastManager.getRecentLocation(rc,
						BroadcastManager.LocationInfoType.GARDENER_HELP);
				if (gardenerHelpLocation != null) {
					moveTowardsCombat(gardenerHelpLocation, BroadcastManager.LocationInfoType.GARDENER_HELP);
					continue;
				}
				// Try to help archons
				MapLocation archonHelpLocation = BroadcastManager.getRecentLocation(rc,
						BroadcastManager.LocationInfoType.ARCHON_HELP);
				if (archonHelpLocation != null) {
					moveTowardsCombat(archonHelpLocation, BroadcastManager.LocationInfoType.ARCHON_HELP);
					continue;
				}

				// Attack mode
				// If unit is a soldier, wait until we have more than 50 units
				// to attack. If tank, go for it.
				if ((rc.getType() == RobotType.SOLDIER && rc.getRobotCount() > SOLDIER_UNIT_COUNT_ATTACK_THRESHOLD)
						|| rc.getType() == RobotType.TANK) {
					MapLocation enemyLocation = getEnemyLocation();
					if (enemyLocation != null) {
						boolean success = moveTowardsCombat(enemyLocation, BroadcastManager.LocationInfoType.ENEMY);
						if (success) {
							continue;
						}
					}
				}

				// Discovery mode, move randomly
				moveIntelligentlyRandomly();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void checkVisitedArchonLocation() {
		// Avoid out of bounds index error
		if (archonVisitedIndex >= enemyArchonLocations.length) {
			return;
		}
		if (rc.getLocation().distanceTo(enemyArchonLocations[archonVisitedIndex]) < rc.getType().sensorRadius * 0.8
				&& rc.senseNearbyRobots(-1, getEnemyTeam()).length > 0) {
			// This archon has been visited and there's no one here, move onto
			// the next one
			archonVisitedIndex++;
		}
	}

	/*
	 * Returns null if there are no good found enemy locations to travel
	 * towards.
	 */
	private MapLocation getEnemyLocation() throws GameActionException {
		// Try to first read from shared broadcast manager
		MapLocation notedEnemyLocation = BroadcastManager.getRecentLocation(rc,
				BroadcastManager.LocationInfoType.ENEMY);
		if (notedEnemyLocation != null) {
			return notedEnemyLocation;
		}

		// Try to go to initial archon locations
		if (archonVisitedIndex < enemyArchonLocations.length) {
			return enemyArchonLocations[archonVisitedIndex];
		}

		return null;
	}

	private void moveIntelligentlyRandomly() throws GameActionException {
        moveWithRandomBounce(Utils.randomDirection());
	}

	private void executeCombat(RobotInfo[] enemyRobots) throws GameActionException {
		BulletInfo[] surroundingBullets = rc.senseNearbyBullets();

		// Move
		// moveAndDodge(surroundingBullets); // Dhruv will make this
		BulletInfo hittingBullet = getTargetingBullet(surroundingBullets);
		if (hittingBullet != null) {
			// Dodge the bullet!
			dodge(hittingBullet);
		}

		// Shoot
		RobotInfo target = getHighestPriorityTarget(enemyRobots, true); // Wyatt will
																	// optimize
																	// this
		if (target != null) {
            
            // Broadcast the location of the target
            BroadcastManager.saveLocation(rc, target.location, BroadcastManager.LocationInfoType.ENEMY);

			if (rc.canFirePentadShot()) {
				rc.firePentadShot(rc.getLocation().directionTo(target.location));
			} else if (rc.canFireTriadShot()) {
				rc.fireTriadShot(rc.getLocation().directionTo(target.location));
			} else if (rc.canFireSingleShot()) {
				rc.fireSingleShot(rc.getLocation().directionTo(target.location));
			}

		} else {
			// Try to get closer to the enemy
			// moveAndDodge(surroundingBullets);
			target = (RobotInfo) this.getClosestBody(enemyRobots);
			Direction toMove = moveTowards(target.location);
			if (toMove != null) {
				if (rc.canMove(toMove)) {
					move(toMove);
				}
			} else {
				this.moveWithRandomBounce(Utils.randomDirection());
			}
		}
	}

	/*
	 * Returns true if unit was able to move towards the map location.
	 */
	private boolean moveTowardsCombat(MapLocation combatLocation, BroadcastManager.LocationInfoType type) throws GameActionException {

        // Invalidate broadcast location if no enemies
        if (combatLocation != null && closeToLocationAndNoEnemies(rc, combatLocation)) {
            BroadcastManager.invalidateLocation(rc, type);
            return false;
        }

		// Move with intelligent pathfinding
		Direction directionToMove = moveTowards(combatLocation);
		if (directionToMove != null) {
			move(directionToMove);
			return true;
		}
		return false;
	}

}
