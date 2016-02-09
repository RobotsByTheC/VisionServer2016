/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.CMonster2016.vision;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.usfirst.frc.team2084.CMonster2016.vision.HighGoalProcessor;
import org.usfirst.frc.team2084.CMonster2016.vision.ImageHandler;
import org.usfirst.frc.team2084.CMonster2016.vision.OpenCVLoader;
import org.usfirst.frc.team2084.CMonster2016.vision.VideoServer;
import org.usfirst.frc.team2084.CMonster2016.vision.VisionProcessor;
import org.usfirst.frc.team2084.CMonster2016.vision.capture.CameraCapture;

/**
 * @author Ben Wolsieffer
 */
public class StandaloneVision {

    public static final int CAMERA_OPEN_ERROR = 1;
    public static final int VIDEO_SERVER_ERROR = 2;
    public static final int HEADLESS_ERROR = 3;
    public static final int UNKNOWN_ERROR = 4;

    private static final boolean headless = GraphicsEnvironment.isHeadless();

    private ImageFrame imageFrame;
    private final HashMap<String, ImageFrame> debugFrames = new HashMap<>();

    private final CameraCapture camera = new CameraCapture(0);
    private final VisionProcessor processor = new HighGoalProcessor(camera);
    private VideoServer videoServer;

    /**
     * Runs the vision processing algorithm and displays the results in a
     * window.
     */
    public StandaloneVision() {
        try {
            videoServer = new VideoServer(1180, 75);
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
