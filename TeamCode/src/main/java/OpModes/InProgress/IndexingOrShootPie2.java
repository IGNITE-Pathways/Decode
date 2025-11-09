package OpModes.InProgress;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import ProgrammingBoard.ProgrammingBoardOTHER;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.JavaUtil;

import java.util.HashMap;
import java.util.Map;

@TeleOp(name = "Main Op Mode 2", group = "Linear OpMode")
public class IndexingOrShootPie2 extends LinearOpMode {

    ProgrammingBoardOTHER board = new ProgrammingBoardOTHER();

    private NormalizedColorSensor intakeColorSensor;
    private Servo indexServo;
    private CRServo intakeServo;
    private Servo kicker;
    private CRServo kickerWheel;

    // Ball color storage (slot 0-2 -> color)
    Map<Integer, String> ballColors = new HashMap<>();

    // Expected shoot order
    String[] need_colors = {"purple", "purple", "green"};
    int indexOFBALLNEEDEDTOBELAUNCHED = 0;

    // ⚙️ TUNE THESE VALUES IF ALIGNMENT IS OFF ⚙️
    private static final double MAX_DEGREES = 720.0;
    private static final double STEP_DEGREES = 60;   // Adjust this until each press = exactly 1/3 rotation
    private static final int SERVO_MOVE_TIME_MS = 100; // Time to wait for servo to complete movement

    private static final int NUM_SLOTS = 3;
    private static final double INITIAL_SERVO_POSITION = 30.0;

    private double targetDegrees = INITIAL_SERVO_POSITION;
    private int currentSlotAtLaunch = 0;

    // Kicker config
    private static final double KICK_REST_POS = 0.50;
    private static final double KICK_FIRE_POS = 0.00;
    private static final int KICK_PULSE_MS = 180;

    // Kicker wheel config
    private static final double KICKER_WHEEL_POWER = -1;  // Negative = counterclockwise
    private static final int KICKER_WHEEL_SPIN_TIME_MS = 2000;  // 2 seconds

    private boolean prevA = false;
    private boolean prevB = false;
    private boolean prevX = false;
    private boolean prevY = false;

    private boolean intakeRunning = true;

    private String lastAction = "Initializing...";

    @Override
    public void runOpMode() {
        board.initializeComponents(hardwareMap);

        intakeColorSensor = board.intakeColorSensor;
        indexServo = board.indexServo;
        intakeServo = board.intakeServo;
        kicker = board.BallLauncherServo;
        kickerWheel = board.kickerWheel;

        // Initialize intake (ON by default)
        intakeServo.setPower(1.0);
        intakeRunning = true;

        // Initialize kicker wheel (OFF by default)
        kickerWheel.setPower(0.0);

        // Initialize ball colors
        ballColors.put(0, "none");
        ballColors.put(1, "none");
        ballColors.put(2, "none");

        // ✅ CRITICAL: Set servo to initial position DURING INIT (before pressing PLAY)
        targetDegrees = INITIAL_SERVO_POSITION;
        indexServo.setPosition(degToPos(targetDegrees));
        currentSlotAtLaunch = 0;

        kicker.setPosition(KICK_REST_POS);

        lastAction = "Servo moving to " + INITIAL_SERVO_POSITION + "°...";

        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("   INITIALIZING SPINDEXER");
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addData("Servo Target", "%.0f°", targetDegrees);
        telemetry.addData("Status", "Moving to start position...");
        telemetry.update();

        // ✅ Wait for servo to reach position BEFORE allowing PLAY
        sleep(1500);

        lastAction = "Ready - servo at " + INITIAL_SERVO_POSITION + "°";
        telemetry.clear();
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("   ✓ READY TO START");
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addData("Servo Position", "%.0f°", targetDegrees);
        telemetry.addData("Slot at Launch", "%d", currentSlotAtLaunch);
        telemetry.addData("Intake", "ON");
        telemetry.addLine("───────────────────────────────────");
        telemetry.addLine("Press ▶ PLAY button to continue");
        telemetry.update();

        waitForStart();

        lastAction = "OpMode running";

        while (opModeIsActive()) {
            // Update color sensor reading
            NormalizedRGBA colors = intakeColorSensor.getNormalizedColors();
            float hue = JavaUtil.colorToHue(colors.toColor());

            // Handle button inputs
            handleButtonInputs(hue);

            // Display real-time telemetry
            displayTelemetry(hue);
            telemetry.update();
        }
    }

