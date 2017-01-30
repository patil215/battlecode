package revisedstrat;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Arrays;

/**
 * Created by patil215 on 1/12/17.
 */

// TODO: Make a remove recent broadcast function
public class BroadcastManager {

	/*
	 * Represents a location. Locations stored: ENEMY: Location of a found enemy
	 * on the map ARCHON_HELP: Location of an archon if the archon sends a help
	 * request (i.e. this location is nonzero) GARDENER_HELP: Location of a
	 * gardener if the gardener sends a help request (i.e. this location is
	 * nonzero)
	 */
	public enum LocationInfoType {
		ENEMY(0, 1, 1), ENEMY_NOT_ARCHON(2, 3, 3), ARCHON_HELP(21, 22, 22), GARDENER_HELP(25, 26, 26), LUMBERJACK_GET_HELP(31,32,32);

		// Represents the index that stores the pointer (i.e. index) of the most
		// recently added/modified location
		int locationPointerIndex;
		// Represents the index start of the "block" that stores locations
		// (inclusive)
		int locationStartIndex;
		// Represents the index end of the "block" that stores locations
		// (inclusive)
		int locationEndIndex;

		LocationInfoType(int locationPointerIndex, int locationStartIndex, int locationEndIndex) {
			this.locationPointerIndex = locationPointerIndex;
			this.locationStartIndex = locationStartIndex;
			this.locationEndIndex = locationEndIndex;
		}

	}

	// Channel for writing spam in the case of desiring to send fake broadcasts
	private static int SPAM_BROADCAST_POINTER = 76;

	// Channel to determine if the locationPointerIndex values have been set to
	// defaults.
	private static int SETUP_INDEX = 77;

	private static int NUM_LUMBERJACK_INITIAL_INDEX = 100;

	/*
	 * Represents the count of a specific unit. All unit types can be written
	 * directly except for ALLY_ROBOT_TOTAL and ALLY_ENEMY_TOTAL - these are
	 * updated automatically.
	 */
	public enum UnitCountInfoType {
		ALLY_ARCHON(77), ALLY_GARDENER(78), ALLY_SOLDIER(79), ALLY_TANK(80), ALLY_SCOUT(81), ALLY_LUMBERJACK(
				82), ALLY_ROBOT_TOTAL(83), ALLY_TREE(84),

		ENEMY_ARCHON(85), ENEMY_GARDENER(86), ENEMY_SOLDIER(87), ENEMY_TANK(88), ENEMY_SCOUT(89), ENEMY_LUMBERJACK(
				90), ENEMY_ROBOT_TOTAL(91), ENEMY_TREE(92),

		NEUTRAL_TREE(93);

		int unitCountIndex;

		UnitCountInfoType(int unitCountIndex) {
			this.unitCountIndex = unitCountIndex;
		}
	}

	/*
	 * Sets up the initial values for the locationPointerIndex values
	 * Returns true if this is the first time this method has been called.
	 */
	public static boolean initializeLocationPointerIndexValues(RobotController rc) throws GameActionException{
		if(rc.readBroadcast(SETUP_INDEX)==0){
			rc.broadcast(SETUP_INDEX, 1);
			for(LocationInfoType type : LocationInfoType.values()) {
				rc.broadcast(type.locationPointerIndex, (type.locationStartIndex));
			}
			return true;
		}
		return false;
	}

	public static void writeLumberjackInitialCount(RobotController rc, int numLumberjacks) throws GameActionException {
		rc.broadcast(NUM_LUMBERJACK_INITIAL_INDEX, numLumberjacks);
	}

	public static int getLumberjackInitialCount(RobotController rc) throws GameActionException {
		return rc.readBroadcast(NUM_LUMBERJACK_INITIAL_INDEX);
	}

	public static void invalidateLocation(RobotController rc, LocationInfoType type) throws GameActionException {
		int currentEnemyLocPointer = rc.readBroadcast(type.locationPointerIndex);
		rc.broadcast(currentEnemyLocPointer, 0);
	}

	public static void broadcastSpam(RobotController rc) throws GameActionException {
		rc.broadcast(SPAM_BROADCAST_POINTER, -1);
	}

