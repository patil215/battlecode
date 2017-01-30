package revisedstrat;

import battlecode.common.*;

import java.util.Arrays;

import static revisedstrat.Utils.randomDirection;

/**
 * Created by patil215 on 1/12/17.
 */
public abstract class RobotLogic {

	public RobotController rc;

	private static int ARCHON_IGNORE_ROUND = 150;

	private static final float NO_INTERSECT = Float.NEGATIVE_INFINITY;

	private static boolean isLeftUnit;

	private static MapLocation destination;

	private static float distanceToDestination;

	protected static final float DISTANCE_TO_CLEAR_DESTINATION = 2;

	private static Direction lastDirection;

	private static boolean needToSetDirection;

	private static boolean dodgeLeft = Math.random() < .5;

	public static MapLocation startLocation;

	public Team allyTeam;
	public Team enemyTeam;

	public MapLocation[] allyArchonLocations;
	public MapLocation[] enemyArchonLocations;

	public RobotType type;

	public RobotLogic(RobotController rc) {
		this.rc = rc;
		isLeftUnit = Math.random() > .5;
		needToSetDirection = true;
		allyTeam = rc.getTeam();
		enemyTeam = rc.getTeam().opponent();
		allyArchonLocations = rc.getInitialArchonLocations(allyTeam);
		enemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
		type = rc.getType();
		startLocation = rc.getLocation();
		// surpriseCalcs();
	}

	public abstract void run();

	public MapLocation getRandomEnemyInitialArchonLocation() {
		return enemyArchonLocations[(int) (enemyArchonLocations.length * (Math.random()))];
	}

	/*
	 * This method takes in a direction and attempts to move in this direction.
	 * If it cannot, then new directions are tried. By the end of this method,
	 * either the robot has moved in the returned angle, or no movable angle was
	 * found.
	 */
	public Direction moveWithRandomBounce(Direction move) throws GameActionException {
		if (smartCanMove(move)) {
			move(move);
		} else {
			for (int count = 0; count < 20 && !smartCanMove(move); count++) {
				move = randomDirection();
			}
			if (smartCanMove(move)) {
				move(move);
			}
		}
		return move;
	}

	public Direction moveWithDiagonalBounce(Direction move) throws GameActionException {
		if (smartCanMove(move)) {
			move(move);
		} else {
			for (int count = 0; count < 8 && !smartCanMove(move); count++) {
				move = Utils.diagonalDirection();
			}
			for (int count = 0; count < 12 && !smartCanMove(move); count++) {
				move = Utils.randomDirection();
			}
			if (smartCanMove(move)) {
				move(move);
			}
		}
		return move;
	}

