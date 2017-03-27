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

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.net2plan.internal.Constants.*;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
class ElementSelector
{
    private final VisualizationMediator mediator;

    private NetworkElement pickedElement;
    private Pair<Demand, Link> pickedForwardingRule;

    ElementSelector(@NotNull final VisualizationMediator mediator)
    {
        this.mediator = mediator;

        this.pickedElement = null;
        this.pickedForwardingRule = null;
    }

    boolean isElementPicked(@NotNull NetworkElement element)
    {
        return element == pickedElement;
    }

    @Nullable
    Object getPickedElement()
    {
        return pickedElement != null ? pickedElement : pickedForwardingRule;
    }

    void pickElement(NetworkElement e)
    {
        if (e instanceof NetworkLayer) pickLayer((NetworkLayer) e);
        else if (e instanceof Node) pickNode((Node) e);
        else if (e instanceof Link) pickLink((Link) e);
        else if (e instanceof Demand) pickDemand((Demand) e);
        else if (e instanceof Route) pickRoute((Route) e);
        else if (e instanceof MulticastDemand) pickMulticastDemand((MulticastDemand) e);
        else if (e instanceof MulticastTree) pickMulticastTree((MulticastTree) e);
        else if (e instanceof Resource) pickResource((Resource) e);
        else if (e instanceof SharedRiskGroup) pickSRG((SharedRiskGroup) e);
        else throw new RuntimeException();
    }

