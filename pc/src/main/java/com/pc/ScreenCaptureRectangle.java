package com.pc;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;


public class ScreenCaptureRectangle {

    public static Rectangle captureRectAdjusted;
    private static Rectangle captureRect;
    public static final JFrame jFrame = new JFrame("Select a rectangle");
    public static final JLabel screenLabel = new JLabel();
    public static Robot robot = null;
    public static Rectangle screenSizes = new Rectangle(0, 0, 0, 0);
    public static final JLabel selectionLabel = new JLabel(
            "Drag a rectangle in the screen shot!");
    public static BufferedImage screen;
    public static BufferedImage screenCopy;

    public static void assignCapturedRectangle() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();

        for (GraphicsDevice g : gs) {
            screenSizes = screenSizes.union(g.getDefaultConfiguration().getBounds());
        }
        assert robot != null;
        screen = robot.createScreenCapture(new Rectangle(screenSizes));

        screenCopy = new BufferedImage(
                screen.getWidth(),
                screen.getHeight(),
                screen.getType());
        screenLabel.setIcon(new ImageIcon(screenCopy));
        JScrollPane screenScroll = new JScrollPane(screenLabel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(screenScroll, BorderLayout.CENTER);


        panel.add(selectionLabel, BorderLayout.SOUTH);

        repaint(screen, screenCopy);
        screenLabel.repaint();
        addMouseListener();


        JButton okButton = new JButton("OK");
        okButton.setBackground(Color.GRAY);
        okButton.addActionListener((actionEvent)->{
            Flow.screenRect = captureRectAdjusted;
            jFrame.setVisible(false);
        });
        jFrame.add(okButton, BorderLayout.PAGE_END);

        jFrame.getRootPane().setDefaultButton(okButton);
        jFrame.add(panel);
        jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    static void addMouseListener() {
        screenLabel.addMouseMotionListener(new MouseMotionAdapter() {

            Point start = new Point();

            @Override
            public void mouseMoved(MouseEvent me) {
                start = me.getPoint();
                repaint(screen, screenCopy);
                selectionLabel.setText("Start Point: " + start);
                screenLabel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent me) {
                Point end = me.getPoint();
                captureRect = new Rectangle(start,
                        new Dimension(end.x-start.x, end.y-start.y));

                repaint(screen, screenCopy);
                screenLabel.repaint();
                captureRectAdjusted = new Rectangle(captureRect.x, captureRect.y + jFrame.getInsets().top, captureRect.width, captureRect.height);
                selectionLabel.setText("Rectangle: " + captureRect);
            }
        });
    }

    public static void repaint(BufferedImage orig, BufferedImage copy) {
        Graphics2D g = copy.createGraphics();
        g.drawImage(orig,0,0, null);
        if (captureRect!=null) {
            g.setColor(Color.RED);
            g.draw(captureRect);
            g.setColor(new Color(255,255,255,150));
            g.fill(captureRect);
        }
        g.dispose();
    }
}