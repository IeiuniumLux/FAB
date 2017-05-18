package me.abencomo.fab;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.MAVLink.common.msg_command_long;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.List;


public class FABActivity extends AppCompatActivity implements DroneListener, TowerListener, LinkListener {

    private static final String TAG = FABActivity.class.getSimpleName();

    public static int PX4_CUSTOM_MAIN_MODE_MANUAL = 1;
    public static int PX4_CUSTOM_MAIN_MODE_ALTCTL = 2;
    public static int PX4_CUSTOM_MAIN_MODE_POSCTL = 3;
    public static int PX4_CUSTOM_MAIN_MODE_STABILIZED = 7;

    private Drone mDrone;
    private ControlTower mControlTower;

    private final Handler handler = new Handler();

    private OffboardPositionUpdater mOffboard = null;
    private Button mTakeoffButton;

    private boolean hasFlown = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fab);

        mTakeoffButton = (Button) findViewById(R.id.buttonTakeoff);

        final Context context = getApplicationContext();
        mControlTower = new ControlTower(context);
        mDrone = new Drone(context);
        mOffboard = new OffboardPositionUpdater(mDrone);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mControlTower.connect(this);
        updateVehicleModesForType(Type.TYPE_COPTER);
    }

    @Override
    protected void onResume() {
        super.onResume();

        toggleHideyBar();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDrone.isConnected()) {
            mDrone.unregisterDroneListener(this);
            mDrone.disconnect();
            updateTakeoffButton(false);
        }
        mControlTower.unregisterDrone(mDrone);
        mControlTower.disconnect();
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:

                change_flight_mode(PX4_CUSTOM_MAIN_MODE_ALTCTL);

                if (!mOffboard.isRunning()) {
                    mOffboard.start(); // Start streaming setpoints before entering offboard mode
                }
                arm(true);
                try {
                    Thread.sleep(7000);
                } catch (InterruptedException e) {

                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mOffboard.land();
                        mTakeoffButton.setText("Landing");
                    }
                }, 20000);
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                mOffboard.stop();
                updateTakeoffButton(mDrone.isConnected());
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                State vehicleState = mDrone.getAttribute(AttributeType.STATE);
                if (vehicleState.isArmed()) {
                    if (!hasFlown) {
                        mOffboard.takeoff();
                        toggle_offboard_control(1.0f);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mOffboard.position();
                            }
                        }, 1800);
                        hasFlown = true;
                        mTakeoffButton.setText("Flying");
                    }
                } else if (vehicleState.isConnected()) {
                    if (hasFlown) {
                        toggle_offboard_control(0.0f);
                        change_flight_mode(PX4_CUSTOM_MAIN_MODE_MANUAL);
                        arm(false);
                        mTakeoffButton.performClick();
                        hasFlown = false;
                    }
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                State vehicleStateMode = mDrone.getAttribute(AttributeType.STATE);
                VehicleMode vehicleMode = vehicleStateMode.getVehicleMode();
                Log.i("VEHICLE_MODE - ", vehicleMode.getLabel());
                break;

            case AttributeEvent.SPEED_UPDATED:
                Speed droneSpeed = mDrone.getAttribute(AttributeType.SPEED);
                Log.i("SPEED - ", String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                Altitude droneAltitude = mDrone.getAttribute(AttributeType.ALTITUDE);
                Log.i("ALTITUDE - ", String.format("%3.1f", droneAltitude.getAltitude()) + "m");
                break;

            default:
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch (connectionStatus.getStatusCode()) {
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                Log.e(TAG, ">>> Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onTowerConnected() {
        mControlTower.registerDrone(mDrone, handler);
        mDrone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        Log.e(TAG, "<!< DroneKit-Android Interrupted >!>");
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    public void onButtonTakeoffPressed(View view) {
        if (mDrone.isConnected()) {
            mDrone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            mDrone.connect(connectionParams);
            mTakeoffButton.setText("Arming");
        }
    }

    protected void toggle_offboard_control(float flag) {
        msg_command_long msg = new msg_command_long();
        msg.command = MAV_CMD.MAV_CMD_NAV_GUIDED_ENABLE;
        msg.confirmation = (short) 1;
        msg.param1 = flag; // flag > 0.5 => start, < 0.5 => stop
        MavlinkMessageWrapper msgWrapper = new MavlinkMessageWrapper(msg);
        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(msgWrapper);
    }

    protected void change_flight_mode(int mode) {
        msg_command_long msg = new msg_command_long();
        msg.command = MAV_CMD.MAV_CMD_DO_SET_MODE;
        // The first parameter is a bitmask
        msg.param1 = MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED | MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED;
        msg.param2 = mode;
        msg.param3 = 0;
        MavlinkMessageWrapper msgWrapper = new MavlinkMessageWrapper(msg);
        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(msgWrapper);
    }

    protected void arm(boolean arm) {
        Log.e("Drone - ", (arm) ? "Arming" : "Disarming");
        VehicleApi.getApi(mDrone).arm(arm, false, new SimpleCommandListener());
    }

    // UI updating
    // ==========================================================
    protected void updateTakeoffButton(Boolean isConnected) {
        if (isConnected) {
            mTakeoffButton.setText("Land");
        } else {
            mTakeoffButton.setText("Takeoff");
        }
    }

    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    public void toggleHideyBar() {
        // The SYSTEM_UI_FLAG_IMMERSIVE_STICKY flag doesn't trigger any listeners since the system
        // bars are in a transient state and they will automatically hide after a few moments.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}