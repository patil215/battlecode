package revisedstrat;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;

import static revisedstrat.Utils.randomDirection;

/**
 * Created by patil215 on 1/12/17.
 */
public abstract class RobotLogic {

	public RobotController rc;

	private static int ARCHON_IGNORE_ROUND = 200;

	private static final float NO_INTERSECT = Float.NEGATIVE_INFINITY;

	private static boolean isLeftUnit;

	private static boolean FLAG = false;
	
	public RobotLogic(RobotController rc) {
		this.rc = rc;
		isLeftUnit = Math.random() > .5;
	}

	public abstract void run();

	public Team getEnemyTeam() {
		return rc.getTeam().opponent();
	}

	public MapLocation getRandomEnemyInitialArchonLocation() {
		MapLocation[] enemyLocs = rc.getInitialArchonLocations(getEnemyTeam());
		return enemyLocs[(int) (enemyLocs.length * (Math.random()))];
	}

	/*
	 * This method takes in a direction and attempts to move in this direction.
	 * If it cannot, then new directions are tried. By the end of this method,
	 * either the robot has moved in the returned angle, or no movable angle was
	 * found.
	 */
	public Direction moveWithRandomBounce(Direction move) throws GameActionException {
		if (rc.canMove(move)) {
			move(move);
		} else {
			for (int count = 0; count < 20 && !rc.canMove(move); count++) {
				move = randomDirection();
			}
			if (rc.canMove(move)) {
				move(move);
			}
		}
		return move;
	}

	public Direction moveWithDiagonalBounce(Direction move) throws GameActionException {
		if (rc.canMove(move)) {
			move(move);
		} else {
			for (int count = 0; count < 8 && !rc.canMove(move); count++) {
				move = Utils.diagonalDirection();
			}
			for (int count = 0; count < 12 && !rc.canMove(move); count++) {
				move = Utils.randomDirection();
			}
			if (rc.canMove(move)) {
				move(move);
			}
		}
		return move;
	}

	/*
	 * If it is possible to move towards the target, then this method returns
	 * the best angle to do so with. Otherwise, null is returned. The method
	 * will also disallow angles that will result in the robot getting hit by a
	 * bullet.
	 */
	public Direction moveTowards(MapLocation destination) {
		if (!rc.getLocation().equals(destination)) {
			Direction toMove = rc.getLocation().directionTo(destination);
			return moveTowards(toMove);
		}
		return null;
	}

	/*
	 * This method returns the bullets that will hit the player in some
	 * location. Returns null if no bullet will hit the target.
	 */
	protected BulletInfo[] getAllTargetingBullets(BulletInfo[] bullets, MapLocation location) {
		ArrayList<BulletInfo> targetingBullets = new ArrayList<>();
		RobotInfo player = new RobotInfo(-1, null, rc.getType(), location, 1, 1, 1);

		for (BulletInfo bullet : bullets) {
			if (getIntersectionDistance(bullet.location, bullet.dir, player) != NO_INTERSECT) {
				targetingBullets.add(bullet);
			}
		}

		BulletInfo[] bulletArray = new BulletInfo[targetingBullets.size()];
		return targetingBullets.toArray(bulletArray);
	}

	public boolean move(MapLocation location) throws GameActionException {
		if (!rc.hasMoved()) {
			rc.move(location);
			return true;
		}
		return false;
	}

	public void endTurn() throws GameActionException {
		tryAndShakeATree();
		econWinIfPossible();

		// Dump all bullets if game about to end to get tiebreaker
		if(rc.getRoundLimit() - rc.getRoundNum() < 2) {
			float bulletCount = rc.getTeamBullets();
			bulletCount /= 10;
			int donateCount = (int) bulletCount;
			donateCount *= 10;
			rc.donate(donateCount);
		}
		Clock.yield();
	}

	public boolean move(Direction direction) throws GameActionException {
		if (!rc.hasMoved() && rc.canMove(direction)) {
			rc.move(direction);
			return true;
		}
		return false;
	}

	public boolean move(Direction direction, float distance) throws GameActionException {
		if (!rc.hasMoved()) {
			rc.move(direction, distance);
			return true;
		}
		return false;
	}

	public void detectEnemiesAndSendHelpBroadcast() throws GameActionException {
		RobotInfo[] foes = rc.senseNearbyRobots(-1, getEnemyTeam());

		if (foes.length > 0) {
			BroadcastManager.saveLocation(rc, foes[0].location, BroadcastManager.LocationInfoType.GARDENER_HELP);
		}
	}

