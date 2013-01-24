/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.viz.stats.ui;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.stats.data.DataPoint;
import com.raytheon.uf.common.stats.data.GraphData;
import com.raytheon.uf.common.stats.data.StatsData;
import com.raytheon.uf.common.stats.util.DataViewUtils;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.stats.display.ScaleManager;

/**
 * Statistics graph canvas.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 3, 2012     728     mpduff      Initial creation
 *
 * </pre>
 *
 * @author mpduff
 * @version 1.0
 */

public class StatsDisplayCanvas extends Canvas {
    /** Date Format object for graph title */
    private final ThreadLocal<SimpleDateFormat> titleDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sTemp = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            sTemp.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sTemp;
        }
    };

    /** Date Format object for x axis */
    private final ThreadLocal<SimpleDateFormat> axisFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sTemp = new SimpleDateFormat("MM/dd/yyyy");
            sTemp.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sTemp;
        }
    };

    /** Decimal Format object */
    private final ThreadLocal<DecimalFormat> numFormat = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            DecimalFormat tmp = new DecimalFormat("####.##");
            return tmp;
        }
    };

    /** Constant */
    private final String COLON = ":";

    /** Constant */
    private final String ZERO = "0";

    /** Constant */
    private final String COUNT = "count";

    /** Canvas initialized flag */
    private boolean initialized = false;

    /** Constant */
    private final String MINUTE_00 = "00";

    private final int canvasWidth = 800;

    private final int canvasHeight = 575;

    private final int GRAPH_BORDER = 75;

    private final int GRAPH_WIDTH = canvasWidth - GRAPH_BORDER * 2;

    private final int GRAPH_HEIGHT = canvasHeight - GRAPH_BORDER * 2;

    /** Y Axis dimensions */
    private final int[] yAxis = new int[] { GRAPH_BORDER, GRAPH_BORDER,
            GRAPH_BORDER, GRAPH_HEIGHT + GRAPH_BORDER };

    /** X Axis dimensions */
    private final int[] xAxis = new int[] { GRAPH_BORDER,
            GRAPH_HEIGHT + GRAPH_BORDER, GRAPH_WIDTH + GRAPH_BORDER,
            GRAPH_HEIGHT + GRAPH_BORDER };

    /** Top border dimensions */
    private final int[] borderTop = new int[] { GRAPH_BORDER, GRAPH_BORDER,
            GRAPH_BORDER + GRAPH_WIDTH, GRAPH_BORDER };

    /** Right border dimensions */
    private final int[] borderRight = new int[] { GRAPH_BORDER + GRAPH_WIDTH,
            GRAPH_BORDER, GRAPH_BORDER + GRAPH_WIDTH,
            GRAPH_BORDER + GRAPH_HEIGHT };

    /** Canvas center */
    private final int center = canvasWidth / 2;

    /** Canvas font */
    private Font canvasFont;

    /** Parent Composite */
    private final Composite parentComp;

    /** Font height in pixels */
    private int fontHeight;

    /** Font width in pixels */
    private int fontAveWidth;

    /** Scale value manager */
    private ScaleManager scalingManager;

    /** The graph title */
    private String graphTitle;

    /** The secondary graph title */
    private final String graphTitle2;

    /** Millis per pixel in the X direction */
    private long millisPerPixelX;

    /** Callback */
    private final IStatsDisplay callback;

    /** Map of Rectangle objects */
    private final Map<String, List<Rectangle>> rectangleMap = new HashMap<String, List<Rectangle>>();

    /** Tooltip shell */
    private Shell tooltip;

    /** Graph Data object */
    private GraphData graphData;

    /** Smallest value in the data set */
    private double minValue;

    /** Largest value in the data set */
    private double maxValue;

    private String view = DataViewUtils.DataView.AVG.getView(); // Defaults to
                                                                // average

    /**
     * Constructor
     *
     * @param parent
     *            Parent composite
     * @param callback
     *            Callback to the parent
     * @param graphTitle
     *            The graph title
     */
    public StatsDisplayCanvas(Composite parent, IStatsDisplay callback,
            String graphTitle) {
        super(parent, SWT.DOUBLE_BUFFERED);
        this.parentComp = parent;
        this.callback = callback;
        this.graphTitle = graphTitle;

        this.graphData = callback.getGraphData();
        TimeRange tr = graphData.getTimeRange();
        String start = titleDateFormat.get().format(tr.getStart());
        String end = titleDateFormat.get().format(tr.getEnd());

        this.graphTitle = graphTitle;
        this.graphTitle2 = start + "Z - " + end + "Z";

        setupCanvas();
    }

    /**
     * Initialize the canvas.
     */
    private void setupCanvas() {
        canvasFont = new Font(parentComp.getDisplay(), "Monospace", 9,
                SWT.NORMAL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = canvasHeight;
        gd.widthHint = canvasWidth;
        setLayoutData(gd);

        addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                drawCanvas(e.gc);
            }
        });

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if ((canvasFont != null) && (!canvasFont.isDisposed())) {
                    canvasFont.dispose();
                }
            }
        });

        addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                handleMouseMoveEvent(e);
            }
        });
    }

    /**
     * Initialize drawing settings.
     *
     * @param gc
     *            The Graphics Context
     */
    private void init(GC gc) {
        if (!this.initialized) {
            initialized = true;
            gc.setAntialias(SWT.ON);

            gc.setFont(canvasFont);

            fontHeight = (gc.getFontMetrics().getHeight());
            fontAveWidth = gc.getFontMetrics().getAverageCharWidth();
        }
    }

    /**
     * Draw on the canvas.
     *
     * @param gc
     *            The Graphics Context
     */
    protected void drawCanvas(GC gc) {
        init(gc);
        gc.setBackground(parentComp.getDisplay()
                .getSystemColor(SWT.COLOR_WHITE));
        gc.fillRectangle(0, 0, canvasWidth, canvasHeight + 2);

        RGB color = RGBColors.getRGBColor("gray85");
        gc.setBackground(new Color(parentComp.getDisplay(), color.red,
                color.green, color.blue));
        Rectangle graphArea = new Rectangle(GRAPH_BORDER, GRAPH_BORDER,
                GRAPH_WIDTH, GRAPH_HEIGHT);
        gc.fillRectangle(graphArea);
        gc.setBackground(parentComp.getDisplay()
                .getSystemColor(SWT.COLOR_WHITE));

        gc.setForeground(parentComp.getDisplay()
                .getSystemColor(SWT.COLOR_BLACK));

        gc.drawPolygon(borderTop);

        gc.drawPolygon(borderRight);

        // Draw the graph title
        int titleLength = graphTitle.length() * fontAveWidth;
        int title2Length = graphTitle2.length() * fontAveWidth;
        int titleX = center - titleLength / 2;
        int titleY = GRAPH_BORDER / 2 - fontHeight;
        gc.drawText(graphTitle, titleX, titleY);

        titleX = center - title2Length / 2;
        titleY = GRAPH_BORDER / 2 + 5;
        gc.drawText(graphTitle2, titleX, titleY);

        drawXAxis(gc);
        drawYAxis(gc);

        drawData(gc);

        drawYAxisLabel(gc);
    }

    /**
     * Draw the X axis.
     *
     * @param gc
     *            The Graphics Context
     */
    private void drawXAxis(GC gc) {
        // Draw the xAxis line
        gc.drawPolyline(xAxis);

        // List of locations for the date labels
        List<Integer> dateLocationList = new ArrayList<Integer>();
        dateLocationList.add(GRAPH_BORDER); // first one

        List<Date> dateList = new ArrayList<Date>();

        SimpleDateFormat sdf = axisFormat.get();

        TimeRange tr = graphData.getTimeRange();
        dateList.add(tr.getStart()); // Add the first date

        long milliRange = tr.getDuration();
        long numHours = milliRange / TimeUtil.MILLIS_PER_HOUR;

        millisPerPixelX = milliRange / GRAPH_WIDTH;

        boolean showLine = true;
        int height = 15;
        long startMillis = tr.getStart().getTime();
        StringBuilder buffer = new StringBuilder();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        for (long i = tr.getStart().getTime(); i <= tr.getEnd().getTime(); i += TimeUtil.MILLIS_PER_HOUR) {
            cal.setTimeInMillis(i);
            int[] tickArray = {
                    (int) (GRAPH_BORDER + (i - startMillis) / millisPerPixelX),
                    GRAPH_BORDER + GRAPH_HEIGHT,
                    (int) (GRAPH_BORDER + (i - startMillis) / millisPerPixelX),
                    GRAPH_BORDER + GRAPH_HEIGHT + height };
            if (cal.get(Calendar.HOUR_OF_DAY) == 0) {
                gc.setLineWidth(3);
            } else {
                gc.setLineWidth(1);
            }
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            if ((numHours / 24 <= 7) || (hour % 6) == 0) {
                gc.drawPolyline(tickArray);

                if (callback.drawGridLines()) {
                    boolean draw = false;
                    if (numHours <= 6) {
                        draw = true;
                    } else if (numHours == 12) {
                        if (hour % 2 == 0) {
                            draw = true;
                        }
                    }

                    if (draw) {
                        int[] gridLine = new int[] {
                                (int) (GRAPH_BORDER + (i - startMillis)
                                        / millisPerPixelX),
                                GRAPH_BORDER + GRAPH_HEIGHT,
                                (int) (GRAPH_BORDER + (i - startMillis)
                                        / millisPerPixelX), GRAPH_BORDER };
                        gc.drawPolyline(gridLine);
                    }
                }
            }

            if (hour == 0) {
                dateLocationList.add((int) (GRAPH_BORDER + (i - startMillis)
                        / millisPerPixelX));
                if (!dateList.contains(cal.getTime())) {
                    dateList.add(cal.getTime());
                }
            }
            buffer.setLength(0); // Clear the buffer
            int y = GRAPH_BORDER + GRAPH_HEIGHT + 20;
            int hr = cal.get(Calendar.HOUR_OF_DAY);
            if ((numHours <= 24) || (hour % 6 == 0 && numHours <= 168)) {
                if (numHours <= 3) {
                    for (int j = 0; j < 60; j += 15) {
                        buffer.setLength(0);
                        int x = (int) (GRAPH_BORDER + (i - startMillis + j
                                * TimeUtil.MILLIS_PER_MINUTE)
                                / millisPerPixelX);
                        if (numHours == 1
                                || (numHours == 3 && (j == 0 || j == 30))) {
                            String hrStr = (hr < 10) ? ZERO.concat(String
                                    .valueOf(hr)) : String.valueOf(hr);
                            if (j == 0) {
                                buffer.append(hrStr).append(COLON)
                                        .append(MINUTE_00);
                            } else {
                                buffer.append(hrStr).append(COLON).append(j);
                            }
                            int adjustment = buffer.length() * fontAveWidth / 2
                                    - 1;
                            gc.drawText(buffer.toString(), x - adjustment, y);

                            if (callback.drawGridLines()) {
                                int[] gridLineArray = {
                                        (int) (GRAPH_BORDER + (i - startMillis + TimeUtil.MILLIS_PER_MINUTE
                                                * j)
                                                / millisPerPixelX),
                                        GRAPH_BORDER + GRAPH_HEIGHT,
                                        (int) (GRAPH_BORDER + (i - startMillis + TimeUtil.MILLIS_PER_MINUTE
                                                * j)
                                                / millisPerPixelX),
                                        GRAPH_BORDER };
                                gc.drawPolyline(gridLineArray);
                            }
                        }
                        int[] minorTickArray = {
                                (int) (GRAPH_BORDER + (i - startMillis + TimeUtil.MILLIS_PER_MINUTE
                                        * j)
                                        / millisPerPixelX),
                                GRAPH_BORDER + GRAPH_HEIGHT,
                                (int) (GRAPH_BORDER + (i - startMillis + TimeUtil.MILLIS_PER_MINUTE
                                        * j)
                                        / millisPerPixelX),
                                GRAPH_BORDER + GRAPH_HEIGHT + height - 5 };
                        gc.drawPolyline(minorTickArray);
                    }
                } else {
                    int x = (int) (GRAPH_BORDER + (i - startMillis)
                            / millisPerPixelX);
                    String hrStr = (hr < 10) ? ZERO.concat(String.valueOf(hr))
                            : String.valueOf(hr);
                    buffer.append(hrStr);
                    int adjustment = buffer.length() * fontAveWidth / 2 - 1;
                    gc.drawText(buffer.toString(), x - adjustment, y);

                    if (callback.drawGridLines()) {
                        if ((numHours == 24 && hr % 6 == 0)
                                || (numHours == 168 && hr == 0)) {

                            int[] gridLineArray = {
                                    (int) (GRAPH_BORDER + (i - startMillis)
                                            / millisPerPixelX),
                                    GRAPH_BORDER + GRAPH_HEIGHT,
                                    (int) (GRAPH_BORDER + (i - startMillis)
                                            / millisPerPixelX), GRAPH_BORDER };
                            gc.setLineWidth(1);
                            gc.drawPolyline(gridLineArray);
                            gc.setLineWidth(3);
                        }
                    }
                }
            } else if (numHours == 336 && hour == 0) {
                int x = (int) (GRAPH_BORDER + (i - startMillis)
                        / millisPerPixelX);
                buffer.append(cal.get(Calendar.MONTH) + 1);
                buffer.append("/");
                buffer.append(cal.get(Calendar.DAY_OF_MONTH));
                int adjustment = buffer.length() * fontAveWidth / 2 - 1;
                gc.drawText(buffer.toString(), x - adjustment, y);

                if (callback.drawGridLines()) {
                    // show every other line
                    if (showLine) {
                        int[] gridLineArray = {
                                (int) (GRAPH_BORDER + (i - startMillis)
                                        / millisPerPixelX),
                                GRAPH_BORDER + GRAPH_HEIGHT,
                                (int) (GRAPH_BORDER + (i - startMillis)
                                        / millisPerPixelX), GRAPH_BORDER };
                        gc.setLineWidth(1);
                        gc.drawPolyline(gridLineArray);
                        gc.setLineWidth(3);
                    }
                    showLine = !showLine;
                }

            } else if (numHours == 720) {
                if (cal.get(Calendar.DAY_OF_MONTH) % 2 == 0 && hour == 0) {
                    int x = (int) (GRAPH_BORDER + (i - startMillis)
                            / millisPerPixelX);
                    buffer.append(cal.get(Calendar.MONTH) + 1);
                    buffer.append("/");
                    buffer.append(cal.get(Calendar.DAY_OF_MONTH));
                    int adjustment = buffer.length() * fontAveWidth / 2 - 1;
                    gc.drawText(buffer.toString(), x - adjustment, y);
                    if (callback.drawGridLines()) {
                        if (showLine) {
                            int[] gridLineArray = {
                                    (int) (GRAPH_BORDER + (i - startMillis)
                                            / millisPerPixelX),
                                    GRAPH_BORDER + GRAPH_HEIGHT,
                                    (int) (GRAPH_BORDER + (i - startMillis)
                                            / millisPerPixelX), GRAPH_BORDER };
                            gc.setLineWidth(1);
                            gc.drawPolyline(gridLineArray);
                            gc.setLineWidth(3);
                        }
                        showLine = !showLine;
                    }
                }
            }
        }

        dateLocationList.add(GRAPH_BORDER + GRAPH_WIDTH); // last one

        int idx = 0;
        if (dateLocationList.get(0) == dateLocationList.get(1)) {
            dateLocationList.remove(0);
        }

        // Lower Date label
        if (numHours < 336) {
            for (Date date : dateList) {
                // only one date
                String dateStr = sdf.format(date);
                int loc1 = dateLocationList.get(idx);
                int loc2 = dateLocationList.get(++idx);
                if (loc2 - loc1 > dateStr.length() * fontAveWidth) {
                    int loc = ((loc2 + loc1) / 2)
                            - (((dateStr.length() * fontAveWidth) / 2) + 1);
                    if (loc < GRAPH_BORDER + GRAPH_WIDTH) {
                        gc.drawText(dateStr, loc, GRAPH_BORDER + GRAPH_HEIGHT
                                + 40);
                    }
                }
            }
        }
    }

    /**
     * Draw the Y axis.
     *
     * @param gc
     *            The Graphics Context
     */
    private void drawYAxis(GC gc) {
        DecimalFormat format = numFormat.get();

        gc.setLineWidth(1);

        gc.drawPolyline(yAxis);

        Map<String, RGB> groupSettings = callback.getGroupSettings();

        double minVal = graphData.getMinValue(groupSettings.keySet(), view);
        double maxVal = graphData.getMaxValue(groupSettings.keySet(), view);
        int numberTicks = 4;
        double inc = 5;
        double minScaleVal = 0;
        double maxScaleVal = 10;
        scalingManager = new ScaleManager(minVal, maxVal);
        numberTicks = scalingManager.getMajorTickCount();
        inc = scalingManager.getMajorTickIncrement();

        minScaleVal = scalingManager.getMinScaleValue();
        maxScaleVal = scalingManager.getMaxScaleValue();

        double yVal = minScaleVal;

        // Draw the axis tick marks
        for (int i = 0; i < numberTicks; i++) {
            int yPix = y2pixel(minScaleVal, maxScaleVal, yVal);
            int[] tick = { GRAPH_BORDER, yPix, GRAPH_BORDER - 10, yPix };
            gc.drawPolyline(tick);

            String label = format.format(yVal);
            int labelX = GRAPH_BORDER - (label.length() * fontAveWidth) - 20;
            int labelY = yPix - (fontHeight / 2);
            gc.drawText(label, labelX, labelY);
            yVal += inc;

            // Draw gridline if needed
            if (callback.drawGridLines()) {
                int[] gridLine = new int[] { GRAPH_BORDER, yPix,
                        GRAPH_BORDER + GRAPH_WIDTH, yPix };
                gc.drawPolyline(gridLine);
            }
        }
    }

    /**
     * Draw the YAxis label.
     *
     * @param gc
     *            The Graphics Context
     */
    private void drawYAxisLabel(GC gc) {
        String unit = this.graphData.getDisplayUnit();
        StringBuilder yAxisLabel = new StringBuilder(graphTitle);

        if (unit != null && !unit.equalsIgnoreCase(COUNT) && unit.length() > 0
                && !view.equals(DataViewUtils.DataView.COUNT.getView())) {
            yAxisLabel.append(" (").append(unit).append(")");
        }

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));

        // Rotate the Y header text.
        int labelInPixels = gc.getFontMetrics().getAverageCharWidth()
                * yAxisLabel.length();

        int yCoord = (canvasHeight / 2) + (labelInPixels / 2);

        Transform t = new Transform(gc.getDevice());
        t.translate(2, yCoord); // new origin
        t.rotate(-90f);
        gc.setTransform(t);
        gc.drawString(yAxisLabel.toString(), 0, 0, true);

        t.dispose();
    }

    /**
     * Draw the data on the canvas.
     *
     * @param gc
     *            The Graphics Context
     */
    private void drawData(GC gc) {
        double maxScaleVal = scalingManager.getMaxScaleValue();
        double minScaleVal = scalingManager.getMinScaleValue();
        Map<String, RGB> groupSettings = callback.getGroupSettings();

        for (String key : graphData.getKeysWithData()) {
            if (groupSettings.containsKey(key)) {
                Color color = new Color(getDisplay(), groupSettings.get(key));
                gc.setForeground(color);
                gc.setBackground(color);
                if (groupSettings.containsKey(key)) {

                    List<Integer> pointList = new ArrayList<Integer>();
                    if (!rectangleMap.containsKey(key)) {
                        rectangleMap.put(key, new ArrayList<Rectangle>());
                    }
                    rectangleMap.get(key).clear();

                    StatsData data = graphData.getStatsData(key);

                    if (data != null) {
                        // Loop over all group members
                        List<DataPoint> dataList = data.getData();
                        long startMillis = graphData.getTimeRange().getStart()
                                .getTime();
                        int lastXpix = -999;
                        int lastYpix = -999;
                        for (DataPoint point : dataList) {
                            long x = point.getX();
                            double y;

                            if (view.equals(DataViewUtils.DataView.AVG
                                    .getView())) {
                                y = point.getAvg();
                            } else if (view.equals(DataViewUtils.DataView.MIN
                                    .getView())) {
                                y = point.getMin();
                            } else if (view.equals(DataViewUtils.DataView.MAX
                                    .getView())) {
                                y = point.getMax();
                            } else if (view.equals(DataViewUtils.DataView.SUM
                                    .getView())) {
                                y = point.getSum();
                            } else {
                                y = point.getCount();
                            }
                            int yPix = y2pixel(minScaleVal, maxScaleVal, y);
                            int xPix = (int) ((x - startMillis)
                                    / millisPerPixelX + GRAPH_BORDER);

                            if (xPix > GRAPH_BORDER + GRAPH_WIDTH) {
                                break;
                            }
                            pointList.add(xPix);
                            pointList.add(yPix);
                            Rectangle rect = new Rectangle(xPix - 3, yPix - 3,
                                    6, 6);
                            rectangleMap.get(key).add(rect);
                            if (lastXpix != -999) {
                                if (callback.drawDataLines()) {
                                    gc.drawLine(lastXpix, lastYpix, xPix, yPix);
                                }
                            }
                            lastXpix = xPix;
                            lastYpix = yPix;
                        }

                        for (int i = 0; i < rectangleMap.get(key).size(); i++) {
                            Rectangle rect = rectangleMap.get(key).get(i);
                            gc.setForeground(color);

                            gc.fillRectangle(rect);
                            gc.setForeground(getDisplay().getSystemColor(
                                    SWT.COLOR_BLACK));
                            gc.drawRectangle(rect);
                        }
                    }
                }
            }
        }
    }

    /**
     * Y Value to pixel conversion.
     *
     * @param yMin
     *            The smallest y value
     * @param yMax
     *            The largest y value
     * @param y
     *            The y value
     * @return the pixel corresponding to the y value
     */
    private int y2pixel(double yMin, double yMax, double y) {
        double yDiff = yMax - yMin;
        double yValue = (GRAPH_HEIGHT / yDiff) * (y - yMin);
        return (int) (GRAPH_HEIGHT - Math.round(yValue) + GRAPH_BORDER);
    }

    /**
     * Mouse move event hanler.
     *
     * @param e
     *            MouseEvent object
     */
    private void handleMouseMoveEvent(MouseEvent e) {
        int x = e.x;
        int y = e.y;
        final String colon = ": ";
        final String nl = "\n";
        StringBuilder sb = new StringBuilder();
        GraphData graphData = callback.getGraphData();
        if (graphData == null) {
            return;
        }
        for (String key : graphData.getKeys()) {
            int idx = 0;
            if (rectangleMap.containsKey(key)) {
                for (Rectangle rect : rectangleMap.get(key)) {
                    if (callback.getGroupSettings().containsKey(key)) {
                        // if true then data are on the graph
                        if (rect.contains(x, y)) {
                            if (sb.length() > 0) {
                                sb.append(nl);
                            }
                            sb.append(key).append(colon);
                            DataPoint point = graphData.getStatsData(key)
                                    .getData().get(idx);
                            sb.append(point.getSampleText(view));
                        }
                    }
                    idx++;
                }
            }
        }

        Rectangle bounds = this.getBounds();
        Point pos = getShell().toDisplay(bounds.x + x + 15, bounds.y + y + 15);
        if (sb.length() > 0) {
            showTooltip(getShell(), pos.x, pos.y, sb.toString());
        } else {
            if (tooltip != null && !tooltip.isDisposed()) {
                this.tooltip.dispose();
            }
        }
    }

    /**
     * Show the "tooltip" mouseover.
     *
     * @param parent
     * @param x
     * @param y
     * @return
     */
    private Shell showTooltip(Shell parent, int x, int y, String text) {
        if (tooltip != null && !tooltip.isDisposed()) {
            tooltip.dispose();
        }
        tooltip = new Shell(parent, SWT.TOOL | SWT.ON_TOP);
        tooltip.setLayout(new GridLayout());

        tooltip.setBackground(tooltip.getDisplay().getSystemColor(
                SWT.COLOR_INFO_BACKGROUND));
        tooltip.setBackgroundMode(SWT.INHERIT_FORCE);

        Label lbContent = new Label(tooltip, SWT.NONE);
        lbContent.setText(text);

        Point lbContentSize = lbContent.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        int width = lbContentSize.x + 10;
        int height = lbContentSize.y + 10;

        tooltip.setBounds(x, y, width, height);
        tooltip.setVisible(true);
        return tooltip;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if (this.canvasFont != null && !canvasFont.isDisposed()) {
            this.canvasFont.dispose();
        }
        super.dispose();
    }

    /**
     * Set the graph data.
     *
     * @param graphData
     *            The GraphData object
     */
    public void setGraphData(GraphData graphData) {
        this.graphData = graphData;
    }

    /**
     * Set the view type.
     * @param view The view type
     */
    public void setView(String view) {
        this.view = view;
    }
}
