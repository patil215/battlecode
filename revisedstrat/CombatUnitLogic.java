package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

import java.util.Arrays;

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
				if (currentDestinationType < MOVE_TOWARDS_COMBAT_PRIORITY) {
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

		// Move
		BulletInfo hittingBullet = getTargetingBullet(surroundingBullets);
		if (hittingBullet != null) {
			System.out.println("In combat");
			MapLocation safeLocation = getBulletAvoidingLocation(rc);
			if (safeLocation != null) {
				move(safeLocation);
			}
		}

		// Shoot
		RobotInfo target = getHighestPriorityTarget(enemyRobots, true);
		if (target != null) {
			System.out.println("Found a target");
			// Broadcast the location of the target
			BroadcastManager.saveLocation(rc, target.location, BroadcastManager.LocationInfoType.ENEMY);
			tryAndFireAShot(target);
		} else {
			// Try to get closer to the enemy
			System.out.println("Found no target");
			target = (RobotInfo) getClosestBody(enemyRobots);
			if (target != null) {
				BroadcastManager.saveLocation(rc, target.location, LocationInfoType.ENEMY);
				Direction toMove = moveTowards(target.location);
				if (toMove != null) {
					if (rc.canMove(toMove)) {
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

	private void tryAndFireASmartShot(RobotInfo target) throws GameActionException {
		RobotInfo[] enemies = Arrays.stream(rc.senseNearbyRobots())
				.filter(r -> r.team == rc.getTeam().opponent()).toArray(RobotInfo[]::new);


	}

	private boolean shouldFireTriShot(RobotInfo target) {
		MapLocation currLoc = rc.getLocation();
		MapLocation targetLoc = target.getLocation();
		Direction toTarget = currLoc.directionTo(targetLoc);

		int numHit = 0;
		Team opponent = rc.getTeam().opponent();
		float radius = rc.getType().sensorRadius;

		if (getFirstHitTeam(currLoc, toTarget.rotateLeftDegrees(-20),
				true, radius) == opponent) {
			numHit++;
		}

		if (getFirstHitTeam(currLoc, toTarget.rotateLeftDegrees(20),
				true, radius) == rc.getTeam().opponent()) {
			numHit++;
		}

		return numHit > 0;
	}

	private boolean shouldFirePentadShot(RobotInfo target) {
		MapLocation currLoc = rc.getLocation();
		MapLocation targetLoc = target.getLocation();
		Direction toTarget = currLoc.directionTo(targetLoc);

		int numHit = 0;
		Team opponent = rc.getTeam().opponent();
		float radius = rc.getType().sensorRadius;

		if (getFirstHitTeam(currLoc, toTarget.rotateLeftDegrees(-30),
				true, radius) == opponent) {
			numHit++;
		}

		if (getFirstHitTeam(currLoc, toTarget.rotateLeftDegrees(30),
				true, radius) == rc.getTeam().opponent()) {
			numHit++;
		}

		return numHit > 0;
	}

	private void tryAndFireAShot(RobotInfo target) throws GameActionException {
		Direction shotDir = rc.getLocation().directionTo(target.location);
		if (rc.canFireSingleShot()) {
			if (shouldFirePentadShot(target) && rc.canFirePentadShot()) {
				rc.firePentadShot(shotDir);
			} else {
				if (shouldFireTriShot(target) && rc.canFireTriadShot()) {
					rc.fireTriadShot(shotDir);
				} else {
					rc.fireSingleShot(shotDir);
				}
			}
		}
	}

//	private void tryAndFireAShot(RobotInfo target) throws GameActionException {
////		tryAndFireASmartShot(target);
//		if (rc.canFirePentadShot() && rc.getTeamBullets() > 70) {
//			rc.firePentadShot(rc.getLocation().directionTo(target.location));
//		} else if (rc.canFireTriadShot() && rc.getTeamBullets() > 30) {
//			rc.fireTriadShot(rc.getLocation().directionTo(target.location));
//		} else if (rc.canFireSingleShot()) {
//			rc.fireSingleShot(rc.getLocation().directionTo(target.location));
//		}
//	}

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

	private static class Shot {
		public enum Type {
			SINGLE, TRI, PENTA;
		}

		Direction direction;
		Type type;

		public Shot(Direction direction, Type type) {
			this.direction = direction;
			this.type = type;
		}
	}

	private double getPrioritySum(BodyInfo... bodies) {
		double sum = 0;

		for (BodyInfo body : bodies) {
			// Friendly trees have negative multiplier of 1
			// Friendly allies have negative multiplier of 1.5
			// Enemies have multiplier of 1
			// Enemy trees have multiplier of 0.75

			if (body == null) {
				continue;
			}

			if (body.isRobot()) {
				RobotInfo robot = (RobotInfo) body;
				if (robot.getTeam().equals(rc.getTeam())) {
					sum += -1.5 * Math.abs(robot.getType().attackPower)
							/ (Math.max(robot.health, 1) * rc.getLocation().distanceTo(robot.getLocation()));
				} else {
					sum += Math.abs(robot.getType().attackPower)
							/ (Math.max(robot.health, 1) * rc.getLocation().distanceTo(robot.getLocation()));
				}
			}
		}
		return sum;
	}

	private Shot getOptimalShot() {
		int startBytecode = Clock.getBytecodeNum();
		AimManager aimManager = new AimManager(rc);

		double maxSingleSum = Double.NEGATIVE_INFINITY;
		int maxSingleDirection = 0;

		double maxTripleSum = Double.NEGATIVE_INFINITY;
		int maxTripleDirection = 0;

		double maxPentaSum = Double.NEGATIVE_INFINITY;
		int maxPentaDirection = 0;

		for (int i = 0; i < 360; i += 10) {
			int start = Clock.getBytecodeNum();
			System.out.println("beginning query at " + start);
			BodyInfo singleShot = aimManager.getTargetSingleShot(new Direction(i));
			BodyInfo[] tripleShot = aimManager.getTargetTripleShot(new Direction(i));
			BodyInfo[] pentaShot = aimManager.getTargetPentadShot(new Direction(i));

			double singleSum = getPrioritySum(singleShot);
			if (singleSum > maxSingleSum) {
				maxSingleDirection = i;
				maxSingleSum = singleSum;
			}

			double tripleSum = getPrioritySum(tripleShot);
			if (tripleSum > maxTripleSum) {
				maxTripleDirection = i;
				maxTripleSum = tripleSum;
			}

			double pentaSum = getPrioritySum(pentaShot);
			if (pentaSum > maxPentaSum) {
				maxPentaDirection = i;
				maxPentaSum = pentaSum;
			}
		}

		double bestShotSum = maxSingleSum;
		Shot shot = new Shot(new Direction(maxSingleDirection), Shot.Type.SINGLE);

		if (maxTripleSum - 0.01 > bestShotSum) {
			bestShotSum = maxTripleSum;
			shot.direction = new Direction(maxTripleDirection);
			shot.type = Shot.Type.TRI;
		}

		if (maxPentaSum - 0.01 > bestShotSum) {
			shot.direction = new Direction(maxPentaDirection);
			shot.type = Shot.Type.PENTA;
		}

		if (bestShotSum <= 0) {
			return null;
		}

		return shot;

	}

}
