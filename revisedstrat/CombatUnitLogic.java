package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

/**
 * Created by patil215 on 1/17/17.
 */
public class CombatUnitLogic extends RobotLogic {

	private final int SOLDIER_UNIT_COUNT_ATTACK_THRESHOLD = 15;

	private int archonVisitedIndex;
	private final static int GARDENER_HELP_PRIORITY = 3;
	private final static int ARCHON_HELP_PRIORITY = 2;
	private final static int MOVE_TOWARDS_COMBAT_PRIORITY = 1;
	private final static int COMBAT_MEMORY = 2;
	private static int currentDestinationType;

	private static MapLocation birthLocation;
	private static int birthRound;

	private static RobotInfo enemySeenLastRound;
	private int enemyCounter = 0;

	public CombatUnitLogic(RobotController rc) {
		super(rc);
		archonVisitedIndex = 0;
		currentDestinationType = 0;
		birthLocation = rc.getLocation();
		birthRound = rc.getRoundNum();
	}

	@Override
	public void run() {
		while (true) {

			try {

				enemyCounter--;
				if (enemyCounter <= 0) {
					enemyCounter = 0;
					enemySeenLastRound = null;
				}

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
					// System.out.println("reached");
					currentDestinationType = 0;
					setDestination(null);
				}

				// Combat mode
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
				if (enemyRobots.length > 0) {
					currentDestinationType = 0;
					setDestination(null);
					executeCombat();
					endTurn();
					continue;
				}

				// Defense mode
				// Try to help gardeners
				MapLocation gardenerHelpLocation = BroadcastManager.getRecentLocation(rc,
						BroadcastManager.LocationInfoType.GARDENER_HELP);
				if (gardenerHelpLocation != null && currentDestinationType < GARDENER_HELP_PRIORITY) {
					// moveTowardsCombat(gardenerHelpLocation,
					// BroadcastManager.LocationInfoType.GARDENER_HELP);
					if (currentDestinationType < GARDENER_HELP_PRIORITY || gardenerHelpLocation.x != getDestination().x
							|| gardenerHelpLocation.y != getDestination().y) {

						if (getDestination() == null || (rc.getLocation().distanceTo(gardenerHelpLocation) * 1.5 < rc
								.getLocation().distanceTo(getDestination()))) {
							setDestination(gardenerHelpLocation);
							currentDestinationType = GARDENER_HELP_PRIORITY;
							dodgeBullets();
							tryToMoveToDestinationTwo();
							RobotInfo target = getHighestPriorityTarget(enemyRobots, false);
							if (target != null) {
								// System.out.println("Found a target");
								// Broadcast the location of the target
								BroadcastManager.saveLocation(rc, target.location,
										BroadcastManager.LocationInfoType.ENEMY);
								tryAndFireAShot(target);
							}
							endTurn();
							continue;
						}
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
						if (getDestination() == null || (rc.getLocation().distanceTo(archonHelpLocation) * 1.5 < rc
								.getLocation().distanceTo(getDestination()))) {
							setDestination(archonHelpLocation);
							currentDestinationType = ARCHON_HELP_PRIORITY;
							dodgeBullets();
							tryToMoveToDestinationTwo();
							RobotInfo target = getHighestPriorityTarget(enemyRobots, false);
							if (target != null) {
								// Broadcast the location of the target
								BroadcastManager.saveLocation(rc, target.location,
										BroadcastManager.LocationInfoType.ENEMY);
								tryAndFireAShot(target);
							}
							endTurn();
							continue;
						}
					}
				}

				// Attack mode
				if (currentDestinationType < MOVE_TOWARDS_COMBAT_PRIORITY) {
					MapLocation enemyLocation = getEnemyLocation();
					if (enemyLocation != null) {
						// boolean success = moveTowardsCombat(enemyLocation,
						// BroadcastManager.LocationInfoType.ENEMY);

						if (currentDestinationType < MOVE_TOWARDS_COMBAT_PRIORITY
								|| enemyLocation.x != getDestination().x || enemyLocation.y != getDestination().y) {
							if (getDestination() == null || (rc.getLocation().distanceTo(enemyLocation) * 1.5 < rc
									.getLocation().distanceTo(getDestination()))) {
								setDestination(enemyLocation);
								currentDestinationType = MOVE_TOWARDS_COMBAT_PRIORITY;
								dodgeBullets();
								boolean success = tryToMoveToDestinationTwo();
								RobotInfo target = getHighestPriorityTarget(enemyRobots, false);
								if (target != null) {
									// System.out.println("Found a target");
									// Broadcast the location of the target
									BroadcastManager.saveLocation(rc, target.location,
											BroadcastManager.LocationInfoType.ENEMY);
									tryAndFireAShot(target);
								}

								if (success) {
									endTurn();
									continue;
								}
							}
						}
					}
				}

				// Discovery mode, move randomly
				if (currentDestinationType > 0) {
					tryToMoveToDestinationTwo();
					endTurn();
				} else {
					moveIntelligentlyRandomly();
					endTurn();
				}

			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean shouldClearLocation(MapLocation destination) throws GameActionException {
		Direction toMove = rc.getLocation().directionTo(destination);
		RobotInfo frontRobot = rc.senseRobotAtLocation(rc.getLocation().add(toMove));
		// System.out.println("run");
		return this.getClosestDistance() <= DISTANCE_TO_CLEAR_DESTINATION
				|| rc.getLocation().distanceTo(destination) <= DISTANCE_TO_CLEAR_DESTINATION
				|| (rc.canSenseLocation(destination) && frontRobot != null && frontRobot.team == allyTeam);
	}

	private void checkVisitedArchonLocation() {
		// Avoid out of bounds index error
		if (archonVisitedIndex >= enemyArchonLocations.length) {
			return;
		}
		if (rc.getLocation().distanceTo(enemyArchonLocations[archonVisitedIndex]) < type.sensorRadius * 0.8
				&& rc.senseNearbyRobots(-1, enemyTeam).length == 0) {
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

	private Direction moveDir;

	private void moveIntelligentlyRandomly() throws GameActionException {
		if (moveDir == null) {
			moveDir = Utils.randomDirection();
		}
		moveDir = moveWithRandomBounce(moveDir);
	}

	private void executeCombat() throws GameActionException {

		boolean result = dodgeBullets();
		if (!result) {
			// Move towards the enemy, especially if it's an econ unit
			RobotInfo robotInfo = (RobotInfo) getClosestBody(rc.senseNearbyRobots(-1, enemyTeam));
			if (robotInfo != null) {
				if (((robotInfo.getType().equals(RobotType.GARDENER) || robotInfo.getType().equals(RobotType.ARCHON))
						&& robotInfo.getLocation().distanceTo(rc.getLocation()) > 4.2)
						|| (robotInfo.getType().equals(RobotType.LUMBERJACK)
								&& robotInfo.getLocation().distanceTo(rc.getLocation()) > 3.5)) {
					move(getDirectionTowards(robotInfo.getLocation()));
				}
			}
			/*
			 * for (RobotInfo robotInfo : enemyRobots) { if
			 * (robotInfo.getType().equals(RobotType.GARDENER) ||
			 * robotInfo.getType().equals(RobotType.ARCHON) ||
			 * (robotInfo.getType().equals(RobotType.LUMBERJACK) &&
			 * robotInfo.getLocation().distanceTo(rc.getLocation()) > 3)) {
			 * move(getDirectionTowards(robotInfo.getLocation())); break; } }
			 */
		}

		RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

		// Shoot
		// System.out.println("Start bytecode for highestPriorityTarget = " +
		// Clock.getBytecodeNum());
		boolean hitTrees = rc.getTeamBullets() > 60;
		RobotInfo target = getHighestPriorityTarget(enemyRobots, hitTrees);
		// System.out.println("End bytecode for highestPriorityTarget = " +
		// Clock.getBytecodeNum());

		if (target == null) {
			target = enemySeenLastRound;
		} else {
			enemySeenLastRound = target;
			enemyCounter = COMBAT_MEMORY;
		}

		if (target != null) {
			// System.out.println("Found a target");
			// Broadcast the location of the target
			BroadcastManager.saveLocation(rc, target.location, BroadcastManager.LocationInfoType.ENEMY);
			tryAndFireAShot(target);
		} else {

			// Try to get closer to the enemy
			// System.out.println("Found no target");
			target = (RobotInfo) getClosestBody(enemyRobots);
			if (target != null) {
				BroadcastManager.saveLocation(rc, target.location, LocationInfoType.ENEMY);
				Direction toMove = getDirectionTowards(target.location);
				if (toMove != null) {
					if (smartCanMove(toMove)) {
						move(toMove);
					}
				} else {
					moveWithRandomBounce(Utils.randomDirection());
				}
			} else {
				moveWithRandomBounce(Utils.randomDirection());
			}
		}
	}

	private boolean shouldFireTriShot(RobotInfo target) throws GameActionException {
		MapLocation currLoc = rc.getLocation();

		if (getFirstHitTeamAprox(currLoc, currLoc.directionTo(target.location).rotateLeftDegrees(20), true) == rc
				.getTeam()
				|| getFirstHitTeamAprox(currLoc, currLoc.directionTo(target.location).rotateRightDegrees(20),
						true) == rc.getTeam()) {

			return false;
		}

		return rc.getLocation().distanceTo(target.location) >= 5 && rc.getLocation().distanceTo(target.location) < 12;
	}

	private boolean shouldFirePentadShot(RobotInfo target) throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		if (getFirstHitTeamAprox(currLoc, currLoc.directionTo(target.location).rotateLeftDegrees(30), true) == rc
				.getTeam()
				|| getFirstHitTeamAprox(currLoc, currLoc.directionTo(target.location).rotateRightDegrees(30),
						true) == rc.getTeam()) {

			return false;
		}

		return true;
		// return rc.getLocation().distanceTo(target.location) < 5;
	}

	private void tryAndFireAShot(RobotInfo target) throws GameActionException {
		if (target == null) {
			return;
		}
		Direction shotDir = rc.getLocation().directionTo(target.location);

		if (target.type.equals(RobotType.ARCHON)) {
			if (rc.canFireSingleShot()) {
				rc.fireSingleShot(shotDir);
				return;
			}
		}
		/*
		 * float randSpread = (float) (Math.random() * 1); if (Math.random() <
		 * .5) { shotDir = shotDir.rotateLeftDegrees(randSpread); } else {
		 * shotDir = shotDir.rotateRightDegrees(randSpread); }
		 */
		if (rc.canFireSingleShot()) {
			if (shouldFirePentadShot(target) && rc.canFirePentadShot()) {
				rc.firePentadShot(shotDir);
			} else if (shouldFireTriShot(target) && rc
					.canFireTriadShot()/* && getBulletGenerationSpeed() > 3 */) {
				rc.fireTriadShot(shotDir);
			} else {
				rc.fireSingleShot(shotDir);
			}
		}
	}

}
