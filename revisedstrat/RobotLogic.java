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

}
