/*
 * ******************************************************************************
 *  * Copyright (c) 2017 Pablo Pavon-Marino.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the GNU Lesser License v3.0
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

import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.sun.istack.internal.NotNull;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
class CanvasController
{
    private VisualizationMediator mediator;

    private boolean showNodeNames;
    private boolean showLinkLabels;
    private boolean showLinksInNonActiveLayer;
    private boolean showInterLayerLinks;
    private boolean showLowerLayerPropagation;
    private boolean showUpperLayerPropagation;
    private boolean showLayerPropagation;
    private boolean showNonConnectedNodes;

    CanvasController(@NotNull final VisualizationMediator mediator)
    {
        this.mediator = mediator;

        this.showNodeNames = false;
        this.showLinkLabels = false;
        this.showLinksInNonActiveLayer = true;
        this.showInterLayerLinks = true;
        this.showNonConnectedNodes = true;
        this.showLowerLayerPropagation = true;
        this.showUpperLayerPropagation = true;
        this.showLayerPropagation = true;
    }

    boolean isVisibleInCanvas(GUINode gn)
    {
        final Node n = gn.getAssociatedNode();
        if (nodesToHideInCanvasAsMandatedByUserInTable.contains(n)) return false;
        if (!showInCanvasNonConnectedNodes)
        {
            final NetworkLayer layer = gn.getLayer();
            if (n.getOutgoingLinks(layer).isEmpty() && n.getIncomingLinks(layer).isEmpty()
                    && n.getOutgoingDemands(layer).isEmpty() && n.getIncomingDemands(layer).isEmpty()
                    && n.getOutgoingMulticastDemands(layer).isEmpty() && n.getIncomingMulticastDemands(layer).isEmpty())
                return false;
        }
        return true;
    }

    boolean isVisibleInCanvas(GUILink gl)
    {
        if (gl.isIntraNodeLink())
        {
            final Node node = gl.getOriginNode().getAssociatedNode();
            final NetworkLayer originLayer = gl.getOriginNode().getLayer();
            final NetworkLayer destinationLayer = gl.getDestinationNode().getLayer();
            final int originIndexInVisualization = getCanvasVisualizationOrderRemovingNonVisible(originLayer);
            final int destinationIndexInVisualization = getCanvasVisualizationOrderRemovingNonVisible(destinationLayer);
            final int lowerVIndex = originIndexInVisualization < destinationIndexInVisualization ? originIndexInVisualization : destinationIndexInVisualization;
            final int upperVIndex = originIndexInVisualization > destinationIndexInVisualization ? originIndexInVisualization : destinationIndexInVisualization;
            cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(gl.getOriginNode());
            boolean atLeastOneLowerLayerVisible = false;
            for (int vIndex = 0; vIndex <= lowerVIndex; vIndex++)
                if (isVisibleInCanvas(getCanvasAssociatedGUINode(node, getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(vIndex))))
                {
                    atLeastOneLowerLayerVisible = true;
                    break;
                }
            if (!atLeastOneLowerLayerVisible) return false;
            boolean atLeastOneUpperLayerVisible = false;
            for (int vIndex = upperVIndex; vIndex < getCanvasNumberOfVisibleLayers(); vIndex++)
                if (isVisibleInCanvas(getCanvasAssociatedGUINode(node, getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(vIndex))))
                {
                    atLeastOneUpperLayerVisible = true;
                    break;
                }
            return atLeastOneUpperLayerVisible;
        } else
        {
            final Link e = gl.getAssociatedNetPlanLink();

            if (!visualizationSnapshot.getCanvasLinkVisibility(e.getLayer())) return false;
            if (linksToHideInCanvasAsMandatedByUserInTable.contains(e)) return false;
            final boolean inActiveLayer = e.getLayer() == this.getNetPlan().getNetworkLayerDefault();
            if (!showInCanvasLinksInNonActiveLayer && !inActiveLayer) return false;
            return true;
        }
    }

    void hideOnCanvas(Node n)
    {
        nodesToHideInCanvasAsMandatedByUserInTable.add(n);
    }

    void showOnCanvas(Node n)
    {
        if (nodesToHideInCanvasAsMandatedByUserInTable.contains(n)) nodesToHideInCanvasAsMandatedByUserInTable.remove(n);
    }

    boolean isHiddenOnCanvas(Node n)
    {
        return nodesToHideInCanvasAsMandatedByUserInTable.contains(n);
    }

    void hideOnCanvas(Link e)
    {
        linksToHideInCanvasAsMandatedByUserInTable.add(e);
    }
    void showOnCanvas(Link e)
    {
        if (linksToHideInCanvasAsMandatedByUserInTable.contains(e)) linksToHideInCanvasAsMandatedByUserInTable.remove(e);
    }

    boolean isHiddenOnCanvas(Link e)
    {
        return linksToHideInCanvasAsMandatedByUserInTable.contains(e);
    }

    boolean isShowNodeNames()
    {
        return showNodeNames;
    }

    void setShowNodeNames(boolean showNodeNames)
    {
        this.showNodeNames = showNodeNames;
    }

    boolean isShowLinkLabels()
    {
        return showLinkLabels;
    }

    void setShowLinkLabels(boolean showLinkLabels)
    {
        this.showLinkLabels = showLinkLabels;
    }

    boolean isShowLinksInNonActiveLayer()
    {
        return showLinksInNonActiveLayer;
    }

    void setShowLinksInNonActiveLayer(boolean showLinksInNonActiveLayer)
    {
        this.showLinksInNonActiveLayer = showLinksInNonActiveLayer;
    }

    boolean isShowInterLayerLinks()
    {
        return showInterLayerLinks;
    }

    void setShowInterLayerLinks(boolean showInterLayerLinks)
    {
        this.showInterLayerLinks = showInterLayerLinks;
    }

    boolean isShowLowerLayerPropagation()
    {
        return showLowerLayerPropagation;
    }

    void setShowLowerLayerPropagation(boolean showLowerLayerPropagation)
    {
        this.showLowerLayerPropagation = showLowerLayerPropagation;
    }

    boolean isShowUpperLayerPropagation()
    {
        return showUpperLayerPropagation;
    }

    void setShowUpperLayerPropagation(boolean showUpperLayerPropagation)
    {
        this.showUpperLayerPropagation = showUpperLayerPropagation;
    }

    boolean isShowLayerPropagation()
    {
        return showLayerPropagation;
    }

    void setShowLayerPropagation(boolean showLayerPropagation)
    {
        this.showLayerPropagation = showLayerPropagation;
    }

    boolean isShowNonConnectedNodes()
    {
        return showNonConnectedNodes;
    }

    void setShowNonConnectedNodes(boolean showNonConnectedNodes)
    {
        this.showNonConnectedNodes = showNonConnectedNodes;
    }
}
