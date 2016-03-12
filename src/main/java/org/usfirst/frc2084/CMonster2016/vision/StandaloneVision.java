/* 
 * Copyright (c) 2016 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.CMonster2016.vision;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.usfirst.frc.team2084.CMonster2016.vision.HighGoalProcessor;
import org.usfirst.frc.team2084.CMonster2016.vision.ImageHandler;
import org.usfirst.frc.team2084.CMonster2016.vision.OpenCVLoader;
import org.usfirst.frc.team2084.CMonster2016.vision.VideoServer;
import org.usfirst.frc.team2084.CMonster2016.vision.VisionParameters;
import org.usfirst.frc.team2084.CMonster2016.vision.VisionProcessor;
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

    private CameraCapture camera;
    private VisionProcessor processor;
    private VideoServer videoServer;

    /**
     * Runs the vision processing algorithm and displays the results in a
     * window.
     */
    public StandaloneVision() {
        try {
            // NetworkTable.initialize();
            NetworkTable.setClientMode();
            NetworkTable.setIPAddress("roborio-2084-frc.local");

            // Wait to get latest network table values
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }

            int device = VisionParameters.getCameraSourceLocal();
            if (device != -1) {
                camera = new CameraCapture(device);
            } else {
                camera = new CameraCapture(VisionParameters.getCameraSourceRemote());
            }

            processor = new HighGoalProcessor(camera);

            videoServer = new VideoServer(1180, 20);
            videoServer.start();

            // Initialize the vision processor.
            processor.addImageHandler(new ImageHandler() {

                /**
                 * Displays the processed image.
                 */
                @Override
                public void imageProcessed(Mat image) {
                    if (!headless) {
                        try {
                            SwingUtilities.invokeAndWait(() -> imageFrame.showImage(image));
                        } catch (InvocationTargetException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        videoServer.setQuality(VisionParameters.getStreamQuality());
                    }
                    try {
                        videoServer.sendImage(image);
                    } catch (IOException e) {
                        System.out.println("Cannot stream video over network: " + e);
                    }

                    if (VisionParameters.shouldShutdown()) {
                        try {
                            Runtime.getRuntime().exec("sudo poweroff").waitFor();
                            System.exit(0);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
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

            if (!headless) {
                initGUI();
            }

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
            e.printStackTrace();
            System.exit(UNKNOWN_ERROR);
        }
    }

    public static void main(String[] args) {
        OpenCVLoader.loadOpenCV();
        new StandaloneVision();
    }
}
