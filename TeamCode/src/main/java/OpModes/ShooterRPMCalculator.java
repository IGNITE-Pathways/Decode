package OpModes;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Sensor: Limelight3A Distance + Position", group = "Sensor")
public class ShooterRPMCalculator extends LinearOpMode {

    private Limelight3A limelight;

    // Camera and target configuration
    private static final double CAMERA_HEIGHT_METERS = 0.20; // Camera height
    private static final double TARGET_HEIGHT_METERS = 2.50; // Target height
    private static final double CAMERA_MOUNT_ANGLE_DEGREES = 25.0; // Camera tilt

    @Override
    public void runOpMode() throws InterruptedException {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        limelight.start(); // Start polling

        telemetry.addLine("Robot Ready. Press Play.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            LLResult result = limelight.getLatestResult();

            if (result != null && result.isValid()) {
                // Vertical and horizontal offsets from Limelight
                double verticalOffsetDeg = result.getTy();
                double horizontalOffsetDeg = result.getTx();

                // Total angle to target
                double totalAngleDeg = CAMERA_MOUNT_ANGLE_DEGREES + verticalOffsetDeg;
                double totalAngleRad = Math.toRadians(totalAngleDeg);
                double horizontalAngleRad = Math.toRadians(horizontalOffsetDeg);

                // Distance straight to target (along ground)
                double distanceMeters = (TARGET_HEIGHT_METERS - CAMERA_HEIGHT_METERS) / Math.tan(totalAngleRad);

                // Robot X/Y position relative to target
                double x = distanceMeters * Math.cos(horizontalAngleRad); // Forward distance
                double y = distanceMeters * Math.sin(horizontalAngleRad); // Side distance

                // Telemetry output
                telemetry.addData("Distance (m)", "%.2f", distanceMeters);
                telemetry.addData("Robot X (m)", "%.2f", x);
                telemetry.addData("Robot Y (m)", "%.2f", y);
                telemetry.addData("Vertical Offset (deg)", "%.2f", verticalOffsetDeg);
                telemetry.addData("Horizontal Offset (deg)", "%.2f", horizontalOffsetDeg);

            } else {
                telemetry.addLine("No valid target detected");
            }

            telemetry.update();
        }

        limelight.stop();
    }
}