    /**
     * Handle all button inputs
     */
    private void handleButtonInputs(float hue) {
        boolean a = gamepad1.a;
        boolean b = gamepad1.b;
        boolean x = gamepad1.x;
        boolean y = gamepad1.y;

        // A button: rotate one slot clockwise
        if (a && !prevA) {
            double nextPos = targetDegrees - STEP_DEGREES;

            // Check if we would exceed max range
            if (nextPos < 0) {
                // Would wrap - check if this is valid
                double wrapped = nextPos + MAX_DEGREES;
                if (wrapped > MAX_DEGREES - STEP_DEGREES) {
                    lastAction = "⚠ AT MAX! Press Y to reset to 0°";
                } else {
                    rotateOneSlot();
                }
            } else {
                rotateOneSlot();
            }
        }

        // B button: kick ball with kicker wheel
        if (b && !prevB) {
            lastAction = "Kicking ball with wheel spinning...";
            kickBall();
            lastAction = "Ball kicked from slot " + currentSlotAtLaunch;
        }

        // X button: toggle intake on/off
        if (x && !prevX) {
            intakeRunning = !intakeRunning;
            intakeServo.setPower(intakeRunning ? 1.0 : 0.0);
            lastAction = "Intake: " + (intakeRunning ? "ON" : "OFF");
        }

        // Y button: reset to 0 degrees
        if (y && !prevY) {
            lastAction = "Resetting to 0°...";
            resetToZero();
        }

        // Auto-detect and add balls
        if (intakeRunning) {  // Only detect when intake is running
            if (hue > 160 && hue < 350) {
                if (!ballColors.get(currentSlotAtLaunch).equals("purple")) {
                    ballColors.put(currentSlotAtLaunch, "purple");
                    lastAction = "✓ PURPLE detected → Slot " + currentSlotAtLaunch;
                }
            } else if (hue >= 100 && hue <= 160) {
                if (!ballColors.get(currentSlotAtLaunch).equals("green")) {
                    ballColors.put(currentSlotAtLaunch, "green");
                    lastAction = "✓ GREEN detected → Slot " + currentSlotAtLaunch;
                }
            }
        }

        prevA = a;
        prevB = b;
        prevX = x;
        prevY = y;
    }

    /**
     * Simplified, real-time telemetry
     */
    private void displayTelemetry(float hue) {
        telemetry.clear();

        // === HEADER ===
        telemetry.addLine("╔═══════════════════════════════════╗");
        telemetry.addLine("║      SPINDEXER CONTROL           ║");
        telemetry.addLine("╚═══════════════════════════════════╝");
        telemetry.addLine();

        // === SERVO POSITION ===
        telemetry.addData("🔧 SERVO", "%.0f° / %.0f° MAX", targetDegrees, MAX_DEGREES);
        telemetry.addData("📍 LAUNCH SLOT", ">>> SLOT %d <<<", currentSlotAtLaunch);
        telemetry.addLine();

        // === SLOT CONTENTS ===
        telemetry.addLine("┌─ SLOTS ─────────────────────┐");
        for (int i = 0; i < NUM_SLOTS; i++) {
            String color = ballColors.get(i);
            String marker = (i == currentSlotAtLaunch) ? " ◄◄ LAUNCH" : "";
            String display = color.equals("none") ? "EMPTY" : "●" + color.toUpperCase();
            telemetry.addData("│ Slot " + i, display + marker);
        }
        telemetry.addLine("└─────────────────────────────┘");
        telemetry.addLine();

        // === SENSORS & SYSTEMS ===
        String detected = "—";
        if (hue > 160 && hue < 350) {
            detected = "PURPLE ●";
        } else if (hue >= 100 && hue <= 160) {
            detected = "GREEN ●";
        }
        telemetry.addData("🎨 COLOR", detected + " (Hue: %.0f)", hue);
        telemetry.addData("🔄 INTAKE", intakeRunning ? "ON ✓" : "OFF ✗");
        telemetry.addData("⚡ KICKER", kicker.getPosition() < 0.3 ? "FIRING" : "REST");
        telemetry.addData("⚙️ KICKER WHEEL", Math.abs(kickerWheel.getPower()) > 0.1 ? "SPINNING ⟳" : "STOPPED");
        telemetry.addLine();

        // === CONTROLS ===
        telemetry.addLine("┌─ CONTROLS ──────────────────┐");
        telemetry.addData("│ [A]", gamepad1.a ? "▶ ROTATING" : "Rotate +120°");
        telemetry.addData("│ [B]", gamepad1.b ? "▶ KICKING" : "Kick + Wheel");
        telemetry.addData("│ [X]", gamepad1.x ? "▶ TOGGLING" : "Toggle Intake");
        telemetry.addData("│ [Y]", gamepad1.y ? "▶ RESETTING" : "Reset to 0°");
        telemetry.addLine("└─────────────────────────────┘");
        telemetry.addLine();

        // === LAST ACTION ===
        telemetry.addData("📋 ACTION", lastAction);

        // === TUNING HINT ===
        if (targetDegrees > MAX_DEGREES - 100) {
            telemetry.addLine();
            telemetry.addLine("⚠ NEAR MAX - Press Y to reset");
        }
    }

