package me.abencomo.fab;

import android.util.Log;

import com.MAVLink.common.msg_set_position_target_local_ned;
import com.MAVLink.enums.MAV_FRAME;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;


public class OffboardPositionUpdater implements Runnable {

    private static final String TAG = OffboardPositionUpdater.class.getSimpleName();

    /**
     * Defines for mavlink_set_position_target_local_ned_t.type_mask
     *
     * Bitmask to indicate which dimensions should be ignored by the vehicle
     *
     * a value of 0b0000000000000000 or 0b0000001000000000 indicates that none of
     * the setpoint dimensions should be ignored.
     *
     * If bit 10 is set, then afx afy afz should be interpreted as force instead of acceleration.
     *
     * Mapping:
     * bit 1: x,
     * bit 2: y,
     * bit 3: z,
     * bit 4: vx,
     * bit 5: vy,
     * bit 6: vz,
     * bit 7: afx,
     * bit 8: afy,
     * bit 9: afz,
     * bit 10: is force setpoint,
     * bit 11: yaw,
     * bit 12: yaw rate,
     * bit 13: is takeoff setpoint,
     * bit 14: is land setpoint,
     * bit 15: is loiter setpoint,
     * bit 16: is idle setpoint
     *
     */
    public static final short POSITION  = 0b0000110111111000; //  3576
    private static final short TAKEOFF  = 0b0001110111111000; //  7672
    private static final short LAND     = 0b0010110111111000; // 11768


    private static final double toRad   = Math.PI / 180.0;
    private static final double fromRad = 180.0 / Math.PI ;

    private Drone drone;
    private boolean isRunning = false;

    // PX4 flight stack operates in the aerospace NED coordinate frame, therefore z is set to a negative number...
    private float z_pos = -1.0f;
    private float x_pos = 0f;
    private float y_pos = 0f;

    private float yaw = 0f;

    private short type_mask = POSITION;

    private int duration_ms = 0;
    private long start_time_ms = 0;

    public OffboardPositionUpdater(Drone drone) {
        this.drone = drone;
    }

    public void start() {
        start(0);
    }

    public void start(int duration_ms) {
        isRunning = true;
        type_mask = POSITION;
        Thread t = new Thread(this);
        t.setName("Offboard worker");
        t.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) { }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        isRunning = false;
        type_mask = POSITION;
    }

    public void setPosition(float x, float y, float z) {
        this.y_pos = y;
        this.x_pos = x;
        this.z_pos = z;
        System.out.printf("Offboard Set: X: %2.1f Y: %2.1f Z: %2.1f\n",x_pos, y_pos,z_pos);
    }

    public void setYaw(float yaw_deg) {
        this.yaw = yaw_deg;
        System.out.printf("Offboard Set: YAW: %2f°\n",yaw);
    }

    @Override
    public void run() {

        if(!isRunning)
            return;

        start_time_ms = System.currentTimeMillis();

        while(isRunning) {

            if(duration_ms > 0 && (System.currentTimeMillis() - start_time_ms) > duration_ms) {
                isRunning = false;
            }

            msg_set_position_target_local_ned msg = new msg_set_position_target_local_ned();
            msg.type_mask = this.type_mask;
            msg.x =  x_pos;
            msg.y =  y_pos;
            msg.z =  z_pos;
            msg.yaw = (float)(yaw * toRad);
            msg.coordinate_frame = MAV_FRAME.MAV_FRAME_LOCAL_NED;
            MavlinkMessageWrapper msgWrapper2 = new MavlinkMessageWrapper(msg);

            ExperimentalApi.getApi(drone).sendMavlinkMessage(msgWrapper2);

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) { }
        }
        Log.e(TAG, "Offboard updater stopped ("+(System.currentTimeMillis()-start_time_ms)+"ms)");
    }

    public void land() {
        this.type_mask = LAND;
        Log.e(TAG, "vvv Landing vvv");
    }

    public void takeoff() {
        this.type_mask = TAKEOFF;
    }

    public void position() {
        this.type_mask = POSITION;
    }
}