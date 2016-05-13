/* 
 * Copyright (c) 2016 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.CMonster2016.vision;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.usfirst.frc.team2084.CMonster2016.vision.HighGoalProcessor;
import org.usfirst.frc.team2084.CMonster2016.vision.OpenCVLoader;
import org.usfirst.frc.team2084.CMonster2016.vision.UDPVideoServer;
import org.usfirst.frc.team2084.CMonster2016.vision.VisionParameters;
import org.usfirst.frc.team2084.CMonster2016.vision.VisionProcessor;
import org.usfirst.frc.team2084.CMonster2016.vision.VisionProcessor.DebugHandler;
import org.usfirst.frc.team2084.CMonster2016.vision.capture.CameraCapture;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

/**
 * @author Ben Wolsieffer
 */
public class StandaloneVision {

    public static final int CAMERA_OPEN_ERROR = 1;
    public static final int VIDEO_SERVER_ERROR = 2;
    public static final int UNKNOWN_ERROR = 3;

    private static final boolean headless = GraphicsEnvironment.isHeadless();

    private ImageFrame imageFrame;
    private final HashMap<String, ImageFrame> debugFrames = new HashMap<>();

    private CameraCapture aimingCamera;
    private CameraCapture intakeCamera;
    private VisionProcessor goalProcessor;
    private UDPVideoServer videoServer;

    /**
     * Runs the vision processing algorithm and displays the results in a
     * window.
     */
    public StandaloneVision() {
        try {
            // NetworkTable.initialize();
            NetworkTable.setClientMode();
            NetworkTable.setIPAddress("10.20.84.2");

            // Wait to get latest network table values
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }

            aimingCamera = new CameraCapture(getCameraIndex(new File("/dev/aiming-camera")));
            intakeCamera = new CameraCapture(getCameraIndex(new File("/dev/intake-camera")));

            intakeCamera.setResolution(HighGoalProcessor.IMAGE_SIZE);

            goalProcessor = new HighGoalProcessor(aimingCamera);

            videoServer = new UDPVideoServer(20);
            videoServer.start();

            DebugHandler debugHandler = (name, image) -> {
                if (!headless) {
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            ImageFrame debugFrame = debugFrames.get(name);
                            if (debugFrame == null) {
                                debugFrame = new ImageFrame(name);
                                debugFrame.setVisible(true);
                                debugFrames.put(name, debugFrame);
                            }
                            debugFrame.showImage(image);
                        });
                    } catch (InvocationTargetException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            // Initialize the vision processor.
            goalProcessor.addDebugHandler(debugHandler);

            if (!headless) {
                initGUI();
            }

            Mat intakeImage = new Mat();
            Mat aimingImage = new Mat();

            (new Thread(() -> {
                intakeCamera.start();
                while (true) {
                    videoServer.setQuality(VisionParameters.getStreamQuality());

                    if (VisionParameters.shouldShutdown()) {
                        try {
                            Runtime.getRuntime().exec("sudo poweroff").waitFor();
                            System.exit(0);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (VisionParameters.isIntakeCamera()) {
                        if (intakeCamera.capture(intakeImage, 300)) {
                            outputImage(intakeImage);
                        }
                    }
                }
            })).start();

            (new Thread(() -> {
                aimingCamera.start();
                while (true) {
                    if (aimingCamera.capture(aimingImage, 300)) {
                        goalProcessor.process(aimingImage);
                        if (!VisionParameters.isIntakeCamera()) {
                            outputImage(aimingImage);
                        }
                    }
                }
            })).start();

        } catch (IOException ioe) {
            System.out.println("Cannot start video server: " + ioe);
            System.exit(VIDEO_SERVER_ERROR);
        }
    }

    private static int getCameraIndex(File file) throws IOException {
        String name = file.getCanonicalPath();

        int offset = name.length();
        for (int i = name.length() - 1; i >= 0; i--) {
            char c = name.charAt(i);
            if (Character.isDigit(c)) {
                offset--;
            } else {
                if (offset == name.length()) {
                    // No int at the end
                    return Integer.MIN_VALUE;
                }
                return Integer.parseInt(name.substring(offset));
            }
        }
        return Integer.parseInt(name.substring(offset));
    }

    private void outputImage(Mat image) {
        if (!headless) {
            try {
                SwingUtilities.invokeAndWait(() -> imageFrame.showImage(image));
            } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            synchronized (videoServer) {
                videoServer.sendImage(image);
            }
        } catch (IOException e) {
            System.err.println("Could not stream video: " + e);
        }
    }

    public void initGUI() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                imageFrame = new ImageFrame("Processed Image");

                imageFrame.setVisible(true);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
            System.exit(UNKNOWN_ERROR);
        }
    }

    public static void main(String[] args) {
        OpenCVLoader.loadOpenCV();
        new StandaloneVision();
    }
}