	/*
	 * If it is possible to move towards the specified direction, then this
	 * method returns the best angle to do so with. Otherwise, null is returned.
	 * The method will also disallow angles that will result in the robot
	 * getting hit by a bullet.
	 */
	public Direction moveTowards(Direction toMove) {
		if (rc.canMove(toMove)) {
			return toMove;
		} else {
			BulletInfo[] bullets = rc.senseNearbyBullets();
			for (int deltaAngle = 0; deltaAngle < 360; deltaAngle += 10) {
				if (isLeftUnit) {
					Direction leftDir = toMove.rotateLeftDegrees(deltaAngle);
					if (rc.canMove(leftDir)
							&& !willGetHitByABullet(rc.getLocation().add(leftDir, rc.getType().strideRadius),
									bullets)) {
						return leftDir;
					}
				} else {
					Direction rightDir = toMove.rotateRightDegrees(deltaAngle);
					if (rc.canMove(rightDir)
							&& !willGetHitByABullet(rc.getLocation().add(rightDir, rc.getType().strideRadius),
									bullets)) {
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

	/*
	 * This method takes the location and direction of a bullet.
	 *
	 * It returns the team of the first object that it will hit. If no object
	 * will be hit, this method returns NEUTRAL.
	 */
	public Team getFirstHitTeam(MapLocation location, Direction direction, boolean hitTrees, float maxDistance) {

		// Detect tree collisions.
		TreeInfo[] trees = rc.senseNearbyTrees(maxDistance);

		float minTreeDistance = Float.MAX_VALUE;
		TreeInfo hitTree = null;

		for (TreeInfo tree : trees) {
			if(FLAG || rc.getLocation().directionTo(tree.location).radiansBetween(direction) < Math.PI / 2){
				float distance = getIntersectionDistance(location, direction, tree);

				if (distance < minTreeDistance && distance != NO_INTERSECT) {
					hitTree = tree;
					minTreeDistance = distance;
				}
			}
		}

		// Detect robot collisions
		RobotInfo[] robots = rc.senseNearbyRobots(maxDistance);

		float minRobotDistance = Float.MAX_VALUE;
		RobotInfo hitRobot = null;

		for (RobotInfo robot : robots) {
			
			if(FLAG || rc.getLocation().directionTo(robot.location).radiansBetween(direction) < Math.PI / 2){
				float distance = getIntersectionDistance(location, direction, robot);

				if (distance < minRobotDistance && distance != NO_INTERSECT) {
					hitRobot = robot;
					minRobotDistance = distance;
				}
			}
		}

		// If nothing is hit, return neutral
		if (hitTree == null && hitRobot == null) {
			return Team.NEUTRAL;
		}

		// If only robots are intersected, return the nearest robot's team
		else if (hitTree == null && hitRobot != null) {
			return hitRobot.getTeam();
		}

		// If only trees are intersected, return team if hitTrees is true, otherwise neutral
		else if (hitTree != null && hitRobot == null) {
			if(hitTrees) {
				return hitTree.getTeam();
			} else {
				return Team.NEUTRAL;
			}
		}

		// If both are intersected, return the team of whichever is closer
		else {
			if (minTreeDistance < minRobotDistance) {
				return Team.NEUTRAL;
			} else {
				return hitRobot.getTeam();
			}
		}
	}

	/*
	 * This method returns a bullet that will hit the player in its current
	 * position. If multiple bullets will hit the target, only the closest
	 * bullet is returned. Returns null if no bullet will hit the target.
	 */
	protected BulletInfo getTargetingBullet(BulletInfo[] bullets) {
		RobotInfo player = new RobotInfo(-1, null, rc.getType(), rc.getLocation(), 1, 1, 1);

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
		RobotInfo player = new RobotInfo(-1, null, rc.getType(), playerLocation, 1, 1, 1);
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

		// Compute the shortest distance between the bullet and the center of
		// the target
		float angle = direction.radians;
		float dist = (float) Math.abs(Math.sin(angle) * (xTarget - xStart) - Math.cos(angle) * (yTarget - yStart));

		// If the shortest distance is too large, the bullet won't ever
		// intersect the target
		if (dist > targetRadius) {
			return NO_INTERSECT;
		}

		// Compute the distance the bullet travels to get to the point of
		// closest approach
		float lengthToClosestApproach = (float) Math
				.sqrt((xTarget - xStart) * (xTarget - xStart) + (yTarget - yStart) * (yTarget - yStart) - dist * dist);

		// Compute the distance the bullet travels from the intersection point
		// to the closest approach
		float excessDistance = (float) Math.sqrt(targetRadius * targetRadius - dist * dist);

		return lengthToClosestApproach - excessDistance;
	}

	public boolean closeToLocationAndNoEnemies(RobotController rc, MapLocation location) throws GameActionException {
		if (rc.getLocation().distanceTo(location) < (rc.getType().sensorRadius * .8)
				&& rc.senseNearbyRobots(-1, getEnemyTeam()).length == 0) {
			return true;
		}
		return false;
	}

	public boolean closeToLocationAndJustArchons(RobotController rc, MapLocation location) throws GameActionException {
		boolean enemy = false;
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, getEnemyTeam());
		for(RobotInfo info : nearbyRobots) {
			if(info.getType() != RobotType.ARCHON) {
				enemy = true;
				break;
			}
		}
		if (rc.getLocation().distanceTo(location) < (rc.getType().sensorRadius * .8)
				&& !enemy) {
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

		loop: for (int index = 0; index < enemies.length; index++) {

			double priority = 0;

			if (enemies[index].getType().canAttack()) {
				priority = enemies[index].getType().attackPower /
						(Math.max(enemies[index].health, 1) * rc.getLocation().distanceTo(enemies[index].getLocation()));
			}

			// TODO: Refactor
			if ((priority > maxPriority || (maxPriority == 0 && enemies[index].health < enemies[maxIndex].health))) {

				// Don't attack archons at the start of the game.
				if (enemies[index].type == RobotType.ARCHON && rc.getRoundNum() < ARCHON_IGNORE_ROUND) {
					continue loop;
				}

				Direction toEnemy = rc.getLocation().directionTo(enemies[index].location);
				float spawnOffset = rc.getType().bodyRadius + GameConstants.BULLET_SPAWN_OFFSET;

				MapLocation bulletSpawnPoint = rc.getLocation().add(toEnemy, spawnOffset);

				// Only attack if we will hit an enemy.
				if (getFirstHitTeam(bulletSpawnPoint, toEnemy, hitTrees, rc.getLocation().distanceTo(enemies[index].getLocation()))
				== getEnemyTeam()) {
					maxIndex = index;
					maxPriority = priority;
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

		loop: for (BodyInfo enemy : foes) {
			// Ignore enemy archons at the start of the game.
			if (enemy instanceof RobotInfo) {

				RobotInfo robot = (RobotInfo) enemy;
				if (robot.type == RobotType.ARCHON && rc.getRoundNum() < ARCHON_IGNORE_ROUND) {
					continue loop;
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
		if (rc.getTeamBullets() >= GameConstants.VICTORY_POINTS_TO_WIN * GameConstants.BULLET_EXCHANGE_RATE) {
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

	public void drawBullshitLine() {
		/*
		 * int[] color = getRandomColor(); MapLocation[] enemyLocs =
		 * rc.getInitialArchonLocations(getEnemyTeam()); MapLocation[] allyLocs
		 * = rc.getInitialArchonLocations(rc.getTeam()); MapLocation[] locs =
		 * new MapLocation[allyLocs.length + enemyLocs.length]; for(int i = 0; i
		 * < enemyLocs.length; i++) { locs[i] = enemyLocs[i]; } for(int i =
		 * enemyLocs.length; i < allyLocs.length + enemyLocs.length; i++) {
		 * locs[i] = allyLocs[i - enemyLocs.length]; } float minX =
		 * Float.MAX_VALUE; float maxX = Float.MIN_VALUE; float minY =
		 * Float.MAX_VALUE; float maxY = Float.MIN_VALUE; for(MapLocation loc :
		 * locs) { if(loc.x < minX) { minX = loc.x; } if(loc.y < minY) { minY =
		 * loc.y; } if(loc.x > maxX) { maxX = loc.x; } if(loc.y > maxY) { maxY =
		 * loc.y; } }
		 * 
		 * float x = minX + (float) (Math.random() * (maxX - minX)); float y =
		 * minY + (float) (Math.random() * (maxY - minY));
		 * 
		 * rc.setIndicatorLine(rc.getLocation(), new MapLocation(x, y),
		 * color[0], color[1], color[2]);
		 */
	}

	protected BulletInfo[] getAllIncomingBullets(BulletInfo[] bullets, MapLocation location, float angleTolerance) {
		ArrayList<BulletInfo> incoming = new ArrayList<>();
		for (BulletInfo bullet : bullets) {
			if (bullet.location.directionTo(location).degreesBetween(bullet.dir) < angleTolerance
					&& bullet.location.distanceTo(location) < 5) {
				incoming.add(bullet);
			}
		}

		return incoming.toArray(new BulletInfo[incoming.size()]);
	}

	public int[] getRandomColor() {
		return new int[] { (int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256) };
	}

	protected void moveAndDodge(MapLocation enemy, BulletInfo[] bullets) throws GameActionException {
		MapLocation currLocation = rc.getLocation();
		Direction toEnemy = currLocation.directionTo(enemy);

		float minDamage = rc.getHealth();
		int bestAngle = -40;
		for (int angle = -40; angle < 40; angle += 10) {
			MapLocation expectedLocation = currLocation.add(toEnemy.rotateLeftDegrees(angle), (float) getStrideRadius(rc.getType()));
			float damage = expectedDamage(bullets, expectedLocation);

			if (damage < minDamage) {
				bestAngle = angle;
				minDamage = damage;
			}
		}

		move(toEnemy.rotateLeftDegrees(bestAngle));
	}

	public static double getStrideRadius(RobotType rt) {
		switch (rt) {
			case ARCHON:
			case TANK:
			case SOLDIER: {
				return 1;
			}

			case GARDENER: {
				return 2;
			}

			case SCOUT: {
				return 2.5;
			}

			case LUMBERJACK: {
				return 1.5;
			}

			default: return 1;
		}
	}

	protected void dodge(BulletInfo[] bullets) throws GameActionException {
		// bullets = getAllIncomingBullets(bullets, rc.getLocation(), 20);
		BulletInfo[] predictNext = Arrays.stream(bullets).map(b -> new BulletInfo(b.getID(),
				b.location.add(b.getDir(), b.speed), b.getDir(), b.getSpeed(), b.getDamage()))
				.toArray(BulletInfo[]::new);
		Direction densestDirection = findDensestDirection(bullets);
		MapLocation currentLocation = rc.getLocation();

		float stationaryImminentDanger = getImminenetDanger(bullets, currentLocation);

		int safestAngle = 0;
		float leastDanger = stationaryImminentDanger;
		for (int angle = 90; angle < 270; angle += 10) {

			float expectedDanger = getImminenetDanger(predictNext,
					currentLocation.add(densestDirection.rotateLeftDegrees(angle)));

			if (expectedDanger < leastDanger && rc.canMove(densestDirection.rotateLeftDegrees(angle))) {
				leastDanger = expectedDanger;
				safestAngle = angle;
			}
		}

		Direction toMove = densestDirection.rotateLeftDegrees(safestAngle);

		if (rc.canMove(toMove)) {
			// rc.setIndicatorLine(currentLocation,
			// currentLocation.add(densestDirection, 3), 255, 0, 0);
			// rc.setIndicatorDot(currentLocation.add(densestDirection, 3), 0,
			// 0, 255);
			// rc.setIndicatorLine(currentLocation, currentLocation.add(toMove,
			// 3), 0, 255, 0);
			// rc.setIndicatorDot(currentLocation.add(toMove, 3), 0, 255, 0);
			move(toMove);
		}

	}

	private float getImminenetDanger(BulletInfo[] bullets, MapLocation loc) {
		float danger = 0;
		RobotInfo player = new RobotInfo(-1, null, rc.getType(), loc, 1, 1, 1);
		float totalDamage = 0;
		for (BulletInfo bullet : bullets) {
			danger += bullet.getDamage() / loc.distanceTo(bullet.getLocation());
			if (willHit(bullet, player)) {
				totalDamage += bullet.damage;
			}
		}

		return danger * totalDamage;
	}

	/**
	 * This method returns the expected damage which a specific location will
	 * get in the next round, based on the current bullet information
	 * 
	 * @param bullets
	 * @return
	 */
	protected float expectedDamage(BulletInfo[] bullets, MapLocation loc) {
		RobotInfo player = new RobotInfo(-1, null, rc.getType(), loc, 1, 1, 1);
		float totalDamage = 0;
		for (BulletInfo bullet : bullets) {
			if (willHit(bullet, player)) {
				totalDamage += bullet.damage;
			}
		}

		return totalDamage;
	}

	protected void dodge(BulletInfo toDodge) throws GameActionException {
		Direction toBullet = rc.getLocation().directionTo(toDodge.location);
		Direction toMove;
		for (int angle = 90; angle <= 180; angle += 10) {
			toMove = toBullet.rotateLeftDegrees(angle);
			if (rc.canMove(toMove)) {
				move(toMove);
				return;
			}
			toMove = toBullet.rotateRightDegrees(angle);
			if (rc.canMove(toMove)) {
				move(toMove);
				return;
			}
		}
	}

}
