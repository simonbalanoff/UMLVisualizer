package umlrenderer;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

// Keyboard shortcuts:  F = fit all,  R = reset zoom,  Esc = deselect

public class UMLRenderer {
    private static final Color BG = new Color(0x0F1117);
    private static final Color GRID = new Color(0x1C1F2A);
    private static final Color NODE_BG = new Color(0x1A1D27);
    private static final Color NODE_BORDER = new Color(0x2E3246);
    private static final Color NODE_SEL = new Color(0x3D6FFF);
    private static final Color HDR_CLASS = new Color(0x1F2B45);
    private static final Color HDR_ABSTRACT = new Color(0x2B1F45);
    private static final Color HDR_INTERFACE = new Color(0x1F3D35);
    private static final Color HDR_ENUM = new Color(0x3D2B1F);
    private static final Color TXT_NAME = new Color(0xF0F4FF);
    private static final Color TXT_STEREO = new Color(0x7A8AAA);
    private static final Color TXT_MEMBER = new Color(0xB0B8CC);
    private static final Color TXT_STATIC = new Color(0x8899BB);
    private static final Color TXT_ABSTRACT = new Color(0xAA88CC);
    private static final Color TXT_PRIVATE = new Color(0x667799);
    private static final Color DIVIDER = new Color(0x252840);
    private static final Color ARROW_INHERIT = new Color(0x4A7FFF);
    private static final Color ARROW_IMPL = new Color(0x44BB88);
    private static final Color ACCENT_CLASS = new Color(0x3D6FFF);
    private static final Color ACCENT_ABST = new Color(0x9966FF);
    private static final Color ACCENT_IFACE = new Color(0x33CC88);
    private static final Color ACCENT_ENUM = new Color(0xFF8844);
    private static final Color SIDEBAR_BG = new Color(0x13151F);
    private static final Color SIDEBAR_HDR = new Color(0x1A1D2A);
    private static final Color MINI_BG = new Color(0x0C0E16, true);
    private static final Color MINI_VIEWPORT = new Color(0x3D6FFF44, true);

    private static final Font FONT_NAME;
    private static final Font FONT_STEREO;
    private static final Font FONT_MEMBER;
    private static final Font FONT_MEMBER_STATIC;
    private static final Font FONT_MEMBER_ABSTRACT;
    private static final Font FONT_SIDEBAR_HEADER;
    private static final Font FONT_SIDEBAR_LABEL;
    private static final Font FONT_SIDEBAR_VALUE;

    static {
        Font base = loadFont("JetBrains Mono", Font.PLAIN, 12f);
        FONT_NAME = base.deriveFont(Font.BOLD, 13f);
        FONT_STEREO = base.deriveFont(Font.PLAIN, 10f);
        FONT_MEMBER = base.deriveFont(Font.PLAIN, 11.5f);
        FONT_MEMBER_STATIC = addUnderline(base.deriveFont(Font.PLAIN, 11.5f));
        FONT_MEMBER_ABSTRACT = base.deriveFont(Font.ITALIC, 11.5f);
        FONT_SIDEBAR_HEADER = base.deriveFont(Font.BOLD, 15f);
        FONT_SIDEBAR_LABEL = base.deriveFont(Font.PLAIN, 10f);
        FONT_SIDEBAR_VALUE = base.deriveFont(Font.PLAIN, 12f);
    }

    private static Font loadFont(String family, int style, float size) {
        Font f = new Font(family, style, (int) size);
        if (f.getFamily().equals(family)) return f.deriveFont(style, size);
        return new Font(Font.MONOSPACED, style, (int) size).deriveFont(style, size);
    }

    private static Font addUnderline(Font f) {
        Map<TextAttribute, Object> attrs = new HashMap<>(f.getAttributes());
        attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        return f.deriveFont(attrs);
    }

    private static final int PAD = 12; // inner padding
    private static final int ROW_H = 18; // px per member row
    private static final int HEADER_H = 44; // class name area
    private static final int SIDEBAR_W = 260;
    private static final int MINI_W = 180;
    private static final int MINI_H = 110;
    private static final int MINI_MARGIN = 12;

    private static final double GRID_WORLD = 40.0;

    private final List<UMLClass> classes;
    private final Map<UMLClass, Rectangle2D> bounds;
    private UMLClass selected = null;

    private double camX = 0, camY = 0;
    private double zoom = 1.0;

