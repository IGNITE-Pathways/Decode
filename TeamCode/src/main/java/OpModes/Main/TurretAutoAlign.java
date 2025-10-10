package OpModes.Main;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name="Turret Servo Auto Align (Smart TX Direction + Stop on Target)", group="Main")
public class TurretAutoAlign extends OpMode {
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
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        turretServo = hardwareMap.get(Servo.class, "turretServo");

        if (limelight != null) {
            limelight.setPollRateHz(100);
            limelight.start();
        }

        turretServo.setPosition(servoPos);
        telemetry.addLine("Turret Servo Auto Align Initialized");
    }

    @Override
    public void loop() {
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
    }
}
