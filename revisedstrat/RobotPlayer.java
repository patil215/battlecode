package revisedstrat;

import battlecode.common.RobotController;

/**
 * Created by patil215 on 1/12/17.
 */
public strictfp class RobotPlayer {

    public static void run(RobotController rc) {

        RobotLogic logic;

        switch(rc.getType()) {
            case ARCHON:
                logic = new ArchonLogic(rc);
                break;
            case GARDENER:
                logic = new GardenerLogic(rc);
                break;
            case LUMBERJACK:
                logic = new LumberjackLogic(rc);
                break;
            case SCOUT:
                logic = new ScoutLogic(rc);
                break;
            case TANK:
                logic = new TankLogic(rc);
                break;
            case SOLDIER:
            	logic = new SoldierLogic(rc);
            	break;
            default:
            	logic = new ScoutLogic(rc);
       }

        logic.run();
    }

}