    /**
     * Rotate the spindexer by one slot (120° physical) clockwise
     */
    private void rotateOneSlot() {
        double oldTarget = targetDegrees;
        int oldSlot = currentSlotAtLaunch;

        // Move servo BACKWARD by STEP_DEGREES (clockwise direction)
        targetDegrees = targetDegrees - STEP_DEGREES;

        // Handle wraparound
        if (targetDegrees < 0) {
            targetDegrees += MAX_DEGREES;
        }

        // Command the servo
        indexServo.setPosition(degToPos(targetDegrees));

        // Update which slot is at launch position (cycles 0→1→2→0)
        currentSlotAtLaunch = (currentSlotAtLaunch + 1) % NUM_SLOTS;

        lastAction = String.format("Rotated: %.0f°→%.0f° | Slot %d→%d",
                oldTarget, targetDegrees, oldSlot, currentSlotAtLaunch);

        // ✅ CRITICAL: Wait for servo to complete movement
        sleep(SERVO_MOVE_TIME_MS);
    }

    /**
     * Reset servo to 0 degrees
     */
    private void resetToZero() {
        targetDegrees = 0.0;
        indexServo.setPosition(degToPos(targetDegrees));

        // Reset slot tracking
        currentSlotAtLaunch = 0;

        lastAction = "Reset complete - Servo at 0°, Slot 0 at launch";

        // Wait for reset to complete
        sleep(1500);
    }

    /**
     * Find which slot contains the specified color
     */
    private int findSlotWithColor(String color) {
        for (Map.Entry<Integer, String> entry : ballColors.entrySet()) {
            if (entry.getValue().equals(color)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * Kick ball with kicker servo AND spin the kicker wheel counterclockwise
     */
    private void kickBall() {
        double lockPosition = indexServo.getPosition();

        // Lock index servo
        indexServo.setPosition(lockPosition);

        // ✅ START kicker wheel spinning counterclockwise
        kickerWheel.setPower(KICKER_WHEEL_POWER);

        // Fire kicker servo
        kicker.setPosition(KICK_FIRE_POS);
        sleep(KICK_PULSE_MS);

        // Return kicker servo to rest
        kicker.setPosition(KICK_REST_POS);

        // ✅ Keep kicker wheel spinning for the full duration
        sleep(KICKER_WHEEL_SPIN_TIME_MS - KICK_PULSE_MS);

        // ✅ STOP kicker wheel
        kickerWheel.setPower(0.0);

        // Re-lock index servo
        indexServo.setPosition(lockPosition);
    }

    private static double degToPos(double degrees) {
        return Range.clip(degrees / MAX_DEGREES, 0.0, 1.0);
    }

    private static double posToDeg(double pos) {
        return Range.clip(pos, 0.0, 1.0) * MAX_DEGREES;
    }
}