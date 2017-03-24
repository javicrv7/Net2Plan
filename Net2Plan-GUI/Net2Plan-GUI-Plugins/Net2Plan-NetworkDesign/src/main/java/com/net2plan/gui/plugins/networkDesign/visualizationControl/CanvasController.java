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
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MapUtils;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.util.*;

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

    private Map<Node, Boolean> nodeVisibilityMap;

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

        this.nodeVisibilityMap = new HashMap<>();
    }

    boolean isVisibleInCanvas(GUINode gn)
    {
        final Node n = gn.getAssociatedNode();

        if (!nodeVisibilityMap.get(n)) return false;
        if (!showNonConnectedNodes)
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
        if (nodesToHideInCanvasAsMandatedByUserInTable.contains(n))
            nodesToHideInCanvasAsMandatedByUserInTable.remove(n);
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
        if (linksToHideInCanvasAsMandatedByUserInTable.contains(e))
            linksToHideInCanvasAsMandatedByUserInTable.remove(e);
    }

    boolean isHiddenOnCanvas(Link e)
    {
        return linksToHideInCanvasAsMandatedByUserInTable.contains(e);
    }

    List<NetworkLayer> getCanvasLayersInVisualizationOrder(boolean includeNonVisible)
    {
        BidiMap<Integer, NetworkLayer> map = includeNonVisible ? new DualHashBidiMap<>(MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder())) : cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap();
        List<NetworkLayer> res = new ArrayList<>();
        for (int vIndex = 0; vIndex < this.getNetPlan().getNumberOfLayers(); vIndex++)
            res.add(map.get(vIndex));
        return res;
    }

    Map<NetworkLayer, Integer> getCanvasLayerOrderIndexMap(boolean includeNonVisible)
    {
        return includeNonVisible ? visualizationSnapshot.getMapCanvasLayerVisualizationOrder() : cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible;
    }

    List<GUINode> getCanvasVerticallyStackedGUINodes(Node n)
    {
        return cache_mapNode2ListVerticallyStackedGUINodes.get(n);
    }

    GUINode getCanvasAssociatedGUINode(Node n, NetworkLayer layer)
    {
        final Integer trueVisualizationIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(layer);
        if (trueVisualizationIndex == null) return null;
        return getCanvasVerticallyStackedGUINodes(n).get(trueVisualizationIndex);
    }

    GUILink getCanvasAssociatedGUILink(Link e)
    {
        return cache_canvasRegularLinkMap.get(e);
    }

    Pair<Set<GUILink>, Set<GUILink>> getCanvasAssociatedGUILinksIncludingCoupling(Link e, boolean regularLinkIsPrimary)
    {
        Set<GUILink> resPrimary = new HashSet<>();
        Set<GUILink> resBackup = new HashSet<>();
        if (regularLinkIsPrimary) resPrimary.add(getCanvasAssociatedGUILink(e));
        else resBackup.add(getCanvasAssociatedGUILink(e));
        if (!e.isCoupled()) return Pair.of(resPrimary, resBackup);
        if (e.getCoupledDemand() != null)
        {
            /* add the intranode links */
            final NetworkLayer upperLayer = e.getLayer();
            final NetworkLayer downLayer = e.getCoupledDemand().getLayer();
            if (regularLinkIsPrimary)
            {
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(e.getOriginNode(), upperLayer, downLayer));
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(e.getDestinationNode(), downLayer, upperLayer));
            } else
            {
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(e.getOriginNode(), upperLayer, downLayer));
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(e.getDestinationNode(), downLayer, upperLayer));
            }

			/* add the regular links */
            Pair<Set<Link>, Set<Link>> traversedLinks = e.getCoupledDemand().getLinksThisLayerPotentiallyCarryingTraffic(true);
            for (Link ee : traversedLinks.getFirst())
            {
                Pair<Set<GUILink>, Set<GUILink>> pairGuiLinks = getCanvasAssociatedGUILinksIncludingCoupling(ee, true);
                if (regularLinkIsPrimary) resPrimary.addAll(pairGuiLinks.getFirst());
                else resBackup.addAll(pairGuiLinks.getFirst());
                resBackup.addAll(pairGuiLinks.getSecond());
            }
            for (Link ee : traversedLinks.getSecond())
            {
                Pair<Set<GUILink>, Set<GUILink>> pairGuiLinks = getCanvasAssociatedGUILinksIncludingCoupling(ee, false);
                resPrimary.addAll(pairGuiLinks.getFirst());
                resBackup.addAll(pairGuiLinks.getSecond());
            }
        } else if (e.getCoupledMulticastDemand() != null)
        {
            /* add the intranode links */
            final NetworkLayer upperLayer = e.getLayer();
            final MulticastDemand lowerLayerDemand = e.getCoupledMulticastDemand();
            final NetworkLayer downLayer = lowerLayerDemand.getLayer();
            if (regularLinkIsPrimary)
            {
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), upperLayer, downLayer));
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), downLayer, upperLayer));
                for (Node n : lowerLayerDemand.getEgressNodes())
                {
                    resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(n, upperLayer, downLayer));
                    resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(n, downLayer, upperLayer));
                }
            } else
            {
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), upperLayer, downLayer));
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), downLayer, upperLayer));
                for (Node n : lowerLayerDemand.getEgressNodes())
                {
                    resBackup.addAll(getCanvasIntraNodeGUILinkSequence(n, upperLayer, downLayer));
                    resBackup.addAll(getCanvasIntraNodeGUILinkSequence(n, downLayer, upperLayer));
                }
            }

            for (MulticastTree t : lowerLayerDemand.getMulticastTrees())
                for (Link ee : t.getLinkSet())
                {
                    Pair<Set<GUILink>, Set<GUILink>> pairGuiLinks = getCanvasAssociatedGUILinksIncludingCoupling(ee, true);
                    resPrimary.addAll(pairGuiLinks.getFirst());
                    resBackup.addAll(pairGuiLinks.getSecond());
                }
        }
        return Pair.of(resPrimary, resBackup);
    }

    GUILink getCanvasIntraNodeGUILink(Node n, NetworkLayer from, NetworkLayer to)
    {
        final Integer fromRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(from);
        final Integer toRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(to);
        if ((fromRealVIndex == null) || (toRealVIndex == null)) return null;
        return cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).get(Pair.of(fromRealVIndex, toRealVIndex));
    }

    Set<GUILink> getCanvasIntraNodeGUILinks(Node n)
    {
        return cache_canvasIntraNodeGUILinks.get(n);
    }

    List<GUILink> getCanvasIntraNodeGUILinkSequence(Node n, NetworkLayer from, NetworkLayer to)
    {
        if (from.getNetPlan() != this.getNetPlan()) throw new RuntimeException("Bad");
        if (to.getNetPlan() != this.getNetPlan()) throw new RuntimeException("Bad");
        final Integer fromRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(from);
        final Integer toRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(to);

        final List<GUILink> res = new LinkedList<>();
        if ((fromRealVIndex == null) || (toRealVIndex == null)) return res;
        if (fromRealVIndex == toRealVIndex) return res;
        final int increment = toRealVIndex > fromRealVIndex ? 1 : -1;
        int vLayerIndex = fromRealVIndex;
        do
        {
            final int origin = vLayerIndex;
            final int destination = vLayerIndex + increment;
            res.add(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).get(Pair.of(origin, destination)));
            vLayerIndex += increment;
        } while (vLayerIndex != toRealVIndex);

        return res;
    }

    boolean decreaseCanvasFontSizeAll()
    {
        boolean changedSize = false;
        for (GUINode gn : getCanvasAllGUINodes())
            changedSize |= gn.decreaseFontSize();
        return changedSize;
    }

    void increaseCanvasFontSizeAll()
    {
        for (GUINode gn : getCanvasAllGUINodes())
            gn.increaseFontSize();
    }

    void decreaseCanvasNodeSizeAll()
    {
        nodeSizeIncreaseFactorRespectToDefault *= VisualizationConstants.SCALE_OUT;
        for (GUINode gn : getCanvasAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_OUT);
    }

    void increaseCanvasNodeSizeAll()
    {
        nodeSizeIncreaseFactorRespectToDefault *= VisualizationConstants.SCALE_IN;
        for (GUINode gn : getCanvasAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_IN);
    }

    void decreaseCanvasLinkSizeAll()
    {
        final float multFactor = VisualizationConstants.SCALE_OUT;
        linkWidthIncreaseFactorRespectToDefault *= multFactor;
        for (GUILink e : getCanvasAllGUILinks(true, true))
            e.setEdgeStroke(resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
    }

    void increaseCanvasLinkSizeAll()
    {
        final float multFactor = VisualizationConstants.SCALE_IN;
        linkWidthIncreaseFactorRespectToDefault *= multFactor;
        for (GUILink e : getCanvasAllGUILinks(true, true))
            e.setEdgeStroke(resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
    }

    int getCanvasNumberOfVisibleLayers()
    {
        return cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size();
    }

    boolean isShowInCanvasInterLayerLinks()
    {
        return canvasController.isShowInterLayerLinks();
    }

    void setShowInCanvasInterLayerLinks(boolean showInterLayerLinks)
    {
        canvasController.setShowInterLayerLinks(showInterLayerLinks);
    }

    boolean isCanvasShowNodeNames()
    {
        return canvasController.isShowNodeNames();
    }

    void setCanvasShowNodeNames(boolean showNodeNames)
    {
        canvasController.setShowNodeNames(showNodeNames);
    }

    boolean isCanvasShowLinkLabels()
    {
        return canvasController.isShowLinkLabels();
    }

    void setCanvasShowLinkLabels(boolean showLinkLabels)
    {
        canvasController.setShowLinkLabels(showLinkLabels);
    }

    boolean isCanvasShowNonConnectedNodes()
    {
        return canvasController.isShowNonConnectedNodes();
    }

    void setCanvasShowNonConnectedNodes(boolean showNonConnectedNodes)
    {
        canvasController.setShowNonConnectedNodes(showNonConnectedNodes);
    }

    Set<GUILink> getCanvasAllGUILinks(boolean includeRegularLinks, boolean includeInterLayerLinks)
    {
        Set<GUILink> res = new HashSet<>();
        if (includeRegularLinks) res.addAll(cache_canvasRegularLinkMap.values());
        if (includeInterLayerLinks)
            for (Node n : this.getNetPlan().getNodes())
                res.addAll(this.cache_canvasIntraNodeGUILinks.get(n));
        return res;
    }

    Set<GUINode> getCanvasAllGUINodes()
    {
        Set<GUINode> res = new HashSet<>();
        for (List<GUINode> list : this.cache_mapNode2ListVerticallyStackedGUINodes.values()) res.addAll(list);
        return res;
    }


    double getCanvasDefaultVerticalDistanceForInterLayers()
    {
        if (this.getNetPlan().getNumberOfNodes() == 0) return 1.0;
        final int numVisibleLayers = getCanvasNumberOfVisibleLayers() == 0 ? this.getNetPlan().getNumberOfLayers() : getCanvasNumberOfVisibleLayers();
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Node n : this.getNetPlan().getNodes())
        {
            final double y = n.getXYPositionMap().getY();
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if ((maxY - minY < 1e-6)) return Math.abs(maxY) / (30 * numVisibleLayers);
        return (maxY - minY) / (30 * numVisibleLayers);
    }

    boolean isShowInCanvasLowerLayerPropagation()
    {
        return showInCanvasLowerLayerPropagation;
    }


    void setShowInCanvasLowerLayerPropagation(boolean showLowerLayerPropagation)
    {
        if (showLowerLayerPropagation == this.showInCanvasLowerLayerPropagation) return;
        this.showInCanvasLowerLayerPropagation = showLowerLayerPropagation;
        if (pickedElementType != null)
            if (pickedElementNotFR != null)
                this.pickElement(pickedElementNotFR);
            else
                this.pickForwardingRule(pickedElementFR);
    }


    boolean isShowInCanvasUpperLayerPropagation()
    {
        return showInCanvasUpperLayerPropagation;
    }

    boolean isShowInCanvasThisLayerPropagation()
    {
        return showInCanvasThisLayerPropagation;
    }


    void setShowInCanvasUpperLayerPropagation(boolean showUpperLayerPropagation)
    {
        if (showUpperLayerPropagation == this.showInCanvasUpperLayerPropagation) return;
        this.showInCanvasUpperLayerPropagation = showUpperLayerPropagation;
        if (pickedElementType != null)
            if (pickedElementNotFR != null)
                this.pickElement(pickedElementNotFR);
            else
                this.pickForwardingRule(pickedElementFR);
    }

    void setShowInCanvasThisLayerPropagation(boolean showThisLayerPropagation)
    {
        if (showThisLayerPropagation == this.showInCanvasThisLayerPropagation) return;
        this.showInCanvasThisLayerPropagation = showThisLayerPropagation;
        if (pickedElementType != null)
            if (pickedElementNotFR != null)
                this.pickElement(pickedElementNotFR);
            else
                this.pickForwardingRule(pickedElementFR);
    }

    Map<NetworkLayer, Boolean> getCanvasLayerVisibilityMap()
    {
        return Collections.unmodifiableMap(this.visualizationSnapshot.getMapCanvasLayerVisibility());
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
