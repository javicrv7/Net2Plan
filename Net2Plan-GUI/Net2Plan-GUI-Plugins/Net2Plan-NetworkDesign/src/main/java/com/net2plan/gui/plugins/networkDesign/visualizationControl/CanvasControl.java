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

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
class CanvasControl
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

    private int interLayerSpaceInPixels;

    private float linkWidthIncreaseFactor;
    private float nodeSizeIncreaseFactor;

    private Map<Node, Boolean> nodeVisibilityMap;
    private Map<Link, Boolean> linkVisibilityMap;

    /* These need is recomputed inside a rebuild */
    private Map<Node, Set<GUILink>> cache_canvasIntraNodeGUILinks;
    private Map<Link, GUILink> cache_canvasRegularLinkMap;
    private Map<Node, Map<Pair<Integer, Integer>, GUILink>> cache_mapNode2IntraNodeCanvasGUILinkMap; // integers are orders of REAL VISIBLE LAYERS
    private Map<Node, List<GUINode>> cache_mapNode2ListVerticallyStackedGUINodes;

    CanvasControl(@NotNull final VisualizationMediator mediator)
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

        this.interLayerSpaceInPixels = 50;

        this.linkWidthIncreaseFactor = 1;
        this.nodeSizeIncreaseFactor = 1;

        this.nodeVisibilityMap = new HashMap<>();
        this.linkVisibilityMap = new HashMap<>();
    }

    boolean isVisible(GUINode gn)
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

    boolean isVisible(GUILink gl)
    {
        if (gl.isIntraNodeLink())
        {
            final Node node = gl.getOriginNode().getAssociatedNode();
            final NetworkLayer originLayer = gl.getOriginNode().getLayer();
            final NetworkLayer destinationLayer = gl.getDestinationNode().getLayer();
            final int originIndexInVisualization = mediator.getLayerOrderPosition(originLayer, false);
            final int destinationIndexInVisualization = mediator.getLayerOrderPosition(destinationLayer, false);
            final int lowerVIndex = originIndexInVisualization < destinationIndexInVisualization ? originIndexInVisualization : destinationIndexInVisualization;
            final int upperVIndex = originIndexInVisualization > destinationIndexInVisualization ? originIndexInVisualization : destinationIndexInVisualization;

            boolean atLeastOneLowerLayerVisible = false;
            for (int vIndex = 0; vIndex <= lowerVIndex; vIndex++)
                if (isVisible(getAssociatedGUINode(node, mediator.getLayerAtPosition(vIndex, false))))
                {
                    atLeastOneLowerLayerVisible = true;
                    break;
                }
            if (!atLeastOneLowerLayerVisible) return false;
            boolean atLeastOneUpperLayerVisible = false;
            for (int vIndex = upperVIndex; vIndex < mediator.getNumberOfLayers(false); vIndex++)
                if (isVisible(getAssociatedGUINode(node, mediator.getLayerAtPosition(vIndex, false))))
                {
                    atLeastOneUpperLayerVisible = true;
                    break;
                }
            return atLeastOneUpperLayerVisible;
        } else
        {
            final Link e = gl.getAssociatedNetPlanLink();

            if (!mediator.isVisible(e.getLayer())) return false;
            if (!mediator.isVisible(e)) return false;
            final boolean inActiveLayer = e.getLayer() == mediator.getNetPlan().getNetworkLayerDefault();
            if (!showLinksInNonActiveLayer && !inActiveLayer) return false;
            return true;
        }
    }

    boolean isVisible(Node n)
    {
        return !nodeVisibilityMap.get(n);
    }

    boolean isVisible(Link e)
    {
        return !linkVisibilityMap.get(e);
    }

    void show(Node n)
    {
        nodeVisibilityMap.put(n, true);
    }

    void show(Link e)
    {
        linkVisibilityMap.put(e, true);
    }

    void hide(Node n)
    {
        nodeVisibilityMap.put(n, false);
    }

    void hide(Link e)
    {
        linkVisibilityMap.put(e, true);
    }

    List<GUINode> getStackedGUINodes(Node n)
    {
        return cache_mapNode2ListVerticallyStackedGUINodes.get(n);
    }

    GUINode getAssociatedGUINode(Node n, NetworkLayer layer)
    {
        final Integer trueVisualizationIndex = mediator.getLayerOrderPosition(layer, false);
        if (trueVisualizationIndex == null) return null;
        return getStackedGUINodes(n).get(trueVisualizationIndex);
    }

    GUILink getAssociatedGUILink(Link e)
    {
        return cache_canvasRegularLinkMap.get(e);
    }

    GUILink getIntraNodeGUILink(Node n, NetworkLayer from, NetworkLayer to)
    {
        final Integer fromRealVIndex = mediator.getLayerOrderPosition(from, false);
        final Integer toRealVIndex = mediator.getLayerOrderPosition(to, false);
        if ((fromRealVIndex == null) || (toRealVIndex == null)) return null;
        return cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).get(Pair.of(fromRealVIndex, toRealVIndex));
    }

    Set<GUILink> getIntraNodeGUILinks(Node n)
    {
        return cache_canvasIntraNodeGUILinks.get(n);
    }

    List<GUILink> getIntraNodeGUILinkSequence(Node n, NetworkLayer from, NetworkLayer to)
    {
        if (from.getNetPlan() != mediator.getNetPlan()) throw new RuntimeException("Bad");
        if (to.getNetPlan() != mediator.getNetPlan()) throw new RuntimeException("Bad");
        final Integer fromRealVIndex = mediator.getLayerOrderPosition(from, false);
        final Integer toRealVIndex = mediator.getLayerOrderPosition(to, false);

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

    boolean decreaseFontSizeAll()
    {
        boolean changedSize = false;
        for (GUINode gn : getAllGUINodes())
            changedSize |= gn.decreaseFontSize();
        return changedSize;
    }

    void increaseFontSizeAll()
    {
        for (GUINode gn : getAllGUINodes())
            gn.increaseFontSize();
    }

    void decreaseNodeSizeAll()
    {
        nodeSizeIncreaseFactor *= VisualizationConstants.SCALE_OUT;
        for (GUINode gn : getAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_OUT);
    }

    void increaseNodeSizeAll()
    {
        nodeSizeIncreaseFactor *= VisualizationConstants.SCALE_IN;
        for (GUINode gn : getAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_IN);
    }

    void decreaseLinkSizeAll()
    {
        final float multFactor = VisualizationConstants.SCALE_OUT;
        linkWidthIncreaseFactor *= multFactor;
        for (GUILink e : getAllGUILinks(true, true))
            e.setEdgeStroke(resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
    }

    void increaseLinkSizeAll()
    {
        final float multFactor = VisualizationConstants.SCALE_IN;
        linkWidthIncreaseFactor *= multFactor;
        for (GUILink e : getAllGUILinks(true, true))
            e.setEdgeStroke(resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
    }

    Set<GUILink> getAllGUILinks(boolean includeRegularLinks, boolean includeInterLayerLinks)
    {
        Set<GUILink> res = new HashSet<>();
        if (includeRegularLinks) res.addAll(cache_canvasRegularLinkMap.values());
        if (includeInterLayerLinks)
            for (Node n : mediator.getNetPlan().getNodes())
                res.addAll(this.cache_canvasIntraNodeGUILinks.get(n));
        return res;
    }

    Set<GUINode> getAllGUINodes()
    {
        Set<GUINode> res = new HashSet<>();
        for (List<GUINode> list : this.cache_mapNode2ListVerticallyStackedGUINodes.values()) res.addAll(list);
        return res;
    }

    boolean isLowerLayerPropagationShown()
    {
        return showLowerLayerPropagation;
    }

    boolean isUpperLayerPropagationShown()
    {
        return showUpperLayerPropagation;
    }

    boolean isThisLayerPropagationShown()
    {
        return showLayerPropagation;
    }

    void setLowerLayerPropagationVisibility(boolean showLowerLayerPropagation)
    {
        if (showLowerLayerPropagation == this.showLowerLayerPropagation) return;
        this.showLowerLayerPropagation = showLowerLayerPropagation;
    }

    void setUpperLayerPropagationVisibility(boolean showUpperLayerPropagation)
    {
        if (showUpperLayerPropagation == this.showUpperLayerPropagation) return;
        this.showUpperLayerPropagation = showUpperLayerPropagation;
    }

    void setThisLayerPropagationVisibility(boolean showThisLayerPropagation)
    {
        if (showThisLayerPropagation == this.showLayerPropagation) return;
        this.showLayerPropagation = showThisLayerPropagation;
    }

    boolean isNodeNamesShown()
    {
        return showNodeNames;
    }

    boolean isLinkLabelsShown()
    {
        return showLinkLabels;
    }

    boolean isLinksInNonActiveLayerShown()
    {
        return showLinksInNonActiveLayer;
    }

    boolean isInterLayerLinksShown()
    {
        return showInterLayerLinks;
    }

    boolean isNonConnectedNodesShown()
    {
        return showNonConnectedNodes;
    }

    void setNodeNamesVisibility(boolean showNodeNames)
    {
        this.showNodeNames = showNodeNames;
    }

    void setLinkLabelsVisibility(boolean showLinkLabels)
    {
        this.showLinkLabels = showLinkLabels;
    }

    void setLinksInNonActiveLayerVisibility(boolean showLinksInNonActiveLayer)
    {
        this.showLinksInNonActiveLayer = showLinksInNonActiveLayer;
    }

    void setInterlayerLinksVisibility(boolean showInterLayerLinks)
    {
        this.showInterLayerLinks = showInterLayerLinks;
    }

    void setNonConnectedNodesVisibility(boolean showNonConnectedNodes)
    {
        this.showNonConnectedNodes = showNonConnectedNodes;
    }

    int getInterLayerSpaceInPixels()
    {
        return interLayerSpaceInPixels;
    }

    void setInterLayerSpaceInPixels(int interLayerSpaceInPixels)
    {
        this.interLayerSpaceInPixels = interLayerSpaceInPixels;
    }

    private static BasicStroke resizedBasicStroke(BasicStroke a, float multFactorSize)
    {
        if (multFactorSize == 1) return a;
        return new BasicStroke(a.getLineWidth() * multFactorSize, a.getEndCap(), a.getLineJoin(), a.getMiterLimit(), a.getDashArray(), a.getDashPhase());
    }

    private void setCurrentDefaultEdgeStroke(GUILink e, BasicStroke a, BasicStroke na)
    {
        e.setEdgeStroke(resizedBasicStroke(a, linkWidthIncreaseFactor), resizedBasicStroke(na, linkWidthIncreaseFactor));
    }

    private void drawColateralLinks(Collection<Link> links, Paint colorIfNotFailedLink)
    {
        for (Link link : links)
        {
            final GUILink glColateral = mediator.getAssociatedGUILink(link);
            if (glColateral == null) continue;
            setCurrentDefaultEdgeStroke(glColateral, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER);
            final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : colorIfNotFailedLink;
            glColateral.setEdgeDrawPaint(color);
            glColateral.setShownSeparated(true);
            glColateral.setHasArrow(true);
        }
    }

    private void drawDownPropagationInterLayerLinks(Set<Link> links, Paint color)
    {
        for (Link link : links)
        {
            final GUILink gl = mediator.getAssociatedGUILink(link);
            if (gl == null) continue;
            if (!link.isCoupled()) continue;
            final boolean isCoupledToDemand = link.getCoupledDemand() != null;
            final NetworkLayer upperLayer = link.getLayer();
            final NetworkLayer lowerLayer = isCoupledToDemand ? link.getCoupledDemand().getLayer() : link.getCoupledMulticastDemand().getLayer();
            if (!mediator.isVisible(lowerLayer)) continue;
            for (GUILink interLayerLink : mediator.getIntraNodeGUISequence(upperLayer, link.getOriginNode(), lowerLayer))
            {
                setCurrentDefaultEdgeStroke(interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
            for (GUILink interLayerLink : mediator.getIntraNodeGUISequence(lowerLayer, link.getDestinationNode(), upperLayer))
            {
                setCurrentDefaultEdgeStroke(interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
        }
    }

    private Pair<Set<GUILink>, Set<GUILink>> getCanvasAssociatedGUILinksIncludingCoupling(Link e, boolean regularLinkIsPrimary)
    {
        Set<GUILink> resPrimary = new HashSet<>();
        Set<GUILink> resBackup = new HashSet<>();
        if (regularLinkIsPrimary) resPrimary.add(getAssociatedGUILink(e));
        else resBackup.add(getAssociatedGUILink(e));
        if (!e.isCoupled()) return Pair.of(resPrimary, resBackup);
        if (e.getCoupledDemand() != null)
        {
            /* add the intranode links */
            final NetworkLayer upperLayer = e.getLayer();
            final NetworkLayer downLayer = e.getCoupledDemand().getLayer();
            if (regularLinkIsPrimary)
            {
                resPrimary.addAll(getIntraNodeGUILinkSequence(e.getOriginNode(), upperLayer, downLayer));
                resPrimary.addAll(getIntraNodeGUILinkSequence(e.getDestinationNode(), downLayer, upperLayer));
            } else
            {
                resBackup.addAll(getIntraNodeGUILinkSequence(e.getOriginNode(), upperLayer, downLayer));
                resBackup.addAll(getIntraNodeGUILinkSequence(e.getDestinationNode(), downLayer, upperLayer));
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
                resPrimary.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), upperLayer, downLayer));
                resPrimary.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), downLayer, upperLayer));
                for (Node n : lowerLayerDemand.getEgressNodes())
                {
                    resPrimary.addAll(getIntraNodeGUILinkSequence(n, upperLayer, downLayer));
                    resPrimary.addAll(getIntraNodeGUILinkSequence(n, downLayer, upperLayer));
                }
            } else
            {
                resBackup.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), upperLayer, downLayer));
                resBackup.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), downLayer, upperLayer));
                for (Node n : lowerLayerDemand.getEgressNodes())
                {
                    resBackup.addAll(getIntraNodeGUILinkSequence(n, upperLayer, downLayer));
                    resBackup.addAll(getIntraNodeGUILinkSequence(n, downLayer, upperLayer));
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
}