    void resetPickedState()
    {
        this.pickedElement = null;
        this.pickedForwardingRule = null;

        for (GUINode n : getCanvasAllGUINodes())
        {
            n.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
            n.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
        }
        for (GUILink e : getCanvasAllGUILinks(true, false))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_REGGUILINK_HASARROW);
            setCurrentDefaultEdgeStroke(e, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE);
            final boolean isDown = e.getAssociatedNetPlanLink().isDown();
            final Paint color = isDown ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR;
            e.setEdgeDrawPaint(color);
            e.setShownSeparated(isDown);
        }
        for (GUILink e : getCanvasAllGUILinks(false, true))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_INTRANODEGUILINK_HASARROW);
            setCurrentDefaultEdgeStroke(e, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE);
            e.setEdgeDrawPaint(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
            e.setShownSeparated(false);
        }
    }

    private void pickLayer(NetworkLayer pickedLayer)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.LAYER;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedLayer;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedLayer);
    }

    private void pickDemand(Demand pickedDemand)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemand;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedDemand);

        final boolean isDemandLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedDemand.getLayer());
        final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
        final GUINode gnDestination = getCanvasAssociatedGUINode(pickedDemand.getEgressNode(), pickedDemand.getLayer());
        Pair<Set<Link>, Set<Link>> thisLayerPropagation = null;
        if (showInCanvasThisLayerPropagation && isDemandLayerVisibleInTheCanvas)
        {
            thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(false);
            final Set<Link> linksPrimary = thisLayerPropagation.getFirst();
            final Set<Link> linksBackup = thisLayerPropagation.getSecond();
            final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary, linksBackup);
            drawColateralLinks(Sets.difference(linksPrimary, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawColateralLinks(Sets.difference(linksBackup, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
            drawColateralLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
        }
        if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
        {
            if (thisLayerPropagation == null)
                thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(false);
            final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoPrimary = getDownCoupling(thisLayerPropagation.getFirst());
            final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoBackup = getDownCoupling(thisLayerPropagation.getSecond());
            final InterLayerPropagationGraph ipgPrimary = new InterLayerPropagationGraph(downLayerInfoPrimary.getFirst(), null, downLayerInfoPrimary.getSecond(), false, false);
            final InterLayerPropagationGraph ipgBackup = new InterLayerPropagationGraph(downLayerInfoBackup.getFirst(), null, downLayerInfoBackup.getSecond(), false, false);
            final Set<Link> linksPrimary = ipgPrimary.getLinksInGraph();
            final Set<Link> linksBackup = ipgBackup.getLinksInGraph();
            final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary, linksBackup);
            final Set<Link> linksOnlyPrimary = Sets.difference(linksPrimary, linksPrimaryAndBackup);
            final Set<Link> linksOnlyBackup = Sets.difference(linksBackup, linksPrimaryAndBackup);
            drawColateralLinks(linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawColateralLinks(linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
            drawDownPropagationInterLayerLinks(linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
            drawColateralLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            drawDownPropagationInterLayerLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
        }
        if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
        {
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true, false);
            drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        /* Picked link the last, so overrides the rest */
        if (isDemandLayerVisibleInTheCanvas)
        {
            gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
        }
    }

    private void pickSRG(SharedRiskGroup pickedSRG)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedSRG;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedSRG);

        final Set<Link> allAffectedLinks = pickedSRG.getAffectedLinksAllLayers();
        Map<Link, Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>>> thisLayerPropInfo = new HashMap<>();
        if (showInCanvasThisLayerPropagation)
        {
            for (Link link : allAffectedLinks)
            {
                thisLayerPropInfo.put(link, link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false));
                final Set<Link> linksPrimary = thisLayerPropInfo.get(link).getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                final Set<Link> linksBackup = thisLayerPropInfo.get(link).getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                final Set<Link> linksMulticast = thisLayerPropInfo.get(link).getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                drawColateralLinks(Sets.union(Sets.union(linksPrimary, linksBackup), linksMulticast), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            }
        }
        if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
        {
            final Set<Link> affectedCoupledLinks = allAffectedLinks.stream().filter(e -> e.isCoupled()).collect(Collectors.toSet());
            final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> couplingInfo = getDownCoupling(affectedCoupledLinks);
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(couplingInfo.getFirst(), null, couplingInfo.getSecond(), false, false);
            final Set<Link> lowerLayerLinks = ipg.getLinksInGraph();
            drawColateralLinks(lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            drawDownPropagationInterLayerLinks(lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
        }
        if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
        {
            final Set<Demand> demandsPrimaryAndBackup = new HashSet<>();
            final Set<Pair<MulticastDemand, Node>> demandsMulticast = new HashSet<>();
            for (Link link : allAffectedLinks)
            {
                final Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> thisLinkInfo =
                        showInCanvasThisLayerPropagation ? thisLayerPropInfo.get(link) : link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
                demandsPrimaryAndBackup.addAll(Sets.union(thisLinkInfo.getFirst().keySet(), thisLinkInfo.getSecond().keySet()));
                demandsMulticast.addAll(thisLinkInfo.getThird().keySet());
            }
            final Set<Link> coupledUpperLinks = getUpCoupling(demandsPrimaryAndBackup, demandsMulticast);
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, coupledUpperLinks, null, true, false);
            drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
        }
        /* Picked link the last, so overrides the rest */
        for (Link link : allAffectedLinks)
        {
            final GUILink gl = getCanvasAssociatedGUILink(link);
            if (gl == null) continue;
            gl.setHasArrow(true);
            setCurrentDefaultEdgeStroke(gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
            final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED;
            gl.setEdgeDrawPaint(color);
            gl.setShownSeparated(true);
        }
        for (Node node : pickedSRG.getNodes())
        {
            for (GUINode gn : getCanvasVerticallyStackedGUINodes(node))
            {
                gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
                gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
            }
        }
    }

    private void pickMulticastDemand(MulticastDemand pickedDemand)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemand;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedDemand);

        final boolean isDemandLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedDemand.getLayer());
        final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
        Set<Link> linksThisLayer = null;
        for (Node egressNode : pickedDemand.getEgressNodes())
        {
            final GUINode gnDestination = getCanvasAssociatedGUINode(egressNode, pickedDemand.getLayer());
            if (showInCanvasThisLayerPropagation && isDemandLayerVisibleInTheCanvas)
            {
                linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode, false);
                drawColateralLinks(linksThisLayer, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
            {
                if (linksThisLayer == null)
                    linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode, false);
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(linksThisLayer);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false, false);
                final Set<Link> linksLowerLayers = ipg.getLinksInGraph();
                drawColateralLinks(linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final Set<Link> upCoupledLink = getUpCoupling(null, Collections.singleton(Pair.of(pickedDemand, egressNode)));
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upCoupledLink, null, true, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isDemandLayerVisibleInTheCanvas)
            {
                gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }
        /* Picked link the last, so overrides the rest */
        if (isDemandLayerVisibleInTheCanvas)
        {
            gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
        }
    }

    private void pickRoute(Route pickedRoute)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedRoute;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedRoute);

        final boolean isRouteLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedRoute.getLayer());
        if (showInCanvasThisLayerPropagation && isRouteLayerVisibleInTheCanvas)
        {
            final List<Link> linksPrimary = pickedRoute.getSeqLinks();
            drawColateralLinks(linksPrimary, pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
        {
            final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = getDownCoupling(pickedRoute.getSeqLinks());
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false, false);
            drawColateralLinks(ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedRoute.getDemand().isCoupled())
        {
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedRoute.getDemand().getCoupledLink()), null, true, false);
            drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        /* Picked link the last, so overrides the rest */
        if (isRouteLayerVisibleInTheCanvas)
        {
            final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedRoute.getIngressNode(), pickedRoute.getLayer());
            final GUINode gnDestination = getCanvasAssociatedGUINode(pickedRoute.getEgressNode(), pickedRoute.getLayer());
            gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
        }
    }

    private void pickMulticastTree(MulticastTree pickedTree)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedTree;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedTree);

        final boolean isTreeLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedTree.getLayer());
        final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedTree.getIngressNode(), pickedTree.getLayer());
        for (Node egressNode : pickedTree.getEgressNodes())
        {
            final GUINode gnDestination = getCanvasAssociatedGUINode(egressNode, pickedTree.getLayer());
            if (showInCanvasThisLayerPropagation && isTreeLayerVisibleInTheCanvas)
            {
                final List<Link> linksPrimary = pickedTree.getSeqLinksToEgressNode(egressNode);
                drawColateralLinks(linksPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = getDownCoupling(pickedTree.getSeqLinksToEgressNode(egressNode));
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedTree.getMulticastDemand().isCoupled())
            {
                final Set<Link> upperCoupledLink = getUpCoupling(null, Arrays.asList(Pair.of(pickedTree.getMulticastDemand(), egressNode)));
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upperCoupledLink, null, true, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (isTreeLayerVisibleInTheCanvas)
            {
                gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }
        /* Picked link the last, so overrides the rest */
        if (isTreeLayerVisibleInTheCanvas)
        {
            gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
        }
    }

    private void pickLink(Link pickedLink)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedLink;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedLink);

        final boolean isLinkLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedLink.getLayer());
        Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> thisLayerTraversalInfo = null;
        if (showInCanvasThisLayerPropagation && isLinkLayerVisibleInTheCanvas)
        {
            thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
            final Set<Link> linksPrimary = thisLayerTraversalInfo.getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
            final Set<Link> linksBackup = thisLayerTraversalInfo.getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
            final Set<Link> linksMulticast = thisLayerTraversalInfo.getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
            drawColateralLinks(Sets.union(Sets.union(linksPrimary, linksBackup), linksMulticast), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
        {
            final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(Arrays.asList(pickedLink));
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false, false);
            drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
        {
            if (thisLayerTraversalInfo == null)
                thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
            final Set<Demand> demandsPrimaryAndBackup = Sets.union(thisLayerTraversalInfo.getFirst().keySet(), thisLayerTraversalInfo.getSecond().keySet());
            final Set<Pair<MulticastDemand, Node>> mDemands = thisLayerTraversalInfo.getThird().keySet();
            final Set<Link> initialUpperLinks = getUpCoupling(demandsPrimaryAndBackup, mDemands);
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(initialUpperLinks), null, true, false);
            drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        /* Picked link the last, so overrides the rest */
        if (isLinkLayerVisibleInTheCanvas)
        {
            final GUILink gl = getCanvasAssociatedGUILink(pickedLink);
            gl.setHasArrow(true);
            setCurrentDefaultEdgeStroke(gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
            final Paint color = pickedLink.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
            gl.setEdgeDrawPaint(color);
            gl.setShownSeparated(true);
        }
    }

    private void pickNode(Node pickedNode)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedNode;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedNode);

        for (GUINode gn : getCanvasVerticallyStackedGUINodes(pickedNode))
        {
            gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
            gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
        }
        for (Link e : Sets.union(pickedNode.getOutgoingLinks(this.getNetPlan().getNetworkLayerDefault()), pickedNode.getIncomingLinks(this.getNetPlan().getNetworkLayerDefault())))
        {
            final GUILink gl = getCanvasAssociatedGUILink(e);
            gl.setShownSeparated(true);
            gl.setHasArrow(true);
        }
    }

    private void pickResource(Resource pickedResource)
    {
        resetPickedState();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedResource;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedResource);

        for (GUINode gn : getCanvasVerticallyStackedGUINodes(pickedResource.getHostNode()))
        {
            gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
            gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
        }
    }

    private void pickForwardingRule(Pair<Demand, Link> pickedFR)
    {
        resetPickedState();
        this.pickedForwardingRule = pickedFR;
        this.pickedElement = null;
        pickTimeLineManager.addElement(this.getNetPlan(), pickedFR);

        final boolean isFRLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedFR.getFirst().getLayer());
        final Demand pickedDemand = pickedFR.getFirst();
        final Link pickedLink = pickedFR.getSecond();
        if (showInCanvasThisLayerPropagation && isFRLayerVisibleInTheCanvas)
        {
            final Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> triple =
                    pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
            final Set<Link> linksPrimary = triple.getFirst().get(pickedDemand);
            final Set<Link> linksBackup = triple.getSecond().get(pickedDemand);
            drawColateralLinks(Sets.union(linksPrimary, linksBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
        {
            final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(Arrays.asList(pickedLink));
            final InterLayerPropagationGraph ipgCausedByLink = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false, false);
            final Set<Link> frPropagationLinks = ipgCausedByLink.getLinksInGraph();
            drawColateralLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
        if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
        {
            final InterLayerPropagationGraph ipgCausedByDemand = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true, false);
            final Set<Link> frPropagationLinks = ipgCausedByDemand.getLinksInGraph();
            drawColateralLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            drawDownPropagationInterLayerLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
        }
		/* Picked link the last, so overrides the rest */
        if (isFRLayerVisibleInTheCanvas)
        {
            final GUILink gl = getCanvasAssociatedGUILink(pickedLink);
            gl.setHasArrow(true);
            setCurrentDefaultEdgeStroke(gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
            final Paint color = pickedLink.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
            gl.setEdgeDrawPaint(color);
            gl.setShownSeparated(true);
            gl.getOriginNode().setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gl.getOriginNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            gl.getDestinationNode().setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            gl.getDestinationNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
        }
    }
}
