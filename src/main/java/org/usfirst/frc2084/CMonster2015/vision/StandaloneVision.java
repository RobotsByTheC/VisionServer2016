/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.CMonster2015.vision;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.usfirst.frc.team2084.CMonster2015.vision.BallProcessor;
import org.usfirst.frc.team2084.CMonster2015.vision.ImageHandler;
import org.usfirst.frc.team2084.CMonster2015.vision.OpenCVLoader;
import org.usfirst.frc.team2084.CMonster2015.vision.Range;
import org.usfirst.frc.team2084.CMonster2015.vision.VideoServer;
import org.usfirst.frc.team2084.CMonster2015.vision.capture.CameraCapture;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;

/**
 * @author Ben Wolsieffer
 */
public class StandaloneVision {

    public static final String H_MIN_KEY = "hMin";
    public static final String H_MAX_KEY = "hMax";

    public static final String S_MIN_KEY = "sMin";
    public static final String S_MAX_KEY = "sMax";

    public static final String V_MIN_KEY = "vMin";
    public static final String V_MAX_KEY = "vMax";

    public static final String MIN_SIZE_KEY = "minSize";

    public static final int CAMERA_OPEN_ERROR = 1;
    public static final int VIDEO_SERVER_ERROR = 2;
    public static final int HEADLESS_ERROR = 3;
    public static final int UNKNOWN_ERROR = 4;

    private static final boolean headless = GraphicsEnvironment.isHeadless();

    private ImageFrame imageFrame;
    private final HashMap<String, ImageFrame> debugFrames = new HashMap<>();

    private final CameraCapture camera = new CameraCapture(0/* "http://192.168.0.90/mjpg/video.mjpg" */);
    private final BallProcessor processor = new BallProcessor(camera);
    private VideoServer videoServer;

    private final ITable visionTable = NetworkTable.getTable("SmartDashboard").getSubTable(
            "Vision");

    /**
     * Runs the vision processing algorithm and displays the results in a
     * window.
     */
    public StandaloneVision() {
        try {
            videoServer = new VideoServer(8080, 75);
            videoServer.start();

            // Initialize the vision processor.
            processor.addImageHandler(new ImageHandler() {

                /**
                 * Displays the processed image.
                 */
                @Override
                public void imageProcessed(Mat image) {
                    processor.setHThreshold(new Range((int) visionTable.getNumber(
                            H_MIN_KEY, 0), (int) visionTable.getNumber(H_MAX_KEY, 255)));
                    processor.setSThreshold(new Range((int) visionTable.getNumber(
                            S_MIN_KEY, 0), (int) visionTable.getNumber(S_MAX_KEY, 255)));
                    processor.setVThreshold(new Range((int) visionTable.getNumber(
                            V_MIN_KEY, 0), (int) visionTable.getNumber(V_MAX_KEY, 255)));

                    if (!headless) {
                        try {
                            SwingUtilities.invokeAndWait(() -> imageFrame.showImage(image));
                        } catch (InvocationTargetException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        videoServer.sendImage(image);
                    } catch (IOException e) {
                        System.out.println("Cannot stream video over network: " + e);
                    }
                }

                /**
                 * Shows the image in its own frame.
                 */
                @Override
                public void debugImage(String name, Mat image) {
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
                }
            });

            initGUI();

            processor.start();

        } catch (IOException ioe) {
            System.out.println("Cannot start video server: " + ioe);
            System.exit(VIDEO_SERVER_ERROR);
        }
    }

    public void initGUI() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                imageFrame = new ImageFrame("Processed Image");

                imageFrame.setVisible(true);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            if (e.getCause() instanceof HeadlessException) {
                System.out.println("Cannot run in a headess environment.");
                System.exit(HEADLESS_ERROR);
            } else {
                e.printStackTrace();
                System.exit(UNKNOWN_ERROR);
            }
        }
    }

    public static void main(String[] args) {
        OpenCVLoader.loadOpenCV();
        new StandaloneVision();
    }
}
