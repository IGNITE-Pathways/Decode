package OpModes.Main;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;


import Constants.Shooter;
@TeleOp(name = "Horizontal Distance (ft)", group = "Main")
public class DistanceFinder extends LinearOpMode {
    Shooter constants = new Shooter();
    private Limelight3A limelight;

    // Real robot + field measurements
    private static final double CAMERA_HEIGHT_METERS = Shooter.CAMERA_HEIGHT_METERS;        // camera lens at floor level
    private static final double TARGET_HEIGHT_METERS = Shooter.TARGET_HEIGHT_METERS;      // 30 inches = 0.762 m
    private static final double CAMERA_MOUNT_ANGLE_DEGREES = Shooter.CAMERA_MOUNT_ANGLE_DEGREES; // camera tilt angle

    @Override
    public void runOpMode() {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        limelight.start();

        telemetry.addLine("Robot Ready. Press Play.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            LLResult result = limelight.getLatestResult();

            if (result != null && result.isValid()) {
                double verticalOffsetDeg = result.getTy(); // vertical angle offset to target
                double totalAngleDeg = CAMERA_MOUNT_ANGLE_DEGREES + verticalOffsetDeg;
                double totalAngleRad = Math.toRadians(totalAngleDeg);

                // Distance along the floor (meters)
                double distanceMeters = (TARGET_HEIGHT_METERS - CAMERA_HEIGHT_METERS) / Math.tan(totalAngleRad);

                // Convert to feet
                double distanceFeet = distanceMeters * 3.28084;

                telemetry.addData("Distance to Target (ft)", "%.2f", distanceFeet);
                telemetry.addData("Vertical Offset (deg)", "%.2f", verticalOffsetDeg);
                telemetry.addData("Total Angle (deg)", "%.2f", totalAngleDeg);
            } else {
                telemetry.addLine("No valid target detected");
            }

            telemetry.update();
        }

        limelight.stop();
    }
}
