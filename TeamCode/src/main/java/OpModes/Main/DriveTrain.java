package OpModes.Main;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name = "Mecanum Drive Simple", group = "Main")
public class DriveTrain extends OpMode {

    DcMotor frontLeftDrive;
    DcMotor frontRightDrive;
    DcMotor backLeftDrive;
    DcMotor backRightDrive;
    private Limelight3A limelight;
    private Servo turretServo;

    // --- Tunable constants ---
    private static final double TOLERANCE = 15.6;
    private static final double ADJUST_SPEED = 0.007;
    private static final double SEARCH_SPEED = 0.0007;
    private static final double SERVO_MIN = 0.0;
    private static final double SERVO_MAX = 1.0;

    private double servoPos = 0.5;
    private double lastTx = 0;
    private boolean searchRight = true;
    private boolean aligned = false;  // ✅ new flag

    @Override
    public void init() {
        frontLeftDrive = hardwareMap.get(DcMotor.class, "leftfrontmotor");
        frontRightDrive = hardwareMap.get(DcMotor.class, "rightfrontmotor");
        backLeftDrive = hardwareMap.get(DcMotor.class, "leftbackmotor");
        backRightDrive = hardwareMap.get(DcMotor.class, "rightbackmotor");

        // Correct motor directions for standard mecanum
        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.REVERSE);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);
    }

    @Override
    public void loop() {
        // Left stick = translation, right stick = rotation
        double forward = -gamepad1.left_stick_y;   // forward/back
        double right = gamepad1.left_stick_x;      // strafe
        double rotate = gamepad1.right_stick_x;    // turn

        // Optional strafe correction
        right *= 1.1;

        // Canonical mecanum wheel math
        double frontLeftPower = forward + right + rotate;
        double frontRightPower = forward - right - rotate;
        double backLeftPower = forward - right + rotate;
        double backRightPower = forward + right - rotate;

        // Normalize powers
        double max = Math.max(
                Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                Math.max(Math.abs(backLeftPower), Math.abs(backRightPower))
        );
        if (max > 1.0) {
            frontLeftPower /= max;
            frontRightPower /= max;
            backLeftPower /= max;
            backRightPower /= max;
        }

        // Apply power directly
        frontLeftDrive.setPower(frontLeftPower);
        frontRightDrive.setPower(frontRightPower);
        backLeftDrive.setPower(backLeftPower);
        backRightDrive.setPower(backRightPower);

        // Debug telemetry
        telemetry.addData("FL", frontLeftPower);
        telemetry.addData("FR", frontRightPower);
        telemetry.addData("BL", backLeftPower);
        telemetry.addData("BR", backRightPower);
        telemetry.update();
    }
}
