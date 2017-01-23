package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

/**
 * Created by patil215 on 1/17/17.
 */
public class CombatUnitLogic extends RobotLogic {

	private final int SOLDIER_UNIT_COUNT_ATTACK_THRESHOLD = 15;

	private MapLocation[] enemyArchonLocations;
	private int archonVisitedIndex;
	private final static int GARDENER_HELP_PRIORITY = 3;
	private final static int ARCHON_HELP_PRIORITY = 2;
	private final static int MOVE_TOWARDS_COMBAT_PRIORITY = 1;
	private static int currentDestinationType;

	private static MapLocation birthLocation;
	private static int birthRound;

	public CombatUnitLogic(RobotController rc) {
		super(rc);
		enemyArchonLocations = rc.getInitialArchonLocations(getEnemyTeam());
		archonVisitedIndex = 0;
		currentDestinationType = 0;
		birthLocation = rc.getLocation();
		birthRound = rc.getRoundNum();
	}

	@Override
	public void run() {
		while (true) {

			try {
				// Check if visited archon location and invalidate it
				checkVisitedArchonLocation();

				MapLocation destination = this.getDestination();
				if (destination != null && shouldClearLocation(destination)) {
					System.out
							.println("Trying to clear location currentDestination type is: " + currentDestinationType);
					switch (currentDestinationType) {
					case GARDENER_HELP_PRIORITY:
						BroadcastManager.invalidateLocation(rc, LocationInfoType.GARDENER_HELP);
						break;
					case ARCHON_HELP_PRIORITY:
						BroadcastManager.invalidateLocation(rc, LocationInfoType.ARCHON_HELP);
						break;
					case MOVE_TOWARDS_COMBAT_PRIORITY:
						// TODO: Replace with more detailed analysis of
						// locations.
						BroadcastManager.invalidateLocation(rc, LocationInfoType.ENEMY);
						break;
					}
					System.out.println("reached");
					currentDestinationType = 0;
					setDestination(null);
				}

				// Combat mode
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, getEnemyTeam());
				if (enemyRobots.length > 0) {
					currentDestinationType = 0;
					setDestination(null);
					executeCombat(enemyRobots);
					endTurn();
					continue;
				}

				// Defense mode
				// Try to help gardeners
				MapLocation gardenerHelpLocation = BroadcastManager.getRecentLocation(rc,
						BroadcastManager.LocationInfoType.GARDENER_HELP);
				if (gardenerHelpLocation != null && currentDestinationType <= GARDENER_HELP_PRIORITY) {
					// moveTowardsCombat(gardenerHelpLocation,
					// BroadcastManager.LocationInfoType.GARDENER_HELP);
					if (currentDestinationType < GARDENER_HELP_PRIORITY || gardenerHelpLocation.x != getDestination().x
							|| gardenerHelpLocation.y != getDestination().y) {

						setDestination(gardenerHelpLocation);
						currentDestinationType = GARDENER_HELP_PRIORITY;
						tryToMoveToDestination();
						endTurn();
						continue;
					}
				}

				// Try to help archons
				MapLocation archonHelpLocation = BroadcastManager.getRecentLocation(rc,
						BroadcastManager.LocationInfoType.ARCHON_HELP);
				if (archonHelpLocation != null && currentDestinationType < ARCHON_HELP_PRIORITY) {
					// moveTowardsCombat(archonHelpLocation,
					// BroadcastManager.LocationInfoType.ARCHON_HELP);

					if (currentDestinationType < ARCHON_HELP_PRIORITY || archonHelpLocation.x != getDestination().x
							|| archonHelpLocation.y != getDestination().y) {
						setDestination(archonHelpLocation);
						currentDestinationType = ARCHON_HELP_PRIORITY;
						tryToMoveToDestination();
						endTurn();
						continue;
					}
				}

				// Attack mode
				// If unit is a soldier, wait until we have more than 50 units
				// to attack. If tank, go for it.
				if (((rc.getType() == RobotType.SOLDIER && rc.getRobotCount() > SOLDIER_UNIT_COUNT_ATTACK_THRESHOLD)
						|| rc.getType() == RobotType.TANK) && currentDestinationType < MOVE_TOWARDS_COMBAT_PRIORITY) {
					MapLocation enemyLocation = getEnemyLocation();
					if (enemyLocation != null) {
						// boolean success = moveTowardsCombat(enemyLocation,
						// BroadcastManager.LocationInfoType.ENEMY);

						if (currentDestinationType < MOVE_TOWARDS_COMBAT_PRIORITY
								|| enemyLocation.x != getDestination().x || enemyLocation.y != getDestination().y) {
							setDestination(enemyLocation);
							currentDestinationType = MOVE_TOWARDS_COMBAT_PRIORITY;
							boolean success = tryToMoveToDestination();

							if (success) {
								endTurn();
								continue;
							}
						}
					}
				}

				// Discovery mode, move randomly
				if (currentDestinationType > 0) {
					tryToMoveToDestination();
					endTurn();
				} else {
					moveIntelligentlyRandomly();
					endTurn();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void updatePanicMode() {
		Direction toTry = Utils.randomDirection();
	}

	private boolean shouldClearLocation(MapLocation destination) throws GameActionException {
		Direction toMove = rc.getLocation().directionTo(destination);
		RobotInfo frontRobot = rc.senseRobotAtLocation(rc.getLocation().add(toMove));
		System.out.println("run");
		return this.getClosestDistance() <= DISTANCE_TO_CLEAR_DESTINATION
				|| rc.getLocation().distanceTo(destination) <= DISTANCE_TO_CLEAR_DESTINATION
				|| (rc.canSenseLocation(destination) && frontRobot != null && frontRobot.team == rc.getTeam());
	}

	private void checkVisitedArchonLocation() {
		// Avoid out of bounds index error
		if (archonVisitedIndex >= enemyArchonLocations.length) {
			return;
		}
		if (rc.getLocation().distanceTo(enemyArchonLocations[archonVisitedIndex]) < rc.getType().sensorRadius * 0.8
				&& rc.senseNearbyRobots(-1, getEnemyTeam()).length == 0) {
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

		BroadcastManager.saveLocation(rc, enemyRobots[0].location, LocationInfoType.ENEMY);

		// Move
		BulletInfo hittingBullet = getTargetingBullet(surroundingBullets);
		if (hittingBullet != null) {
			System.out.println("In combat");
			// moveAndDodge(hittingBullet.getLocation(), surroundingBullets);
			MapLocation safeLocation = getBulletAvoidingLocation(rc);
			if (safeLocation != null) {
				move(safeLocation);
			}
		}

		// Shoot
		RobotInfo target = getHighestPriorityTarget(enemyRobots, true); // Wyatt
																		// will
		// optimize
		// this
		if (target != null) {
			System.out.println("Found a target");
			// Broadcast the location of the target
			BroadcastManager.saveLocation(rc, target.location, BroadcastManager.LocationInfoType.ENEMY);

			tryAndFireAShot(target);

		} else {
			// TODO: Refactor
			// Try to get closer to the enemy
			System.out.println("Found no target");
			target = (RobotInfo) getClosestBody(enemyRobots);
			if (target != null) {
				MapLocation safeLocation = getBulletAvoidingLocation(rc);
				if (safeLocation != null) {
					move(safeLocation);
				}
				// moveAndDodge(target.getLocation(), surroundingBullets);
				Direction toMove = moveTowards(target.location);
				if (toMove != null) {
					if (rc.canMove(toMove)) {
						move(toMove);
					}
				} else {
					this.moveWithRandomBounce(Utils.randomDirection());
				}
			} else {
				this.moveWithRandomBounce(Utils.randomDirection());
			}
		}
	}

	private void tryAndFireAShot(RobotInfo target) throws GameActionException {
		if (rc.canFirePentadShot() && rc.getTeamBullets() > 70) {
			rc.firePentadShot(rc.getLocation().directionTo(target.location));
		} else if (rc.canFireTriadShot() && rc.getTeamBullets() > 30) {
			rc.fireTriadShot(rc.getLocation().directionTo(target.location));
		} else if (rc.canFireSingleShot()) {
			rc.fireSingleShot(rc.getLocation().directionTo(target.location));
		}
	}

	/*
	 * Returns true if unit was able to move towards the map location.
	 */
	private boolean moveTowardsCombat(MapLocation combatLocation, BroadcastManager.LocationInfoType type)
			throws GameActionException {

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
