//package OpModes.InProgress;
//
//import com.qualcomm.hardware.limelightvision.Limelight3A;
//import com.qualcomm.hardware.limelightvision.LLResult;
//import com.qualcomm.hardware.limelightvision.LLResultTypes;
//import com.qualcomm.robotcore.eventloop.opmode.OpMode;
//import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
//import com.qualcomm.robotcore.hardware.DcMotor;
//import com.qualcomm.robotcore.hardware.Servo;
//import ProgrammingBoard.ProgrammingBoardShooter;
//
//import java.util.List;
//
//@TeleOp(name = "Combined (Drive + Turret + Hood + Tag 24 only)", group = "Main")
//public class MainOpMode extends OpMode {
//
//    // ===== DriveTrain =====
//    DcMotor frontLeftDrive;
//    DcMotor frontRightDrive;
//    DcMotor backLeftDrive;
//    DcMotor backRightDrive;
//
//    // ===== Turret =====
//    private Limelight3A limelight;
//    private Servo turretServo;
//    public static double TOLERANCE = 15.6;
//    private static final double ADJUST_SPEED = 0.007;
//    private static final double SEARCH_SPEED = 0.0013;
//    private static final double SERVO_MIN = 0.0;
//    private static final double SERVO_MAX = 1.0;
//    private double servoPos = 0.5;
//    private double lastTx = 0;
//    private boolean searchRight = true;
//    private boolean aligned = false;
//
//    // ===== Hood and Shooter =====
//    private Servo hoodServo;
//    private ProgrammingBoardShooter board = new ProgrammingBoardShooter();
//    private double servoPosition = 0.0;
//    private double flywheelPower = 0.0;
//    private boolean hoodActive = true;
//    private boolean shooterActive = true;
//    private boolean bothActive = true;
//
//    private static final double SERVO_STEP_PER_FOOT = 0.15;
//    private static final double APRILTAG_REAL_HEIGHT_METERS = 0.2032; // 8 inches
//    private static final double CAMERA_VERTICAL_FOV_DEGREES = 49.5;
//    private static final int IMAGE_WIDTH_PIXELS = 1280;
//    private static final int IMAGE_HEIGHT_PIXELS = 720;
//
//    @Override
//    public void init() {
//        // ---- Drive init ----
//        frontLeftDrive = hardwareMap.get(DcMotor.class, "leftfrontmotor");
//        frontRightDrive = hardwareMap.get(DcMotor.class, "rightfrontmotor");
//        backLeftDrive = hardwareMap.get(DcMotor.class, "leftbackmotor");
//        backRightDrive = hardwareMap.get(DcMotor.class, "rightbackmotor");
//
//        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
//        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
//        frontRightDrive.setDirection(DcMotor.Direction.REVERSE);
//        backRightDrive.setDirection(DcMotor.Direction.FORWARD);
//
//        // ---- Limelight + Turret ----
//        limelight = hardwareMap.get(Limelight3A.class, "limelight");
//        turretServo = hardwareMap.get(Servo.class, "turretServo");
//        if (limelight != null) {
//            limelight.setPollRateHz(100);
//            limelight.start();
//        }
//        turretServo.setPosition(servoPos);
//
//        // ---- Hood + Shooter ----
//        hoodServo = hardwareMap.get(Servo.class, "hoodservo");
//        board.initializeComponents(hardwareMap);
//
//        telemetry.addLine("Combined TeleOp Initialized (Tag 24 only)");
//    }
//
//    @Override
//    public void loop() {
//        // ===== DRIVE =====
//        double forward = -gamepad1.left_stick_y;
//        double right = gamepad1.left_stick_x;
//        double rotate = gamepad1.right_stick_x;
//        right *= 1.1;
//
//        double frontLeftPower = forward + right + rotate;
//        double frontRightPower = forward - right - rotate;
//        double backLeftPower = forward - right + rotate;
//        double backRightPower = forward + right - rotate;
//
//        double max = Math.max(
//                Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
//                Math.max(Math.abs(backLeftPower), Math.abs(backRightPower))
//        );
//        if (max > 1.0) {
//            frontLeftPower /= max;
//            frontRightPower /= max;
//            backLeftPower /= max;
//            backRightPower /= max;
//        }
//
//        frontLeftDrive.setPower(frontLeftPower);
//        frontRightDrive.setPower(frontRightPower);
//        backLeftDrive.setPower(backLeftPower);
//        backRightDrive.setPower(backRightPower);
//
//        // ===== LIMELIGHT DETECTION =====
//        LLResult result = limelight.getLatestResult();
//        double distanceFeet = -1;
//        boolean validTag24 = false;
//        double tx = 0;
//
//        if (result != null && result.isValid()) {
//            List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();
//            for (LLResultTypes.FiducialResult fr : fiducialResults) {
//                telemetry.addData("Fiducial", "ID: %d, Family: %s, X: %.2f, Y: %.2f",
//                        fr.getFiducialId(), fr.getFamily(),
//                        fr.getTargetXDegrees(), fr.getTargetYDegrees());
//
//                if (fr.getFiducialId() == 24) {
//                    validTag24 = true;
//                    tx = fr.getTargetXDegrees();
//
//                    double taPercent = result.getTa();
//                    if (taPercent > 0.0) {
//                        double pixelArea = (taPercent / 100.0) * (IMAGE_WIDTH_PIXELS * IMAGE_HEIGHT_PIXELS);
//                        double tagPixelHeight = Math.sqrt(pixelArea);
//                        double focalPx = (IMAGE_HEIGHT_PIXELS / 2.0)
//                                / Math.tan(Math.toRadians(CAMERA_VERTICAL_FOV_DEGREES / 2.0));
//                        double distanceMeters = (APRILTAG_REAL_HEIGHT_METERS * focalPx) / tagPixelHeight;
//                        distanceFeet = distanceMeters * 3.28084;
//                    }
//                }
//            }
//        }
//
//        // ===== TURRET AUTO-ALIGN (Tag 24 only) =====
//        if (validTag24) {
//            if (Math.abs(tx) <= TOLERANCE) {
//                aligned = true;
//            } else {
//                aligned = false;
//                boolean movingCorrect = Math.abs(tx) < Math.abs(lastTx);
//                if (movingCorrect) {
//                    servoPos += (searchRight ? ADJUST_SPEED : -ADJUST_SPEED);
//                } else {
//                    searchRight = !searchRight;
//                    servoPos += (searchRight ? ADJUST_SPEED : -ADJUST_SPEED);
//                }
//                servoPos = Math.max(SERVO_MIN, Math.min(SERVO_MAX, servoPos));
//                turretServo.setPosition(servoPos);
//            }
//            lastTx = tx;
//        } else {
//            aligned = false;
//            // Idle scanning pattern when tag not found
//            if (searchRight) servoPos += SEARCH_SPEED;
//            else servoPos -= SEARCH_SPEED;
//            if (servoPos >= SERVO_MAX) searchRight = false;
//            if (servoPos <= SERVO_MIN) searchRight = true;
//            servoPos = Math.max(SERVO_MIN, Math.min(SERVO_MAX, servoPos));
//            turretServo.setPosition(servoPos);
//        }
//
//        // ===== HOOD + SHOOTER (Tag 24 only) =====
//        if (validTag24 && distanceFeet > 0) {
//            if (gamepad1.square) {
//                bothActive = true;
//                hoodActive = true;
//                shooterActive = true;
//            }
//
//            if (gamepad1.left_trigger > 0.2) hoodActive = true;
//            if (gamepad1.right_trigger > 0.2) shooterActive = true;
//
//            if (hoodActive) {
//                if (distanceFeet < 5){
//                    servoPosition = SERVO_STEP_PER_FOOT * (distanceFeet + 1.1);
//                    TOLERANCE = 15.6;
//
//                }
//                if (distanceFeet > 7){
//                    servoPosition = SERVO_STEP_PER_FOOT * (distanceFeet + 0.3);
//                    TOLERANCE = 20.5;
//                }
//                else if (distanceFeet > 5){
//                    servoPosition = SERVO_STEP_PER_FOOT * (distanceFeet - 1);
//                    TOLERANCE = 17.6;
//                }
//                servoPosition = SERVO_STEP_PER_FOOT * (distanceFeet - 1);
//                servoPosition = Math.max(0.0, Math.min(1.0, servoPosition));
//                hoodServo.setPosition(servoPosition);
//            }
//
//            if (shooterActive) {
//                if (distanceFeet < 5){
//                    flywheelPower = Math.min(1.0, ((0.18 * distanceFeet)));
//                }
//                if (distanceFeet > 7){
//                    flywheelPower = Math.min(1.0, ((0.125 * distanceFeet)));
//                }
//                else if (distanceFeet > 5){
//                    flywheelPower = Math.min(1.0, ((0.130 * distanceFeet)));
//                }
//
//                board.flyWheelMotor.setPower(flywheelPower);
//                board.flyWheelMotor2.setPower(flywheelPower);
//            }
//        } else {
//            // Stop shooter when no valid tag 24
//            board.flyWheelMotor.setPower(0);
//            board.flyWheelMotor2.setPower(0);
//        }
//
//        // ===== TELEMETRY =====
//        telemetry.addLine("=== Drive ===");
//        telemetry.addData("FL", frontLeftPower);
//        telemetry.addData("FR", frontRightPower);
//        telemetry.addData("BL", backLeftPower);
//        telemetry.addData("BR", backRightPower);
//
//        telemetry.addLine("=== Vision ===");
//        telemetry.addData("Valid Tag 24", validTag24);
//        telemetry.addData("Distance (ft)", distanceFeet);
//        telemetry.addData("tx", tx);
//
//        telemetry.addLine("=== Turret ===");
//        telemetry.addData("Servo Pos", servoPos);
//        telemetry.addData("Aligned", aligned);
//
//        telemetry.addLine("=== Hood/Shooter ===");
//        telemetry.addData("Servo Pos", servoPosition);
//        telemetry.addData("Shooter Power", flywheelPower);
//        telemetry.addData("Hood Active", hoodActive);
//        telemetry.addData("Shooter Active", shooterActive);
//        telemetry.addData("Both Active", bothActive);
//        telemetry.update();
//    }
//}
