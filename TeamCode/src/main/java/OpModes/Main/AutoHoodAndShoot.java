package OpModes.Main;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;
import ProgrammingBoard.ProgrammingBoardShooter;
import Constants.Shooter;

@TeleOp(name = "Auto Hood and Shoot", group = "Main")
public class AutoHoodAndShoot extends LinearOpMode {

    private Limelight3A limelight;
    private Servo hoodServo;
    private ProgrammingBoardShooter board = new ProgrammingBoardShooter();

    private double servoPosition = 0.0;
    private static final double SERVO_STEP_PER_FOOT = 0.1; // servo per foot
    private double flywheelPower = 0.0;

    @Override
    public void runOpMode() {
        // Init hardware
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        hoodServo = hardwareMap.get(Servo.class, "hoodservo");
        board.initializeComponents(hardwareMap);
        limelight.start();

        telemetry.addLine("Auto Hood and Shoot Ready. Press Play.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            LLResult result = limelight.getLatestResult();
            double distanceFeet = -1;

            if (result != null && result.isValid()) {
                double verticalOffsetDeg = result.getTy();
                double totalAngleDeg = Shooter.CAMERA_MOUNT_ANGLE_DEGREES + verticalOffsetDeg;
                double totalAngleRad = Math.toRadians(totalAngleDeg);

                double distanceMeters = (Shooter.TARGET_HEIGHT_METERS - Shooter.CAMERA_HEIGHT_METERS)
                        / Math.tan(totalAngleRad);
                distanceFeet = distanceMeters * 3.28084;

                telemetry.addData("Distance (ft)", "%.2f", distanceFeet);
                telemetry.addData("Vertical Offset (deg)", "%.2f", verticalOffsetDeg);
                telemetry.addData("Total Angle (deg)", "%.2f", totalAngleDeg);
            } else {
                telemetry.addLine("No valid target detected");
            }

            // LEFT TRIGGER → Align hood
            if (gamepad1.left_trigger > 0.2 && distanceFeet > 0) {
                servoPosition = SERVO_STEP_PER_FOOT * distanceFeet;
                servoPosition = Math.max(0.0, Math.min(1.0, servoPosition));
                hoodServo.setPosition(servoPosition);
                telemetry.addData("Hood Aligned", "Set to %.2f based on %.2f ft", servoPosition, distanceFeet);
            }

            // RIGHT TRIGGER → Adjust flywheel power based on distance
            if (gamepad1.right_trigger > 0.2 && distanceFeet > 0) {
                flywheelPower = Math.min(1.0, 0.1 * distanceFeet);
                board.flyWheelMotor.setPower(flywheelPower);
                telemetry.addData("Shooter Active", "Power: %.2f (%.2f ft)", flywheelPower, distanceFeet);
            } else if (gamepad1.right_trigger < 0.2 && gamepad1.square == false) {
                board.flyWheelMotor.setPower(0);
            }

            // SQUARE → Do both automatically (hood + shooter)
            if (gamepad1.square && distanceFeet > 0) {
                servoPosition = SERVO_STEP_PER_FOOT * distanceFeet;
                servoPosition = Math.max(0.0, Math.min(1.0, servoPosition));
                hoodServo.setPosition(servoPosition);

                flywheelPower = Math.min(1.0, 0.1 * distanceFeet);
                board.flyWheelMotor.setPower(flywheelPower);

                telemetry.addData("Auto Mode", "Hood: %.2f | Power: %.2f | Distance: %.2f ft",
                        servoPosition, flywheelPower, distanceFeet);
                sleep(200); // debounce
            }

            telemetry.addData("Current Servo Pos", "%.2f", servoPosition);
            telemetry.addData("Current Shooter Power", "%.2f", flywheelPower);
            telemetry.update();
        }

        limelight.stop();
        board.flyWheelMotor.setPower(0);
    }
}