	public boolean smartCanMove(Direction toMove) throws GameActionException {
		if (rc.getType() != RobotType.TANK) {
			return rc.canMove(toMove);
		} else if (rc.canMove(toMove)) {
			System.out.println("We can move, but we are a tank.");
			if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(toMove, rc.getType().strideRadius),
					rc.getType().bodyRadius)) {
				System.out.println("The circle is occupied");
				TreeInfo[] possibleHitAllyTrees = rc
						.senseNearbyTrees(rc.getType().strideRadius + rc.getType().bodyRadius, rc.getTeam());
				for (TreeInfo t : possibleHitAllyTrees) {
					if (Math.abs(toMove.degreesBetween(rc.getLocation().directionTo(t.location))) < 90) {
						return false;
					}
				}
				return true;
			} else{
				return true;
			}
		}
		return false;
	}

	/*
	 * Sets the destination to the specified location. Used with
	 * tryToMoveToDestination for path finding.
	 * 
	 * Passing in null clears the destination.
	 */
	public void setDestination(MapLocation newDesination) {
		needToSetDirection = true;
		if (newDesination != null) {
			destination = newDesination;
			distanceToDestination = Float.MAX_VALUE;
			lastDirection = rc.getLocation().directionTo(destination);
		} else {
			destination = null;
			distanceToDestination = 0;
			lastDirection = null;
		}
	}

	public MapLocation getDestination() {
		return destination;
	}

	public float getClosestDistance() {
		return distanceToDestination;
	}

	private boolean findBestDirection(MapLocation destination) throws GameActionException {
		Direction toMove = rc.getLocation().directionTo(destination);
		for (int count = 0; count < 180; count += 1) {
			if (smartCanMove(toMove.rotateLeftDegrees(count))) {
				return true;
			} else if (smartCanMove(toMove.rotateRightDegrees(count))) {
				return false;
			}
		}
		System.out.println("Reached. This should never happen if a unit is not trapped.");
		return false;
	}

	public boolean move(MapLocation location) throws GameActionException {
		if (!rc.hasMoved()) {
			rc.move(location);
			return true;
		}
		return false;
	}

	public boolean move(Direction direction) throws GameActionException {
		if (!rc.hasMoved() && smartCanMove(direction)) {
			rc.move(direction);
			return true;
		}
		return false;
	}

	public boolean move(Direction direction, float distance) throws GameActionException {
		if (!rc.hasMoved() && smartCanMove(direction, distance)) {
			rc.move(direction, distance);
			return true;
		}
		return false;
	}

	private boolean smartCanMove(Direction direction, float distance) throws GameActionException {
		if (rc.getType() != RobotType.TANK) {
			return rc.canMove(direction, distance);
		} else if (rc.canMove(direction, distance)) {
			if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(direction, distance),
					rc.getType().bodyRadius)) {
				TreeInfo[] possibleHitAllyTrees = rc
						.senseNearbyTrees(rc.getType().strideRadius + rc.getType().bodyRadius, rc.getTeam());
				for (TreeInfo t : possibleHitAllyTrees) {
					if (Math.abs(direction.degreesBetween(rc.getLocation().directionTo(t.location))) < 90) {
						return false;
					}
				}
				return true;
			}else{
				return true;
			}
		}
		return false;
	}

	public void endTurn() throws GameActionException {
		tryAndShakeATree();
		econWinIfPossible();

		// Dump all bullets if game about to end to get tiebreaker
		if (rc.getRoundLimit() - rc.getRoundNum() < 2) {
			float bulletCount = rc.getTeamBullets();
			bulletCount /= rc.getVictoryPointCost();
			int donateCount = (int) bulletCount;
			donateCount *= rc.getVictoryPointCost();
			rc.donate(donateCount);
		} else {
			float bullets = rc.getTeamBullets();
			if (bullets > 250) {
				int bulletCount = (int) ((bullets - 250) / rc.getVictoryPointCost());
				bulletCount *= rc.getVictoryPointCost();
				rc.donate(bulletCount);
			}
		}
		// drawDots();
		Clock.yield();
	}

	/*
	 * If it is possible to move towards the target, then this method returns
	 * the best angle to do so with. Otherwise, null is returned. The method
	 * will also disallow angles that will result in the robot getting hit by a
	 * bullet.
	 */
	public Direction getDirectionTowards(MapLocation destination) throws GameActionException {
		if (!rc.getLocation().equals(destination)) {
			Direction toMove = rc.getLocation().directionTo(destination);
			return getDirectionTowards(toMove);
		}
		return null;
	}

	/*
	 * If it is possible to move towards the specified direction, then this
	 * method returns the best angle to do so with. Otherwise, null is returned.
	 * The method will also disallow angles that will result in the robot
	 * getting hit by a bullet.
	 */
	public Direction getDirectionTowards(Direction toMove) throws GameActionException {
		if (smartCanMove(toMove)) {
			return toMove;
		} else {
			BulletInfo[] bullets = rc.senseNearbyBullets();
			for (int deltaAngle = 0; deltaAngle < 360; deltaAngle += 10) {
				if (isLeftUnit) {
					Direction leftDir = toMove.rotateLeftDegrees(deltaAngle);
					if (smartCanMove(leftDir)
							&& !willGetHitByABullet(rc.getLocation().add(leftDir, type.strideRadius), bullets)) {
						return leftDir;
					}
				} else {
					Direction rightDir = toMove.rotateRightDegrees(deltaAngle);
					if (smartCanMove(rightDir)
							&& !willGetHitByABullet(rc.getLocation().add(rightDir, type.strideRadius), bullets)) {
						return rightDir;
					}
				}
			}
		}
		return null;
	}

	/*
	 * This method should be called at the end of each run statement, if
	 * bytecode can be afforded. It will find a tree that can be shaked and
	 * shake it.
	 */
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

	public Team getFirstHitTeamAprox(MapLocation location, Direction direction, boolean hitTrees)
			throws GameActionException {
		MapLocation testLocation = location.add(direction, type.bodyRadius + GameConstants.BULLET_SPAWN_OFFSET);
		while (rc.canSenseLocation(testLocation)) {
			if (rc.isLocationOccupied(testLocation)) {
				RobotInfo targetRobot = rc.senseRobotAtLocation(testLocation);
				if (targetRobot != null) {
					return targetRobot.team;
				}
				TreeInfo targetTree = rc.senseTreeAtLocation(testLocation);
				if (targetTree != null) {
					if (hitTrees) {
						return targetTree.getTeam();
					} else {
						return Team.NEUTRAL;
					}
				} else {
					System.out.println("This should never happen");
					return Team.NEUTRAL;
				}
			} else {
				float DELTA_BULLET_DISTANCE = .5f;
				testLocation = testLocation.add(direction, DELTA_BULLET_DISTANCE);
			}
		}
		return Team.NEUTRAL;
	}

	/*
	 * This method returns a bullet that will hit the player in its current
	 * position. If multiple bullets will hit the target, only the closest
	 * bullet is returned. Returns null if no bullet will hit the target.
	 */
	protected BulletInfo getTargetingBullet(BulletInfo[] bullets) {
		RobotInfo player = new RobotInfo(-1, null, type, rc.getLocation(), 1, 1, 1);

		float minDistance = Float.MAX_VALUE;
		BulletInfo closestBullet = null;

		for (BulletInfo bullet : bullets) {
			float distance = rc.getLocation().distanceTo(bullet.getLocation());
			if (distance < minDistance
					&& getIntersectionDistance(bullet.location, bullet.dir, player) != NO_INTERSECT) {
				closestBullet = bullet;
				minDistance = distance;
			}
		}
		return closestBullet;
	}

	/**
	 * Returns whether a bullet will hit a player at a specific location.
	 */
	protected boolean willHit(BulletInfo bullet, RobotInfo player) {
		return getIntersectionDistance(bullet.location, bullet.dir, player) != NO_INTERSECT;
	}

	/*
	 * This method determines if a character will be hit by any bullet if it
	 * moves to a particular location. The method takes in the location that the
	 * player wants to move to and an array representing all sensed bullets.
	 *
	 * The method returns true iff a bullet will hit the player in the next
	 * round, given that the player moves to the specified location
	 */
	protected boolean willGetHitByABullet(MapLocation playerLocation, BulletInfo[] bullets) {
		RobotInfo player = new RobotInfo(-1, null, type, playerLocation, 1, 1, 1);
		for (BulletInfo bullet : bullets) {
			if (getIntersectionDistance(bullet.location, bullet.dir, player) != NO_INTERSECT) {
				return true;
			}
		}
		return false;
	}

	/*
	 * This method takes the location and direction of a bullet, as well as the
	 * BodyInfo of the target to be intersected.
	 *
	 * The method returns the distance at which the target will be intersected.
	 * If the target is never intersected, this method returns
	 * Float.NEGATIVE_INFINITY.
	 */
	private float getIntersectionDistance(MapLocation location, Direction direction, BodyInfo target) {

		float targetRadius = target.getRadius();

		// The x and y coordinates of the center of the target.
		float xTarget = target.getLocation().x;
		float yTarget = target.getLocation().y;

		// The x and y coordinates of the bullet's starting point.
		float xStart = location.x;
		float yStart = location.y;

		float dx = xTarget - xStart;
		float dy = yTarget - yStart;
		// Compute the shortest distance between the bullet and the center of
		// the target
		float angle = direction.radians;
		float dist = (float) Math.abs(Math.sin(angle) * (dx) - Math.cos(angle) * dy);

		// If the shortest distance is too large, the bullet won't ever
		// intersect the target
		if (dist > targetRadius) {
			return NO_INTERSECT;
		}

		if (Math.abs(location.directionTo(rc.getLocation()).degreesBetween(direction)) > 90) {
			return NO_INTERSECT;
		}

		// Compute the distance the bullet travels to get to the point of
		// closest approach
		float distSquared = dist * dist;
		float lengthToClosestApproach = (float) Math.sqrt((dx) * (dx) + (dy) * (dy) - distSquared);

		// Compute the distance the bullet travels from the intersection point
		// to the closest approach
		float excessDistance = (float) Math.sqrt(targetRadius * targetRadius - distSquared);

		return lengthToClosestApproach - excessDistance;
	}

	public boolean closeToLocationAndNoEnemies(RobotController rc, MapLocation location) throws GameActionException {
		if (rc.getLocation().distanceTo(location) < (type.sensorRadius * .8)
				&& rc.senseNearbyRobots(-1, enemyTeam).length == 0) {
			return true;
		}
		return false;
	}

	public boolean closeToLocationAndJustArchons(RobotController rc, MapLocation location) throws GameActionException {
		boolean enemy = false;
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemyTeam);
		for (RobotInfo info : nearbyRobots) {
			if (info.getType() != RobotType.ARCHON) {
				enemy = true;
				break;
			}
		}
		if (rc.getLocation().distanceTo(location) < (type.sensorRadius * .8) && !enemy) {
			return true;
		}
		return false;
	}

	/*
	 * Code used to find the highest priority target. If no sutable targets are
	 * found, null is returned. This method only returns a target if it can be
	 * fired at from the robot's current position.
	 */
	RobotInfo getHighestPriorityTarget(RobotInfo[] enemies, boolean hitTrees) throws GameActionException {
		if (enemies.length == 0) {
			return null;
		}

		int maxIndex = -1;
		double maxPriority = -1;

		for (int index = 0; index < enemies.length; index++) {

			double priority = 0;

			if (enemies[index].getType().canAttack()) {
				priority = enemies[index].getType().attackPower / (Math.max(enemies[index].health, 1)
						* rc.getLocation().distanceTo(enemies[index].getLocation()));
			}

			// System.out.println("Priority is: " + priority);

			// TODO: Refactor
			if ((priority > maxPriority || (maxPriority == 0 && enemies[index].health < enemies[maxIndex].health))) {

				// Don't attack archons at the start of the game.
				if (enemies[index].type == RobotType.ARCHON && rc.getRoundNum() < ARCHON_IGNORE_ROUND) {
					continue;
				}

				Direction toEnemy = rc.getLocation().directionTo(enemies[index].location);
				float spawnOffset = type.bodyRadius + GameConstants.BULLET_SPAWN_OFFSET;

				MapLocation bulletSpawnPoint = rc.getLocation().add(toEnemy, spawnOffset);

				// Only attack if we will hit an enemy.
				if (getFirstHitTeamAprox(bulletSpawnPoint, toEnemy, hitTrees) == enemyTeam) {
					maxIndex = index;
					maxPriority = priority;
					System.out.println("We have found a new target");
				}
			}
		}

		if (maxIndex >= 0) {
			return enemies[maxIndex];
		}
		// TODO: actually handle
		return null;
	}

	/*
	 * Returns the body that is closest to the robot tha calls the method. The
	 * passed in array should contain all bodies for analysis.
	 */
	protected BodyInfo getClosestBody(BodyInfo[] foes) {
		if (foes.length == 0) {
			return null;
		}

		BodyInfo closestEnemy = null;
		float closestDistance = Float.MAX_VALUE;

		for (BodyInfo enemy : foes) {
			// Ignore enemy archons at the start of the game.
			if (enemy instanceof RobotInfo) {

				RobotInfo robot = (RobotInfo) enemy;
				if (robot.type == RobotType.ARCHON && rc.getRoundNum() < ARCHON_IGNORE_ROUND) {
					continue;
				}
			}

			float dist = rc.getLocation().distanceTo(enemy.getLocation());
			if (dist < closestDistance) {
				closestEnemy = enemy;
				closestDistance = dist;
			}
		}

		return closestEnemy;
	}

	protected void econWinIfPossible() throws GameActionException {
		if (rc.getTeamBullets() >= (GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints())
				* rc.getVictoryPointCost()) {
			rc.donate(rc.getTeamBullets());
		}
	}

	private Direction findDensestDirection(BulletInfo[] bullets) {
		float avgX = 0, avgY = 0;
		MapLocation currLocation = rc.getLocation();

		for (BulletInfo bullet : bullets) {
			Direction d = currLocation.directionTo(bullet.location);
			avgX += d.getDeltaX(1);
			avgY += d.getDeltaY(1);
		}

		avgX /= bullets.length;
		avgY /= bullets.length;

		return new Direction(avgX, avgY);
	}

	private final float ANGLE_EPSILON = 0.01f;

	protected boolean incomingBullet(BulletInfo[] bullets) {
		for (BulletInfo bullet : bullets) {
			float angleTolerance = (float) (Math.abs(
					Math.asin(type.bodyRadius / bullet.getLocation().distanceTo(rc.getLocation()))) + ANGLE_EPSILON);
			if (Math.abs(bullet.location.directionTo(rc.getLocation()).radiansBetween(bullet.dir)) < angleTolerance) {
				return true;
			}
		}

		return false;
	}

	protected void moveAndDodge(MapLocation enemy, BulletInfo[] bullets) throws GameActionException {
		MapLocation currLocation = rc.getLocation();
		Direction toEnemy = currLocation.directionTo(enemy);

		float minDamage = rc.getHealth();
		int bestAngle = -40;
		for (int angle = -40; angle < 40; angle += 10) {
			MapLocation expectedLocation = currLocation.add(toEnemy.rotateLeftDegrees(angle),
					(float) type.strideRadius);
			float damage = expectedDamage(bullets, expectedLocation);

			if (damage < minDamage) {
				bestAngle = angle;
				minDamage = damage;
			}
		}

		move(toEnemy.rotateLeftDegrees(bestAngle));
	}

	protected void dodge(BulletInfo[] bullets) throws GameActionException {
		// bullets = getAllIncomingBullets(bullets, rc.getLocation(), 20);
		BulletInfo[] predictNext = Arrays.stream(bullets).map(b -> new BulletInfo(b.getID(),
				b.location.add(b.getDir(), b.speed), b.getDir(), b.getSpeed(), b.getDamage()))
				.toArray(BulletInfo[]::new);
		Direction densestDirection = findDensestDirection(bullets);
		MapLocation currentLocation = rc.getLocation();

		float stationaryImminentDanger = getImminentDanger(bullets, currentLocation);

		int safestAngle = 0;
		float leastDanger = stationaryImminentDanger;
		for (int angle = 90; angle < 270; angle += 10) {

			float expectedDanger = getImminentDanger(predictNext,
					currentLocation.add(densestDirection.rotateLeftDegrees(angle)));

			if (expectedDanger < leastDanger && smartCanMove(densestDirection.rotateLeftDegrees(angle))) {
				leastDanger = expectedDanger;
				safestAngle = angle;
			}
		}

		Direction toMove = densestDirection.rotateLeftDegrees(safestAngle);

		if (smartCanMove(toMove)) {
			move(toMove);
		}
	}

	private float getImminentDanger(BulletInfo[] bullets, MapLocation loc) {
		float danger = 0;
		RobotInfo player = new RobotInfo(-1, null, type, loc, 1, 1, 1);
		float totalDamage = 0;
		for (BulletInfo bullet : bullets) {
			danger += bullet.getDamage() / loc.distanceTo(bullet.getLocation());
			if (willHit(bullet, player)) {
				totalDamage += bullet.damage;
			}
		}

		return danger * totalDamage;
	}

	protected boolean inDanger() {
		RobotInfo[] foes = rc.senseNearbyRobots(-1, this.enemyTeam);
		for (RobotInfo enemy : foes) {
			if (enemy.getType() != RobotType.ARCHON && enemy.getType() != RobotType.GARDENER) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method returns the expected damage which a specific location will
	 * get in the next round, based on the current bullet information
	 * 
	 * @param bullets
	 * @return
	 */
	protected float expectedDamage(BulletInfo[] bullets, MapLocation loc) {
		RobotInfo player = new RobotInfo(-1, null, type, loc, 1, 1, 1);
		float totalDamage = 0;
		for (BulletInfo bullet : bullets) {
			if (willHit(bullet, player)) {
				totalDamage += bullet.damage;
			}
		}

		return totalDamage;
	}

	private boolean dodgeTangent(BulletInfo toDodge, MapLocation[][] bulletSegments) throws GameActionException {
		Direction toBullet = rc.getLocation().directionTo(toDodge.location);
		Direction toTry;
		for (int rotate = 85; rotate <= 130; rotate += 10) {
			if (dodgeLeft) {
				toTry = toBullet.rotateLeftDegrees(rotate);
			} else {
				toTry = toBullet.rotateRightDegrees(rotate);
			}
			if (smartCanMove(toTry, type.strideRadius)) {
				move(toTry, type.strideRadius);
				return true;
			}
			if (dodgeLeft) {
				toTry = toBullet.rotateRightDegrees(rotate);
			} else {
				toTry = toBullet.rotateLeftDegrees(rotate);
			}
			if (smartCanMove(toTry, type.strideRadius)) {
				move(toTry, type.strideRadius);
				return true;
			}
		}
		return false;
	}

	public boolean dodgeBullets() throws GameActionException {
		BulletInfo[] surroundingBullets = rc.senseNearbyBullets(type.strideRadius + type.bodyRadius + 3);

		if (incomingBullet(surroundingBullets)) {
			MapLocation[][] segments = getSegments(surroundingBullets);

			// Short term dodge
			if (bulletIntersecting(segments)) {
				MapLocation safeLocation = getBulletAvoidingLocation(segments, 8000);
				if (safeLocation != null) {
					move(safeLocation);
					return true;
				}
			}

			// Try to move long term by going perpendicular
			BulletInfo hittingBullet = getTargetingBullet(surroundingBullets);
			if (hittingBullet != null) {
				boolean result = dodgeTangent(hittingBullet, segments);
				if (result) {
					return true;
				}
			}

		}
		return false;
	}

	private MapLocation[] getBulletLineSegment(BulletInfo bullet) {
		MapLocation[] endpoints = new MapLocation[2];
		endpoints[0] = new MapLocation(bullet.getLocation().x, bullet.getLocation().y);
		endpoints[1] = bullet.getLocation().add(bullet.getDir(), bullet.getSpeed());
		return endpoints;
	}

	private MapLocation[][] getSegments(BulletInfo[] bullets) {
		MapLocation[][] segments = new MapLocation[bullets.length][2];

		for (int i = 0; i < bullets.length; i++) {
			segments[i] = getBulletLineSegment(bullets[i]);
		}
		return segments;
	}

	private float OTHER_OFFSET = 0.005f;

	private float shortestDistance(MapLocation location, MapLocation[] segment) {

		float x = location.x;
		float y = location.y;

		float x1 = segment[0].x;
		float y1 = segment[0].y;
		float x2 = segment[1].x;
		float y2 = segment[1].y;

		float A = x - x1;
		float B = y - y1;
		float C = x2 - x1;
		float D = y2 - y1;

		float dot = A * C + B * D;
		float len_sq = C * C + D * D;
		float param = -1;
		if (len_sq != 0) {
			// in case of 0 length line
			param = dot / len_sq;
		}

		float xx, yy;

		if (param < 0) {
			xx = x1;
			yy = y1;
		} else if (param > 1) {
			xx = x2;
			yy = y2;
		} else {
			xx = x1 + param * C;
			yy = y1 + param * D;
		}

		float dx = x - xx;
		float dy = y - yy;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/*
	 * Returns null if shortest does not exist
	 */
	private boolean bulletIntersecting(MapLocation[][] segments) {
		float intersectDistance = type.bodyRadius + OTHER_OFFSET;

		for (MapLocation[] segment : segments) {
			if (shortestDistance(rc.getLocation(), segment) < intersectDistance) {
				return true;
			}
		}

		return false;
	}

	private MapLocation getRandomLocation() {
		float bodyRadius = type.strideRadius;
		return rc.getLocation().add(Utils.randomDirection(),
				(float) (Math.random() * (type.strideRadius - (bodyRadius / 2))) + bodyRadius / 2);
	}

	/*
	 * Returns null if no location found, or the player is not going to be hit
	 * by a bullet.
	 */
	public MapLocation getBulletAvoidingLocation(MapLocation[][] bulletSegments, int bytecodeToSpend) throws GameActionException {
		int byteCodeStart = Clock.getBytecodeNum();
		while (Clock.getBytecodeNum() - byteCodeStart < bytecodeToSpend) {
			if (Clock.getBytecodeNum() < byteCodeStart) {
				break;
			}
			MapLocation startLoc = getRandomLocation();
			if (!bulletIntersecting(bulletSegments) && smartCanMove(startLoc)) {
				return startLoc;
			}
		}

		return null;
	}

	private boolean smartCanMove(MapLocation startLoc) throws GameActionException {
		if (rc.getType() != RobotType.TANK) {
			return rc.canMove(startLoc);
		} else if (rc.canMove(startLoc)) {
			System.out.println("We can move, but we are a tank");
			if (rc.isCircleOccupiedExceptByThisRobot(startLoc, rc.getType().bodyRadius)) {
				TreeInfo[] possibleHitAllyTrees = rc
						.senseNearbyTrees(rc.getType().strideRadius + rc.getType().bodyRadius, rc.getTeam());
				for (TreeInfo t : possibleHitAllyTrees) {
					if (Math.abs((rc.getLocation().directionTo(startLoc)).degreesBetween(rc.getLocation().directionTo(t.location))) < 90) {
						System.out.println("There is an ally tree that we can hit.");
						return false;
					}
				}
				System.out.println("We will hit no ally trees");
				return true;
			} else{
				return true;
			}
		}
		System.out.println("We can't move to the desired location.");
		return false;

	}

	public void printBytecodeLeft(String id) {
		System.out.println(id + ": " + Clock.getBytecodesLeft());
	}

	public Direction getDirectionAway(RobotInfo[] otherRobots) throws GameActionException {
		MapLocation loc = new MapLocation(0, 0);

		for (RobotInfo robot : otherRobots) {
			double multiplier = 1;
			if (robot.getTeam().equals(enemyTeam)) {
				multiplier = 3;
			}

			float magnitude = (float) ((1 / Math.max(rc.getLocation().distanceTo(robot.getLocation()), 1))
					* multiplier);
			loc = loc.add(rc.getLocation().directionTo(robot.getLocation()), magnitude);
		}

		float distanceMultiplier = 300;
		float distanceCheck = type.sensorRadius - 0.1f;

		if (!rc.onTheMap(rc.getLocation().add(Direction.getWest(), distanceCheck))) {
			loc = loc.add(Direction.getWest(), distanceMultiplier);
		}

		if (!rc.onTheMap(rc.getLocation().add(Direction.getNorth(), distanceCheck))) {
			loc = loc.add(Direction.getNorth(), distanceMultiplier);
		}

		if (!rc.onTheMap(rc.getLocation().add(Direction.getSouth(), distanceCheck))) {
			loc = loc.add(Direction.getSouth(), distanceMultiplier);
		}

		if (!rc.onTheMap(rc.getLocation().add(Direction.getEast(), distanceCheck))) {
			loc = loc.add(Direction.getEast(), distanceMultiplier);
		}

		MapLocation originDir = new MapLocation(0, 0);
		if (!loc.equals(originDir)) {
			return originDir.directionTo(loc).opposite();
		}
		return null;
	}

	public boolean edgeWithinRadius(float radius) throws GameActionException {
		MapLocation loc = rc.getLocation();
		float threshold = (float) Math.ceil(radius / 2);
		return !rc.onTheMap(loc.add(Direction.getNorth(), threshold))
				|| !rc.onTheMap(loc.add(Direction.getEast(), threshold))
				|| !rc.onTheMap(loc.add(Direction.getWest(), threshold))
				|| !rc.onTheMap(loc.add(Direction.getSouth(), threshold));
	}

	public float getBulletGenerationSpeed() {
		float treeHealthMultiplier = 1;
		float treeHealth = 50;
		return (rc.getTreeCount() * GameConstants.BULLET_TREE_BULLET_PRODUCTION_RATE * treeHealth
				* treeHealthMultiplier) + 2;
	}

	public boolean tryToMoveToDestinationTwo() throws GameActionException {
		if (destination == null) {
			return false;
		}
		rc.setIndicatorLine(rc.getLocation(), destination, 0, 0, 40);
		MapLocation currentLocation = rc.getLocation();
		Direction toMove = rc.getLocation().directionTo(destination);
		float currentDistance = currentLocation.distanceTo(destination);
		/*
		 * System.out.println("Current distance is: " + currentDistance +
		 * " closest distance is : " + distanceToDestination +
		 * " canMove returns " + rc.canMove(toMove) +
		 * "If the next statement is true, then this unit leans left " +
		 * isLeftUnit);
		 */
		if (currentDistance <= distanceToDestination && smartCanMove(toMove)) {
			rc.move(toMove);
			distanceToDestination = currentDistance;
			needToSetDirection = true;
			lastDirection = toMove;
			return true;
		} else if (smartCanMove(lastDirection)) {
			toMove = findAngleThatBringsYouClosestToAnObstruction(lastDirection);
			if (smartCanMove(toMove)) {
				lastDirection = toMove;
				rc.move(toMove);
				distanceToDestination = Math.min(distanceToDestination, currentDistance);
				return true;
			} else {
				toMove = getDirectionTowards(toMove);
				distanceToDestination = Math.min(distanceToDestination, currentDistance);
				return false;
			}
		} else {
			if (needToSetDirection) {
				isLeftUnit = findBestDirection(destination);
				needToSetDirection = false;
			}
			toMove = getDirectionTowards(lastDirection);
			if (toMove != null) {
				lastDirection = toMove;
				rc.move(toMove);
				return true;
			} else {
				return false;
			}
		}
	}

	private Direction findAngleThatBringsYouClosestToAnObstruction(Direction lastDirection2) throws GameActionException {
		Direction testAngle = lastDirection2;
		int directionMultiplyer;
		if (isLeftUnit) {
			directionMultiplyer = 1;
		} else {
			directionMultiplyer = -1;
		}
		for (int deltaAngle = 0; deltaAngle < 360 && smartCanMove(testAngle); deltaAngle += 5) {
			testAngle = lastDirection.rotateRightDegrees(deltaAngle * directionMultiplyer);
		}
		while (!smartCanMove(testAngle)) {
			testAngle = testAngle.rotateLeftDegrees(5 * directionMultiplyer);
		}
		return testAngle;
	}

	private final int[][] surprise = { { 1, 3 }, { 1, 4 }, { 1, 5 }, { 1, 7 }, { 1, 8 }, { 1, 9 }, { 1, 11 }, { 1, 14 },
			{ 1, 16 }, { 1, 17 }, { 2, 3 }, { 2, 7 }, { 2, 11 }, { 2, 12 }, { 2, 14 }, { 2, 16 }, { 2, 18 }, { 3, 3 },
			{ 3, 4 }, { 3, 5 }, { 3, 7 }, { 3, 8 }, { 3, 9 }, { 3, 11 }, { 3, 13 }, { 3, 14 }, { 3, 16 }, { 3, 18 },
			{ 4, 5 }, { 4, 7 }, { 4, 11 }, { 4, 14 }, { 4, 16 }, { 4, 18 }, { 5, 3 }, { 5, 4 }, { 5, 5 }, { 5, 7 },
			{ 5, 8 }, { 5, 9 }, { 5, 11 }, { 5, 14 }, { 5, 16 }, { 5, 17 }, { 7, 1 }, { 7, 4 }, { 7, 6 }, { 7, 9 },
			{ 7, 11 }, { 7, 12 }, { 7, 15 }, { 7, 16 }, { 7, 17 }, { 7, 19 }, { 7, 20 }, { 7, 21 }, { 8, 1 }, { 8, 2 },
			{ 8, 4 }, { 8, 6 }, { 8, 9 }, { 8, 11 }, { 8, 13 }, { 8, 15 }, { 8, 19 }, { 9, 1 }, { 9, 3 }, { 9, 4 },
			{ 9, 6 }, { 9, 9 }, { 9, 11 }, { 9, 13 }, { 9, 15 }, { 9, 16 }, { 9, 17 }, { 9, 19 }, { 9, 20 }, { 9, 21 },
			{ 10, 1 }, { 10, 4 }, { 10, 6 }, { 10, 9 }, { 10, 11 }, { 10, 13 }, { 10, 15 }, { 10, 21 }, { 11, 1 },
			{ 11, 4 }, { 11, 7 }, { 11, 8 }, { 11, 11 }, { 11, 12 }, { 11, 15 }, { 11, 16 }, { 11, 17 }, { 11, 19 },
			{ 11, 20 }, { 11, 21 } };
	private final double Xmax = 21.0;
	private final double Ymax = 11.0;
	private static float width;
	private static float height;

	private static float startY;
	private static float startX;

	private static float widthRatio;
	private static float heightRatio;

	private void surpriseCalcs() {
		float minX = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;

		for (MapLocation location : allyArchonLocations) {
			minX = Math.min(minX, location.x);
			minY = Math.min(minY, location.y);
			maxX = Math.max(maxX, location.x);
			maxY = Math.max(maxY, location.y);
		}

		for (MapLocation location : enemyArchonLocations) {
			minX = Math.min(minX, location.x);
			minY = Math.min(minY, location.y);
			maxX = Math.max(maxX, location.x);
			maxY = Math.max(maxY, location.y);
		}

		startY = maxY;
		startX = minX;
		width = maxX - minX;
		height = maxY - minY;

		widthRatio = (float) (width / Xmax);
		heightRatio = (float) (height / Ymax);
	}

	private void drawDots() throws GameActionException {
		while (Clock.getBytecodesLeft() > 1000) {
			int index = (int) (Math.random() * surprise.length);
			int[] coordinate = surprise[index];
			int bytecode = Clock.getBytecodeNum();
			MapLocation proposedLocation = new MapLocation(coordinate[1] * 2 + startX, startY - coordinate[0]);
			if (rc.canSenseLocation(proposedLocation) && rc.onTheMap(proposedLocation)) {
				rc.setIndicatorDot(proposedLocation, 255, 255, 255);
			}
			System.out.println("UOSEFIJE " + (Clock.getBytecodeNum() - bytecode));
		}
	}

}
