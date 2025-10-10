package OpModes.InProgress;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

import ProgrammingBoard.ProgrammingBoardShooter;

@TeleOp(name = "main", group = "Main")
public class MainOpMode extends OpMode {

    DcMotor frontLeftDrive;
    DcMotor frontRightDrive;
    DcMotor backLeftDrive;
    DcMotor backRightDrive;
    private Limelight3A limelight;
    private Servo turretServo;

    // --- Tunable constants ---
    private static final double TOLERANCE = 15.6;
    private static final double ADJUST_SPEED = 0.008;
    private static final double SEARCH_SPEED = 0.0013;
    private static final double SERVO_MIN = 0.0;
    private static final double SERVO_MAX = 1.0;

    private double servoPos = 0.5;
    private double lastTx = 0;
    private boolean searchRight = true;
    private boolean aligned = false;  // ✅ new flag
    private Servo hoodServo;
    private ProgrammingBoardShooter board = new ProgrammingBoardShooter();

    // --- State ---
    private double servoPosition = 0.0;
    private double flywheelPower = 0.0;
    private boolean hoodActive = true;
    private boolean shooterActive = true;
    private boolean bothActive = true;

    // --- Constants ---
    private static final double SERVO_STEP_PER_FOOT = 0.13;
    private static final double APRILTAG_REAL_HEIGHT_METERS = 0.2032;; // 8 inches before now i made it 30
    private static final double CAMERA_VERTICAL_FOV_DEGREES = 49.5;   // Limelight 3A vertical FOV
    private static final int IMAGE_WIDTH_PIXELS = 1280;
    private static final int IMAGE_HEIGHT_PIXELS = 720;
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
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        turretServo = hardwareMap.get(Servo.class, "turretServo");

        if (limelight != null) {
            limelight.setPollRateHz(100);
            limelight.start();
        }

        turretServo.setPosition(servoPos);
        telemetry.addLine("Turret Servo Auto Align Initialized");
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        hoodServo = hardwareMap.get(Servo.class, "hoodservo");
        board.initializeComponents(hardwareMap);
        limelight.start();

        telemetry.addLine("Auto Hood + Shoot Ready");
        telemetry.update();
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


        if (limelight == null || !limelight.isConnected()) {
            telemetry.addLine("Limelight not connected");
            telemetry.update();
            return;
        }

        LLResult result = limelight.getLatestResult();

        if (result != null && result.isValid()) {
            double tx = result.getTx();

            // ✅ Stop if within tolerance
            if (Math.abs(tx) <= TOLERANCE) {
                aligned = true;
                telemetry.addData("tx", tx);
                telemetry.addLine("Aligned! Holding position.");
            } else {
                aligned = false;

                // Determine whether direction is helping
                boolean movingCorrect = Math.abs(tx) < Math.abs(lastTx);

                if (movingCorrect) {
                    servoPos += (searchRight ? ADJUST_SPEED : -ADJUST_SPEED);
                } else {
                    searchRight = !searchRight;
                    servoPos += (searchRight ? ADJUST_SPEED : -ADJUST_SPEED);
                }

                // Clamp servo position
                servoPos = Math.max(SERVO_MIN, Math.min(SERVO_MAX, servoPos));
                turretServo.setPosition(servoPos);

                telemetry.addData("tx", tx);
                telemetry.addData("lastTx", lastTx);
                telemetry.addData("Servo Pos", servoPos);
                telemetry.addData("Direction", searchRight ? "Right" : "Left");
                telemetry.addLine("Tag detected — aligning...");
            }

            lastTx = tx;
        } else {
            aligned = false;

            // Scanning mode when no tag detected
            if (searchRight) servoPos += SEARCH_SPEED;
            else servoPos -= SEARCH_SPEED;

            if (servoPos >= SERVO_MAX) searchRight = false;
            if (servoPos <= SERVO_MIN) searchRight = true;

            servoPos = Math.max(SERVO_MIN, Math.min(SERVO_MAX, servoPos));
            turretServo.setPosition(servoPos);
            telemetry.addLine("No AprilTag detected — scanning...");
        }

        telemetry.addData("Aligned", aligned);
        telemetry.update();
        double distanceFeet = -1;

        // --- Distance estimation via TA (AprilTag area) ---
        if (result != null && result.isValid()) {
            double taPercent = result.getTa();
            if (taPercent > 0.0) {
                double pixelArea = (taPercent / 100.0) * (IMAGE_WIDTH_PIXELS * IMAGE_HEIGHT_PIXELS);
                double tagPixelHeight = Math.sqrt(pixelArea);
                double focalPx = (IMAGE_HEIGHT_PIXELS / 2.0)
                        / Math.tan(Math.toRadians(CAMERA_VERTICAL_FOV_DEGREES / 2.0));

                double distanceMeters = (APRILTAG_REAL_HEIGHT_METERS * focalPx) / tagPixelHeight;
                distanceFeet = distanceMeters * 3.28084;

                telemetry.addData("Distance (ft) [TA]", "%.2f", distanceFeet);
            } else {
                telemetry.addLine("No valid target area data");
            }
        } else {
            telemetry.addLine("No valid target detected");
        }

        // --- X / Square button → both hood + shooter active ---
        if (gamepad1.square && distanceFeet > 0) {
            bothActive = true;
            hoodActive = true;
            shooterActive = true;
        }

        // --- Left Trigger → hood active ---
        if (gamepad1.left_trigger > 0.2 && distanceFeet > 0) {
            hoodActive = true;
        }

        // --- Right Trigger → shooter active ---
        if (gamepad1.right_trigger > 0.2 && distanceFeet > 0) {
            shooterActive = true;
        }

        // --- Update hood if active ---
        if (hoodActive && distanceFeet > 0) {
            servoPosition = SERVO_STEP_PER_FOOT * (distanceFeet-1);
            servoPosition = Math.max(0.0, Math.min(1.0, servoPosition));
            hoodServo.setPosition(servoPosition);
        }

        // --- Update shooter if active ---
        if (shooterActive && distanceFeet > 0) {
            flywheelPower = Math.min(1.0, ((0.13 * distanceFeet)));

            board.flyWheelMotor.setPower(flywheelPower);
            board.flyWheelMotor2.setPower(flywheelPower);
        }

        // --- Keep servo powered always (holding position) ---
        hoodServo.setPosition(servoPosition);

        telemetry.addData("Servo Pos", "%.2f", servoPosition);
        telemetry.addData("Shooter Power", "%.2f", flywheelPower);
        telemetry.addData("Hood Active", hoodActive);
        telemetry.addData("Shooter Active", shooterActive);
        telemetry.addData("Both Active", bothActive);
        telemetry.update();

    }
}
