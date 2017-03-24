/*
 * ******************************************************************************
 *  * Copyright (c) 2017 Pablo Pavon-Marino.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the GNU Lesser Public License v3.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/lgpl.html
 *  *
 *  * Contributors:
 *  *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *  *     Pablo Pavon-Marino - from version 0.4.0 onwards
 *  *     Pablo Pavon Marino - Jorge San Emeterio Villalain, from version 0.4.1 onwards
 *  *****************************************************************************
 */

package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import com.sun.istack.internal.NotNull;
import edu.uci.ics.jung.visualization.FourPassImageShaper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
public class NetPlanControl
{
    private NetPlan netPlan;
    private boolean isNetPlanEditable;

    private ITableRowFilter tableRowFilter;

    private boolean whatIfAnalysisState;

    private static Map<Triple<URL, Integer, Color>, Pair<ImageIcon, Shape>> databaseOfAlreadyReadIcons = new HashMap<>(); // for each url, height, and border color, an image

    NetPlanControl(@NotNull final NetPlan netPlan)
    {
        this.netPlan = netPlan;

        this.tableRowFilter = null;
        this.whatIfAnalysisState = false;
    }

    public NetPlan getNetPlan()
    {
        return netPlan;
    }

    public void setNetPlan(NetPlan netPlan)
    {
        this.netPlan = netPlan;
    }

    public boolean isNetPlanEditable()
    {
        return isNetPlanEditable;
    }

    public void setNetPlanEditable(boolean netPlanEditable)
    {
        isNetPlanEditable = netPlanEditable;
    }

    public ITableRowFilter getTableRowFilter()
    {
        return tableRowFilter;
    }

    public void updateTableRowFilter(ITableRowFilter tableRowFilterToApply)
    {
        if (tableRowFilterToApply == null)
        {
            this.tableRowFilter = null;
            return;
        }
        if (this.tableRowFilter == null)
        {
            this.tableRowFilter = tableRowFilterToApply;
            return;
        }
        this.tableRowFilter.recomputeApplyingShowIf_ThisAndThat(tableRowFilterToApply);
    }

    public boolean isWhatIfAnalysisOn()
    {
        return whatIfAnalysisState;
    }

    public void setWhatIfAnalysisState(boolean state)
    {
        this.whatIfAnalysisState = state;
    }

    public static Pair<ImageIcon, Shape> getIcon(URL url, int height, Color borderColor)
    {
        final Pair<ImageIcon, Shape> iconShapeInfo = databaseOfAlreadyReadIcons.get(Triple.of(url, height, borderColor));
        if (iconShapeInfo != null) return iconShapeInfo;
        if (url == null)
        {
            BufferedImage img = ImageUtils.createCircle(height, (Color) VisualizationConstants.DEFAULT_GUINODE_COLOR);
            if (img.getHeight() != height) throw new RuntimeException();
            final Shape shapeNoBorder = FourPassImageShaper.getShape(img);
            if (borderColor.getAlpha() != 0)
                img = ImageUtils.addBorder(img, VisualizationConstants.DEFAULT_ICONBORDERSIZEINPIXELS, borderColor);
            final ImageIcon icon = new ImageIcon(img);
            final Pair<ImageIcon, Shape> res = Pair.of(icon, shapeNoBorder);
            databaseOfAlreadyReadIcons.put(Triple.of(null, icon.getIconHeight(), borderColor), res);
            return res;
        }
        try
        {
    		/* Read the base buffered image */
            BufferedImage img = ImageIO.read(url);
            if (img.getHeight() != height)
                img = ImageUtils.resize(img, (int) (img.getWidth() * height / (double) img.getHeight()), height);
            if (img.getHeight() != height) throw new RuntimeException();
            final Shape shapeNoBorder = FourPassImageShaper.getShape(img);
            if (borderColor.getAlpha() != 0)
                img = ImageUtils.addBorder(img, VisualizationConstants.DEFAULT_ICONBORDERSIZEINPIXELS, borderColor);
            final ImageIcon icon = new ImageIcon(img);
            final AffineTransform translateTransform = AffineTransform.getTranslateInstance(-icon.getIconWidth() / 2, -icon.getIconHeight() / 2);
            final Pair<ImageIcon, Shape> res = Pair.of(icon, translateTransform.createTransformedShape(shapeNoBorder));
            databaseOfAlreadyReadIcons.put(Triple.of(url, icon.getIconHeight(), borderColor), res);
            return res;
        } catch (Exception e)
        {
            System.out.println("URL: **" + url + "**");
            System.out.println(url);
			/* Use the default image, whose URL is the one given */
            e.printStackTrace();
            return getIcon(null, height, borderColor);
        }
    }
}
