package com.pc;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

/** Getting a Rectangle of interest on the screen.
 Requires the MotivatedEndUser API - sold separately. */
public class ScreenCaptureRectangle {

    static Rectangle captureRect;
    static JFrame jFrame = new JFrame();

    public static void assignCapturedRectangle() {
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Rectangle rect = new Rectangle(0, 0, 0, 0);
        for (GraphicsDevice g : gs) {
            rect = rect.union(g.getDefaultConfiguration().getBounds());
        }
        assert robot != null;
        final BufferedImage screen = robot.createScreenCapture(new Rectangle(rect));

        final BufferedImage screenCopy = new BufferedImage(
                screen.getWidth(),
                screen.getHeight(),
                screen.getType());
        final JLabel screenLabel = new JLabel(new ImageIcon(screenCopy));
        JScrollPane screenScroll = new JScrollPane(screenLabel);

//        screenScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//        screenScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

//        screenScroll.setPreferredSize(new Dimension(
//                (int)(screen.getWidth()/1.2),
//                (int)(screen.getHeight()/1.2)));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(screenScroll, BorderLayout.CENTER);

        final JLabel selectionLabel = new JLabel(
                "Drag a rectangle in the screen shot!");
        panel.add(selectionLabel, BorderLayout.SOUTH);

        repaint(screen, screenCopy);
        screenLabel.repaint();

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
                selectionLabel.setText("Rectangle: " + captureRect);
            }
        });

        //JOptionPane.showMessageDialog(null, panel);

        JButton okButton = new JButton("OK");
        okButton.addActionListener((actionEvent)->{
            Flow.screenRect = captureRect;
            jFrame.dispose();
        });
        jFrame.add(okButton, BorderLayout.PAGE_END);

        jFrame.add(panel);
        jFrame.pack();
        jFrame.setVisible(true);

        //System.out.println("Rectangle of interest: " + captureRect);
        //Flow.screenRect = captureRect;
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