	public static void saveLocation(RobotController rc, MapLocation location, LocationInfoType type)
			throws GameActionException {
		int currentEnemyLocPointer = rc.readBroadcast(type.locationPointerIndex);
		currentEnemyLocPointer++;
		if (currentEnemyLocPointer > type.locationEndIndex) {
			currentEnemyLocPointer -= (type.locationEndIndex - type.locationStartIndex + 1);
		}

		try {
			int x = (int) location.x;
			int y = (int) location.y;
			rc.broadcast(currentEnemyLocPointer, zipValues(x, y));
			rc.broadcast(type.locationPointerIndex, currentEnemyLocPointer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static MapLocation getRecentLocation(RobotController rc, LocationInfoType type) throws GameActionException {
		int index = rc.readBroadcast(type.locationPointerIndex);
		int val = rc.readBroadcast(index);
		if (val == 0) {
			return null;
		}
		int[] loc = unzipValues(val, 2);
		return new MapLocation(loc[0], loc[1]);
	}

	private static void checkValidUnitType(UnitCountInfoType type) throws Exception {
		if (type == UnitCountInfoType.ALLY_ROBOT_TOTAL || type == UnitCountInfoType.ENEMY_ROBOT_TOTAL) {
			throw new Exception();
		}
	}

	private static boolean isAllyRobot(UnitCountInfoType type) {
		if (type == UnitCountInfoType.ALLY_ROBOT_TOTAL || type == UnitCountInfoType.ALLY_ARCHON
				|| type == UnitCountInfoType.ALLY_GARDENER || type == UnitCountInfoType.ALLY_LUMBERJACK
				|| type == UnitCountInfoType.ALLY_SCOUT || type == UnitCountInfoType.ALLY_SOLDIER
				|| type == UnitCountInfoType.ALLY_TANK) {
			return true;
		}
		return false;
	}

	private static boolean isEnemyRobot(UnitCountInfoType type) {
		if (type == UnitCountInfoType.ENEMY_ROBOT_TOTAL || type == UnitCountInfoType.ENEMY_ARCHON
				|| type == UnitCountInfoType.ENEMY_GARDENER || type == UnitCountInfoType.ENEMY_LUMBERJACK
				|| type == UnitCountInfoType.ENEMY_SCOUT || type == UnitCountInfoType.ENEMY_SOLDIER
				|| type == UnitCountInfoType.ENEMY_TANK) {
			return true;
		}
		return false;
	}

	public static int getUnitCount(RobotController rc, UnitCountInfoType type) throws GameActionException {
		return rc.readBroadcast(type.unitCountIndex);
	}

	public static void setUnitCount(RobotController rc, UnitCountInfoType type, int count) throws Exception {
		checkValidUnitType(type);
		int old = rc.readBroadcast(type.unitCountIndex);
		int difference = count - old;
		rc.broadcast(type.unitCountIndex, count);

		if (isAllyRobot(type)) {
			int oldAllyCount = rc.readBroadcast(UnitCountInfoType.ALLY_ROBOT_TOTAL.unitCountIndex);
			oldAllyCount += difference;
			rc.broadcast(UnitCountInfoType.ALLY_ROBOT_TOTAL.unitCountIndex, oldAllyCount);
		}
		if (isEnemyRobot(type)) {
			int oldEnemyCount = rc.readBroadcast(UnitCountInfoType.ENEMY_ROBOT_TOTAL.unitCountIndex);
			oldEnemyCount += difference;
			rc.broadcast(UnitCountInfoType.ENEMY_ROBOT_TOTAL.unitCountIndex, oldEnemyCount);
		}
	}

	public static void incrementUnitCount(RobotController rc, UnitCountInfoType type) throws Exception {
		checkValidUnitType(type);
		int current = rc.readBroadcast(type.unitCountIndex);
		current++;
		rc.broadcast(type.unitCountIndex, current);
		if (isAllyRobot(type)) {
			int allyCount = rc.readBroadcast(UnitCountInfoType.ALLY_ROBOT_TOTAL.unitCountIndex);
			allyCount++;
			rc.broadcast(UnitCountInfoType.ALLY_ROBOT_TOTAL.unitCountIndex, allyCount);
		}
		if (isEnemyRobot(type)) {
			int enemyCount = rc.readBroadcast(UnitCountInfoType.ENEMY_ROBOT_TOTAL.unitCountIndex);
			enemyCount++;
			rc.broadcast(UnitCountInfoType.ENEMY_ROBOT_TOTAL.unitCountIndex, enemyCount);
		}
	}

	public static void decrementUnitCount(RobotController rc, UnitCountInfoType type) throws Exception {
		checkValidUnitType(type);
		int current = rc.readBroadcast(type.unitCountIndex);
		current--;
		rc.broadcast(type.unitCountIndex, current);
		if (isAllyRobot(type)) {
			int allyCount = rc.readBroadcast(UnitCountInfoType.ALLY_ROBOT_TOTAL.unitCountIndex);
			allyCount--;
			rc.broadcast(UnitCountInfoType.ALLY_ROBOT_TOTAL.unitCountIndex, allyCount);
		}
		if (isEnemyRobot(type)) {
			int enemyCount = rc.readBroadcast(UnitCountInfoType.ENEMY_ROBOT_TOTAL.unitCountIndex);
			enemyCount--;
			rc.broadcast(UnitCountInfoType.ENEMY_ROBOT_TOTAL.unitCountIndex, enemyCount);
		}
	}

	/*
	 * Zips an array of values into a single value for writing to a channel.
	 * Uses Cantor's recursive pairing function.
	 */
	private static int zipValues(int... values) throws Exception {
		if (values.length == 1) {
			return values[0];
		}

		int[] remainder = Arrays.copyOfRange(values, 1, values.length);

		int result = zipValues(remainder);

		int res = (int) (((0.5) * (values[0] + result) * (values[0] + result + 1)) + result + 1);
		if (res == Integer.MAX_VALUE) {
			throw new Exception("Trying to zip too large of values!");
		}
		return res;
	}

	/*
	 * Unzips an array of values from a given channel value. Uses the inverse of
	 * Cantor's recursive pairing function.
	 *
	 * @param numValuesToUnzip: the number of values that are expected to be
	 * unzipped.
	 */
	private static int[] unzipValues(int k, int numValuesToUnzip) {

		if (numValuesToUnzip == 1) {
			return new int[] { k };
		}

		k--;

		int w = (int) Math.floor((Math.sqrt(8 * k + 1) - 1) / 2);
		int t = ((int) Math.pow(w, 2) + w) / 2;
		int y = k - t;
		int x = w - y;

		int[] res = unzipValues(y, --numValuesToUnzip);

		int[] resp = new int[res.length + 1];
		resp[0] = x;
		for (int i = 1; i < resp.length; i++) {
			resp[i] = res[i - 1];
		}

		return resp;
	}

}