    private int dragStartX, dragStartY;
    private double dragCamX, dragCamY;

    private double targetZoom = 1.0;
    private double targetCamX = 0, targetCamY = 0;

    private long lastTick = System.nanoTime();

    private Canvas canvas;
    private Sidebar sidebar;
    private JFrame frame;

    public UMLRenderer(List<UMLClass> classes, Map<UMLClass, Rectangle2D> bounds) {
        this.classes = classes;
        this.bounds = bounds;
        buildUI();
        fitAll(false);
    }

    private void buildUI() {
        frame = new JFrame("UML Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 820);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(0, 0));

        sidebar = new Sidebar();
        sidebar.setPreferredSize(new Dimension(SIDEBAR_W, 0));

        canvas = new Canvas();
        canvas.setBackground(BG);

        frame.add(canvas, BorderLayout.CENTER);
        frame.add(sidebar, BorderLayout.EAST);

        frame.setVisible(true);
        setupInputHandlers();
        startAnimationTimer();
    }

    private void setupInputHandlers() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragCamX = camX;
                dragCamY = camY;
                canvas.requestFocusInWindow();

                Point2D world = screenToWorld(e.getX(), e.getY());
                UMLClass hit = hitTest(world.getX(), world.getY());
                if (!Objects.equals(hit, selected)) {
                    selected = hit;
                    sidebar.update(selected);
                    canvas.repaint();
                }
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                double dx = (e.getX() - dragStartX) / zoom;
                double dy = (e.getY() - dragStartY) / zoom;
                targetCamX = dragCamX + dx;
                targetCamY = dragCamY + dy;
                camX = targetCamX;
                camY = targetCamY;
                canvas.repaint();
            }
        });

        canvas.addMouseWheelListener(e -> {
            double factor = e.getPreciseWheelRotation() < 0 ? 1.12 : 1.0 / 1.12;
            double newZ = Math.min(4.0, Math.max(0.15, targetZoom * factor));

            Point2D before = screenToWorldAt(e.getX(), e.getY(), targetZoom, targetCamX, targetCamY);
            targetZoom = newZ;
            Point2D after = screenToWorldAt(e.getX(), e.getY(), targetZoom, targetCamX, targetCamY);
            targetCamX += after.getX() - before.getX();
            targetCamY += after.getY() - before.getY();
        });

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F -> fitAll(true);
                    case KeyEvent.VK_R -> targetZoom = 1.0;
                    case KeyEvent.VK_ESCAPE -> {
                        selected = null;
                        sidebar.update((UMLClass) null);
                        canvas.repaint();
                    }
                    case KeyEvent.VK_EQUALS,
                         KeyEvent.VK_PLUS -> targetZoom = Math.min(4.0, targetZoom * 1.15);
                    case KeyEvent.VK_MINUS -> targetZoom = Math.max(0.15, targetZoom / 1.15);
                }
            }
        });
        canvas.setFocusable(true);
    }

    private void startAnimationTimer() {
        Timer t = new Timer(14, e -> {
            long now = System.nanoTime();
            double dt = (now - lastTick) / 1_000_000.0; // ms
            lastTick = now;

            double alpha = 1.0 - Math.pow(1.0 - 0.25, dt / 16.0);

            boolean dirty = false;

            if (Math.abs(targetZoom - zoom) > 0.0001) {
                zoom += (targetZoom - zoom) * alpha;
                dirty = true;
            } else {
                zoom = targetZoom;
            }
            if (Math.abs(targetCamX - camX) > 0.05) {
                camX += (targetCamX - camX) * alpha;
                dirty = true;
            } else {
                camX = targetCamX;
            }
            if (Math.abs(targetCamY - camY) > 0.05) {
                camY += (targetCamY - camY) * alpha;
                dirty = true;
            } else {
                camY = targetCamY;
            }

            if (dirty) canvas.repaint();
        });
        t.start();
    }

    private void fitAll(boolean animate) {
        if (bounds.isEmpty()) return;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
                maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        for (Rectangle2D r : bounds.values()) {
            minX = Math.min(minX, r.getMinX());
            minY = Math.min(minY, r.getMinY());
            maxX = Math.max(maxX, r.getMaxX());
            maxY = Math.max(maxY, r.getMaxY());
        }
        double cw = canvas.getWidth() == 0 ? 1000 : canvas.getWidth();
        double ch = canvas.getHeight() == 0 ? 800 : canvas.getHeight();
        double scaleX = (cw - 80) / (maxX - minX);
        double scaleY = (ch - 80) / (maxY - minY);
        double scale = Math.min(scaleX, scaleY);

        double newZ = Math.min(1.5, Math.max(0.2, scale));
        double newCamX = (cw / 2.0 / newZ) - (minX + (maxX - minX) / 2.0);
        double newCamY = (ch / 2.0 / newZ) - (minY + (maxY - minY) / 2.0);

        if (animate) {
            targetZoom = newZ;
            targetCamX = newCamX;
            targetCamY = newCamY;
        } else {
            zoom = targetZoom = newZ;
            camX = targetCamX = newCamX;
            camY = targetCamY = newCamY;
        }
    }

    private Point2D screenToWorld(double sx, double sy) {
        return screenToWorldAt(sx, sy, zoom, camX, camY);
    }

    private Point2D screenToWorldAt(double sx, double sy, double z, double cx, double cy) {
        return new Point2D.Double(sx / z - cx, sy / z - cy);
    }

    private Point2D worldToScreen(double wx, double wy) {
        return new Point2D.Double((wx + camX) * zoom, (wy + camY) * zoom);
    }

    private UMLClass hitTest(double wx, double wy) {
        for (Map.Entry<UMLClass, Rectangle2D> e : bounds.entrySet()) {
            if (e.getValue().contains(wx, wy)) return e.getKey();
        }
        return null;
    }

    private class Canvas extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            applyHints(g2);

            drawGrid(g2);

            g2.scale(zoom, zoom);
            g2.translate(camX, camY);

            drawEdges(g2);

            for (UMLClass c : classes) {
                Rectangle2D r = bounds.get(c);
                if (r != null) drawNode(g2, c, r);
            }

            g2.translate(-camX, -camY);
            g2.scale(1.0 / zoom, 1.0 / zoom);

            drawMinimap(g2);
            drawHint(g2);
        }

        private void drawGrid(Graphics2D g2) {
            g2.setColor(GRID);

            double cellPx = GRID_WORLD * zoom;

            double originX = (camX % GRID_WORLD) * zoom;
            double originY = (camY % GRID_WORLD) * zoom;

            if (originX < 0) originX += cellPx;
            if (originY < 0) originY += cellPx;

            int w = getWidth(), h = getHeight();
            for (double x = originX; x < w; x += cellPx)
                g2.drawLine((int) x, 0, (int) x, h);
            for (double y = originY; y < h; y += cellPx)
                g2.drawLine(0, (int) y, w, (int) y);
        }

        private void drawEdges(Graphics2D g2) {
            for (UMLClass c : classes) {
                Rectangle2D childR = bounds.get(c);
                if (childR == null) continue;

                if (c.getParentClass() != null) {
                    Rectangle2D parentR = bounds.get(c.getParentClass());
                    if (parentR != null) {
                        Point2D[] pts = UMLLayout.inheritanceEndpoints(parentR, childR);
                        drawInheritanceArrow(g2, pts[1], pts[0], ARROW_INHERIT);
                    }
                }

                for (UMLClass iface : c.getImplementedIfaces()) {
                    Rectangle2D ifaceR = bounds.get(iface);
                    if (ifaceR != null) {
                        Point2D[] pts = UMLLayout.implementsEndpoints(ifaceR, childR);
                        drawImplementsArrow(g2, pts[1], pts[0], ARROW_IMPL);
                    }
                }
            }
        }

        private void drawInheritanceArrow(Graphics2D g2, Point2D from, Point2D to, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) from.getX(), (int) from.getY(),
                    (int) to.getX(), (int) to.getY());
            drawHollowTriangle(g2, from, to, color, 10);
        }

        private void drawImplementsArrow(Graphics2D g2, Point2D from, Point2D to, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{7, 5}, 0f));
            g2.drawLine((int) from.getX(), (int) from.getY(),
                    (int) to.getX(), (int) to.getY());
            g2.setStroke(new BasicStroke(1.2f));
            drawHollowTriangle(g2, from, to, color, 8);
        }

        private void drawHollowTriangle(Graphics2D g2, Point2D from, Point2D to, Color color, int size) {
            double angle = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
            double spread = Math.PI / 6;
            int[] xs = {
                    (int) to.getX(),
                    (int) (to.getX() - size * Math.cos(angle - spread)),
                    (int) (to.getX() - size * Math.cos(angle + spread))
            };
            int[] ys = {
                    (int) to.getY(),
                    (int) (to.getY() - size * Math.sin(angle - spread)),
                    (int) (to.getY() - size * Math.sin(angle + spread))
            };
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(BG);
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(color);
            g2.drawPolygon(xs, ys, 3);
        }

        private void drawNode(Graphics2D g2, UMLClass c, Rectangle2D r) {
            int x = (int) r.getX(), y = (int) r.getY();
            int w = (int) r.getWidth(), h = (int) r.getHeight();
            boolean sel = c.equals(selected);

            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(x + 4, y + 4, w, h, 10, 10);

            g2.setColor(NODE_BG);
            g2.fillRoundRect(x, y, w, h, 10, 10);

            Color hdrColor = switch (c.getKind()) {
                case ABSTRACT_CLASS -> HDR_ABSTRACT;
                case INTERFACE -> HDR_INTERFACE;
                case ENUM -> HDR_ENUM;
                default -> HDR_CLASS;
            };
            g2.setColor(hdrColor);
            g2.fillRect(x, y, w, HEADER_H);
            g2.fillRoundRect(x, y, w, HEADER_H, 10, 10);
            g2.fillRect(x, y + HEADER_H - 10, w, 10);

            Color accent = switch (c.getKind()) {
                case ABSTRACT_CLASS -> ACCENT_ABST;
                case INTERFACE -> ACCENT_IFACE;
                case ENUM -> ACCENT_ENUM;
                default -> ACCENT_CLASS;
            };
            g2.setColor(accent);
            g2.fillRoundRect(x, y, 4, h, 4, 4);

            if (sel) {
                g2.setColor(NODE_SEL);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, w, h, 10, 10);
            } else {
                g2.setColor(NODE_BORDER);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(x, y, w, h, 10, 10);
            }

            if (!c.stereotype().isEmpty()) {
                g2.setFont(FONT_STEREO);
                g2.setColor(TXT_STEREO);
                int sw = g2.getFontMetrics().stringWidth(c.stereotype());
                g2.drawString(c.stereotype(), x + (w - sw) / 2, y + 16);
            }

            g2.setFont(FONT_NAME);
            g2.setColor(TXT_NAME);
            int nw = g2.getFontMetrics().stringWidth(c.getClassName());
            int nameY = c.stereotype().isEmpty() ? y + 28 : y + 32;
            g2.drawString(c.getClassName(), x + (w - nw) / 2, nameY);

            int divY = y + HEADER_H;
            g2.setColor(DIVIDER);
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawLine(x + 4, divY, x + w - 4, divY);

            int rowY = divY + ROW_H;
            for (UMLClass.Member f : c.getFields()) {
                drawMember(g2, f, x + PAD, rowY, w - PAD * 2);
                rowY += ROW_H;
            }

            if (!c.getFields().isEmpty() && !c.getMethods().isEmpty()) {
                g2.setColor(DIVIDER);
                g2.drawLine(x + 4, rowY, x + w - 4, rowY);
            }

            for (UMLClass.Member m : c.getMethods()) {
                drawMember(g2, m, x + PAD, rowY, w - PAD * 2);
                rowY += ROW_H;
            }
        }

        private void drawMember(Graphics2D g2, UMLClass.Member m, int x, int y, int maxW) {
            Color col;
            if (m.isAbstract) col = TXT_ABSTRACT;
            else if (m.visibility == UMLClass.Member.Visibility.PRIVATE) col = TXT_PRIVATE;
            else if (m.isStatic) col = TXT_STATIC;
            else col = TXT_MEMBER;

            Font font = m.isStatic ? FONT_MEMBER_STATIC
                    : m.isAbstract ? FONT_MEMBER_ABSTRACT
                    : FONT_MEMBER;
            g2.setFont(font);
            g2.setColor(col);

            String label = m.displayLabel();
            FontMetrics fm = g2.getFontMetrics();
            while (label.length() > 4 && fm.stringWidth(label + "…") > maxW) {
                label = label.substring(0, label.length() - 1);
            }
            if (!m.displayLabel().equals(label)) label += "…";

            g2.drawString(label, x, y + 13);
        }

        private void drawMinimap(Graphics2D g2) {
            if (bounds.isEmpty()) return;
            int mx = getWidth() - MINI_W - MINI_MARGIN;
            int my = getHeight() - MINI_H - MINI_MARGIN;

            g2.setColor(MINI_BG);
            g2.fillRoundRect(mx, my, MINI_W, MINI_H, 8, 8);
            g2.setColor(NODE_BORDER);
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawRoundRect(mx, my, MINI_W, MINI_H, 8, 8);

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
                    maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (Rectangle2D r : bounds.values()) {
                minX = Math.min(minX, r.getMinX());
                minY = Math.min(minY, r.getMinY());
                maxX = Math.max(maxX, r.getMaxX());
                maxY = Math.max(maxY, r.getMaxY());
            }
            double worldW = maxX - minX, worldH = maxY - minY;
            if (worldW == 0 || worldH == 0) return;

            double scaleM = Math.min((MINI_W - 12.0) / worldW, (MINI_H - 12.0) / worldH);
            double offMX = mx + 6 + (MINI_W - 12 - worldW * scaleM) / 2;
            double offMY = my + 6 + (MINI_H - 12 - worldH * scaleM) / 2;

            for (Map.Entry<UMLClass, Rectangle2D> e : bounds.entrySet()) {
                Rectangle2D r = e.getValue();
                int nx = (int) (offMX + (r.getX() - minX) * scaleM);
                int ny = (int) (offMY + (r.getY() - minY) * scaleM);
                int nw = Math.max(2, (int) (r.getWidth() * scaleM));
                int nh = Math.max(2, (int) (r.getHeight() * scaleM));
                Color accent = switch (e.getKey().getKind()) {
                    case ABSTRACT_CLASS -> ACCENT_ABST;
                    case INTERFACE -> ACCENT_IFACE;
                    case ENUM -> ACCENT_ENUM;
                    default -> ACCENT_CLASS;
                };
                g2.setColor(accent.darker());
                g2.fillRect(nx, ny, nw, nh);
            }

            double cw = getWidth(), ch = getHeight();
            double vx = offMX + (-camX - minX) * scaleM;
            double vy = offMY + (-camY - minY) * scaleM;
            double vw = (cw / zoom) * scaleM;
            double vh = (ch / zoom) * scaleM;
            g2.setColor(MINI_VIEWPORT);
            g2.fillRect((int) vx, (int) vy, (int) vw, (int) vh);
            g2.setColor(new Color(0x3D6FFF));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect((int) vx, (int) vy, (int) vw, (int) vh);
        }

        private void drawHint(Graphics2D g2) {
            String hint = "F fit  +/- zoom  Esc deselect";
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawString(hint, 12, getHeight() - 10);
        }

        private void applyHints(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
    }

    private class Sidebar extends JPanel {
        private final JPanel content;

        Sidebar() {
            setLayout(new BorderLayout());
            setBackground(SIDEBAR_BG);
            setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, NODE_BORDER));

            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(SIDEBAR_BG);
            content.setMaximumSize(new Dimension(SIDEBAR_W, Integer.MAX_VALUE));

            JScrollPane scroll = new JScrollPane(content,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(SIDEBAR_BG);
            scroll.getViewport().setMinimumSize(new Dimension(SIDEBAR_W, 0));
            add(scroll, BorderLayout.CENTER);

            update((UMLClass) null);
        }

        void update(UMLClass c) {
            content.removeAll();
            if (c == null) {
                addEmptyState();
            } else {
                addClassDetail(c);
            }
            content.revalidate();
            content.repaint();
        }

        private void addEmptyState() {
            JLabel lbl = new JLabel("<html><center>Click a class<br>to inspect it</center></html>");
            lbl.setFont(FONT_SIDEBAR_VALUE);
            lbl.setForeground(new Color(0x44506A));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(Box.createVerticalStrut(60));
            content.add(lbl);
        }

        private void addClassDetail(UMLClass c) {
            JPanel hdr = new JPanel();
            hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
            hdr.setBackground(SIDEBAR_HDR);
            hdr.setBorder(new EmptyBorder(16, 16, 14, 16));
            hdr.setAlignmentX(Component.LEFT_ALIGNMENT);
            hdr.setMaximumSize(new Dimension(SIDEBAR_W, Integer.MAX_VALUE));

            Color accent = switch (c.getKind()) {
                case ABSTRACT_CLASS -> ACCENT_ABST;
                case INTERFACE -> ACCENT_IFACE;
                case ENUM -> ACCENT_ENUM;
                default -> ACCENT_CLASS;
            };

            JLabel kindLbl = new JLabel(c.stereotype().isEmpty() ? "class" : c.stereotype());
            kindLbl.setFont(FONT_SIDEBAR_LABEL);
            kindLbl.setForeground(accent);
            kindLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel nameLbl = new JLabel(c.getClassName());
            nameLbl.setFont(FONT_SIDEBAR_HEADER);
            nameLbl.setForeground(TXT_NAME);
            nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            hdr.add(kindLbl);
            hdr.add(Box.createVerticalStrut(4));
            hdr.add(nameLbl);
            content.add(hdr);

            if (c.getParentClass() != null) {
                addSection("Extends");
                addRow("↑ " + c.getParentClass().getClassName(), ACCENT_CLASS);
            }

            if (!c.getImplementedIfaces().isEmpty()) {
                addSection("Implements");
                for (UMLClass iface : c.getImplementedIfaces())
                    addRow("◇ " + iface.getClassName(), ACCENT_IFACE);
            }

            if (!c.getSubclasses().isEmpty()) {
                addSection("Subclasses");
                for (UMLClass sub : c.getSubclasses())
                    addRow("↓ " + sub.getClassName(), TXT_MEMBER);
            }

            if (!c.getFields().isEmpty()) {
                addSection("Fields");
                for (UMLClass.Member f : c.getFields())
                    addMemberRow(f);
            }

            if (!c.getMethods().isEmpty()) {
                addSection("Methods");
                for (UMLClass.Member m : c.getMethods())
                    addMemberRow(m);
            }

            content.add(Box.createVerticalGlue());
        }

        private void addSection(String title) {
            JLabel lbl = new JLabel(title.toUpperCase());
            lbl.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
            lbl.setForeground(new Color(0x44506A));
            lbl.setBorder(new EmptyBorder(12, 16, 4, 16));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setMaximumSize(new Dimension(SIDEBAR_W, 30));
            content.add(lbl);
        }

        private void addRow(String text, Color color) {
            JLabel lbl = new JLabel(text);
            lbl.setFont(FONT_SIDEBAR_VALUE);
            lbl.setForeground(color);
            lbl.setBorder(new EmptyBorder(2, 20, 2, 16));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setMaximumSize(new Dimension(SIDEBAR_W, 28));
            content.add(lbl);
        }

        private void addMemberRow(UMLClass.Member m) {
            Color col = m.isAbstract ? TXT_ABSTRACT
                    : m.visibility == UMLClass.Member.Visibility.PRIVATE ? TXT_PRIVATE
                    : m.isStatic ? TXT_STATIC
                    : TXT_MEMBER;

            int innerW = SIDEBAR_W - 36; // left indent 20 + right pad 16
            String html = "<html><div style='width:" + innerW + "px'>"
                    + "<tt>" + escapeHtml(m.displayLabel()) + "</tt>"
                    + "</div></html>";

            JLabel lbl = new JLabel(html);
            lbl.setFont(FONT_SIDEBAR_VALUE);
            lbl.setForeground(col);
            lbl.setBorder(new EmptyBorder(1, 20, 1, 16));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setMaximumSize(new Dimension(SIDEBAR_W, Integer.MAX_VALUE));
            content.add(lbl);
        }

        private String escapeHtml(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    public static UMLLayout.NodeSizer nodeSizer() {
        return new UMLLayout.NodeSizer() {
            private final FontMetrics fm;

            {
                Canvas dummy = new UMLRenderer(List.of(), Map.of()).canvas;
                fm = dummy.getFontMetrics(FONT_MEMBER);
            }

            @Override
            public int width(UMLClass c) {
                int max = fm.stringWidth(c.getClassName()) + 20;
                for (UMLClass.Member f : c.getFields())
                    max = Math.max(max, fm.stringWidth(f.displayLabel()));
                for (UMLClass.Member m : c.getMethods())
                    max = Math.max(max, fm.stringWidth(m.displayLabel()));
                return max + PAD * 2 + 16;
            }

            @Override
            public int height(UMLClass c) {
                int rows = c.getFields().size() + c.getMethods().size();
                boolean both = !c.getFields().isEmpty() && !c.getMethods().isEmpty();
                return HEADER_H + rows * ROW_H + (both ? ROW_H : 0) + PAD;
            }
        };
    }
}