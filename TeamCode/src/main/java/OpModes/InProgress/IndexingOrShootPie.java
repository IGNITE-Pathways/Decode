package OpModes.InProgress;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import ProgrammingBoard.ProgrammingBoardShooter;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;

import org.firstinspires.ftc.robotcore.external.JavaUtil;

import java.util.HashMap;
import java.util.Map;

//@Disabled
@TeleOp(name = "Main Op Mode (CRServo Indexed)", group = "Linear OpMode")
public class IndexingOrShootPie extends LinearOpMode {

    ProgrammingBoardShooter board = new ProgrammingBoardShooter();
    private NormalizedColorSensor intakeColorSensor;
    private CRServo indexServo; // ✅ New servo replacing pie motor/servo

    // Pie color pattern memory (3 slots)
    Map<Integer, String> indexColors = new HashMap<>();

    // Expected shoot order
    String[] need_colors = {"purple", "purple", "green"};

    int flag = 0; // index in need_colors
    int currentDivision = 0; // 0–2
    boolean imperfect = false;

    @Override
    public void runOpMode() {
        intakeColorSensor = hardwareMap.get(NormalizedColorSensor.class, "intakeSensor");
        indexServo = hardwareMap.get(CRServo.class, "indexServo"); // ✅ renamed

        board.initializeComponents(hardwareMap);

        // Initialize stored pie colors (can start unknown)
        indexColors.put(0, "none");
        indexColors.put(1, "none");
        indexColors.put(2, "none");

        boolean isPurple = false;
        boolean isGreen = false;
        boolean intakeJustDetectedBall = false;

        waitForStart();

        while (opModeIsActive()) {
            NormalizedRGBA colors = intakeColorSensor.getNormalizedColors();
            float hue = JavaUtil.colorToHue(colors.toColor());

            // === Intake color detection ===
            if (hue > 160 && hue < 350) { // purple
                isPurple = true;
                isGreen = false;
                intakeJustDetectedBall = true;
            } else if (hue >= 100 && hue <= 160) { // green
                isGreen = true;
                isPurple = false;
                intakeJustDetectedBall = true;
            } else {
                isPurple = false;
                isGreen = false;
            }


            // ===============================
            // Intake ball tracking logic
            // ===============================
            if (intakeJustDetectedBall) {
                // TODO: Check for intake being ran before confirming detection
                String detectedColor = isPurple ? "purple" : (isGreen ? "green" : "unknown");
                indexColors.put(currentDivision, detectedColor);
                telemetry.addData("Detected new ball:", detectedColor);
                intakeJustDetectedBall = false;
            }

            String neededBall = need_colors[flag];

            // ===========================
            // A → Manually move one division
            // ===========================
            if (gamepad1.a) {
                currentDivision = (currentDivision + 1) % 3;
                movePie(indexServo, 0.6, 350); // one step clockwise
            }

            // ======================================
            // X → Find & move to correct color slot
            // ======================================
            if (gamepad1.x) {
                movePieToNearest(indexServo, neededBall);

                // === Shoot sequence ===
                board.flyWheelMotor.setPower(1);
                sleep(3000);
                board.flyWheelMotor.setPower(0);

                flag = (flag + 1) % need_colors.length;
                imperfect = false;
            }

            telemetry.addData("Needed Ball", neededBall);
            telemetry.addData("Division", currentDivision);
            telemetry.addData("Pie Colors", indexColors.toString());
            telemetry.addData("Hue", hue);
            telemetry.update();
        }
    }

    /**
     * Move the servo forward for a specific time.
     */
    private void movePie(CRServo servo, double power, int ms) {
        servo.setPower(power);
        sleep(ms);
        servo.setPower(0);
    }

    /**
     * Move the servo in the opposite direction.
     */
    private void movePieReverse(CRServo servo, double power, int ms) {
        servo.setPower(-power);
        sleep(ms);
        servo.setPower(0);
    }

    /**
     * Finds the nearest slot containing the needed color and moves to it.
     * Only moves 1 step left, right, or none.
     */
    private void movePieToNearest(CRServo servo, String neededColor) {
        int targetIndex = -1;

        // Find where the needed color is stored
        for (Map.Entry<Integer, String> entry : indexColors.entrySet()) {
            if (entry.getValue().equals(neededColor)) {
                targetIndex = entry.getKey();
                break;
            }
        }

        if (targetIndex == -1) {
            telemetry.addData("No matching color found in pie", neededColor);
            telemetry.update();
            return;
        }

        // Compute distance in modular arithmetic (since circular)
        int left = (currentDivision - targetIndex + 3) % 3;
        int right = (targetIndex - currentDivision + 3) % 3;

        if (left == 1 && right == 2) { // one step left
            movePieReverse(servo, 0.6, 350);
            currentDivision = (currentDivision + 2) % 3; // move left
        } else if (right == 1 && left == 2) { // one step right
            movePie(servo, 0.6, 350);
            currentDivision = (currentDivision + 1) % 3; // move right
        } else {
            telemetry.addData("Already aligned with needed ball", neededColor);
        }

        telemetry.update();
    }
}
