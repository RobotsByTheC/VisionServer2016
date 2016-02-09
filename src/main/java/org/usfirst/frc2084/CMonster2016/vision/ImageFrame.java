/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.CMonster2016.vision;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import org.opencv.core.Mat;
import org.usfirst.frc.team2084.CMonster2016.vision.ImageConvertor;

/**
 * @author Ben Wolsieffer
 */
@SuppressWarnings("serial")
public class ImageFrame extends JFrame {

    private volatile BufferedImage javaImage;
    private final ImageConvertor convertor = new ImageConvertor();

    /**
     * Creates a image frame with the specified title.
     * 
     * @param title the title of the frame
     */
    public ImageFrame(String title) {
        super(title);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /**
     * Shows the specified image in the frame. The frame resizes to fit the
     * image.
     * 
     * @param image the image to show
     */
    public void showImage(Mat image) {
        // Get the properties of the Mat
        int width = image.width();
        int height = image.height();
        synchronized (this) {
            // Copy Mat data to BufferedImage
            javaImage = convertor.toBufferedImage(image);
        }
        setSize(width, height);
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        synchronized (this) {
            // Draw the image.
            g.drawImage(javaImage, 0, 0, this);
        }
    }
}
