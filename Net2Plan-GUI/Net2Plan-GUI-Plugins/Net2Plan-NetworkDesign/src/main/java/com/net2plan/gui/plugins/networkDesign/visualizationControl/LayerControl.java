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
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MapUtils;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
public class LayerControl
{
    private final VisualizationMediator mediator;
    private final VisualizationSnapshot visualizationSnapshot;

    private Map<NetworkLayer, Integer> cache_mapLayerOrderNoInvisible;

    private float linkWidthIncreaseFactorRespectToDefault;
    private float nodeSizeIncreaseFactorRespectToDefault;

    private int interLayerSpaceInPixels;

    LayerControl(@NotNull final VisualizationMediator mediator)
    {
        this.mediator = mediator;
        this.visualizationSnapshot = new VisualizationSnapshot(mediator.getNetPlan());

        this.cache_mapLayerOrderNoInvisible = new HashMap<>();

        this.interLayerSpaceInPixels = 50;

        this.linkWidthIncreaseFactorRespectToDefault = 1;
        this.nodeSizeIncreaseFactorRespectToDefault = 1;
    }

    /**
     * To call when the topology has new/has removed any link or node, but keeping the same layers.
     * The topology is remade, which involves implicitly a reset of the view
     */
    public void recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals()
    {
        this.setCanvasLayerVisibilityAndOrder(mediator.getNetPlan(), null, null);
    }

    public void setLayerVisibility(final NetworkLayer layer, final boolean isVisible)
    {
        if (!mediator.getNetPlan().getNetworkLayers().contains(layer)) throw new RuntimeException();
        BidiMap<NetworkLayer, Integer> new_layerVisiblityOrderMap = new DualHashBidiMap<>(this.visualizationSnapshot.getMapCanvasLayerVisualizationOrder());
        Map<NetworkLayer, Boolean> new_layerVisibilityMap = new HashMap<>(this.visualizationSnapshot.getMapCanvasLayerVisibility());
        new_layerVisibilityMap.put(layer, isVisible);
        setCanvasLayerVisibilityAndOrder(mediator.getNetPlan(), new_layerVisiblityOrderMap, new_layerVisibilityMap);
    }

    public boolean isLayerVisible(final NetworkLayer layer)
    {
        return visualizationSnapshot.getCanvasLayerVisibility(layer);
    }

    public void setLinksVisibility(final NetworkLayer layer, final boolean showLinks)
    {
        if (!mediator.getNetPlan().getNetworkLayers().contains(layer)) throw new RuntimeException();
        visualizationSnapshot.getMapCanvasLinkVisibility().put(layer, showLinks);
    }

    public boolean isLinksShown(final NetworkLayer layer)
    {
        return visualizationSnapshot.getCanvasLinkVisibility(layer);
    }

    public NetworkLayer getLayerAtPosition(final int layerPosition, final boolean considerNonVisible)
    {
        if (layerPosition < 0 || layerPosition >= mediator.getNumberOfVisibleLayers()) return null;
        return considerNonVisible ? MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()).get(layerPosition) : MapUtils.invertMap(cache_mapLayerOrderNoInvisible).get(layerPosition);
    }

    public int getLayerOrderPosition(@NotNull final NetworkLayer layer, final boolean considerNonVisible)
    {
        if (layer == null) throw new RuntimeException("Layer cannot be refered to null.");
        Integer position = considerNonVisible ? visualizationSnapshot.getCanvasLayerVisualizationOrder(layer) : cache_mapLayerOrderNoInvisible.get(layer);
        if (position == null) throw new RuntimeException("Unknown layer: " + layer);

        return position;
    }

    List<NetworkLayer> getLayersInVisualizationOrder(boolean includeNonVisible)
    {
        Map<Integer, NetworkLayer> map = includeNonVisible ? MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()) : MapUtils.invertMap(cache_mapLayerOrderNoInvisible);

        List<NetworkLayer> res = new ArrayList<>();
        for (int vIndex = 0; vIndex < mediator.getNetPlan().getNumberOfLayers(); vIndex++)
            res.add(map.get(vIndex));

        return res;
    }

    Map<NetworkLayer, Integer> getLayerOrderMap(boolean includeNonVisible)
    {
        return includeNonVisible ? visualizationSnapshot.getMapCanvasLayerVisualizationOrder() : cache_mapLayerOrderNoInvisible;
    }

    Map<NetworkLayer, Boolean> getCanvasLayerVisibilityMap()
    {
        return Collections.unmodifiableMap(this.visualizationSnapshot.getMapCanvasLayerVisibility());
    }

    int getNumberOfVisibleLayers()
    {
        return cache_mapLayerOrderNoInvisible.size();
    }

    public static Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> generateCanvasDefaultVisualizationLayerInfo(NetPlan np)
    {
        final BidiMap<NetworkLayer, Integer> res_1 = new DualHashBidiMap<>();
        final Map<NetworkLayer, Boolean> res_2 = new HashMap<>();

        for (NetworkLayer layer : np.getNetworkLayers())
        {
            res_1.put(layer, res_1.size());
            res_2.put(layer, true);
        }
        return Pair.of(res_1, res_2);
    }

    public Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(Set<NetworkLayer> newNetworkLayers)
    {
        final Map<NetworkLayer, Boolean> oldLayerVisibilityMap = visualizationSnapshot.getMapCanvasLayerVisibility();
        final BidiMap<NetworkLayer, Integer> oldLayerOrderMap = new DualHashBidiMap<>(visualizationSnapshot.getMapCanvasLayerVisualizationOrder());
        final Map<NetworkLayer, Boolean> newLayerVisibilityMap = new HashMap<>();
        final BidiMap<NetworkLayer, Integer> newLayerOrderMap = new DualHashBidiMap<>();
        for (int oldVisibilityOrderIndex = 0; oldVisibilityOrderIndex < oldLayerOrderMap.size(); oldVisibilityOrderIndex++)
        {
            final NetworkLayer oldLayer = oldLayerOrderMap.inverseBidiMap().get(oldVisibilityOrderIndex);
            if (newNetworkLayers.contains(oldLayer))
            {
                newLayerOrderMap.put(oldLayer, newLayerVisibilityMap.size());
                newLayerVisibilityMap.put(oldLayer, oldLayerVisibilityMap.get(oldLayer));
            }
        }
        final Set<NetworkLayer> newLayersNotExistingBefore = Sets.difference(newNetworkLayers, oldLayerVisibilityMap.keySet());
        for (NetworkLayer newLayer : newLayersNotExistingBefore)
        {
            newLayerOrderMap.put(newLayer, newLayerVisibilityMap.size());
            newLayerVisibilityMap.put(newLayer, true); // new layers always visible
        }
        return Pair.of(newLayerOrderMap, newLayerVisibilityMap);
    }

    public int getInterLayerSpaceInPixels()
    {
        return interLayerSpaceInPixels;
    }

    public void setInterLayerSpaceInPixels(int interLayerSpaceInPixels)
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
        e.setEdgeStroke(resizedBasicStroke(a, linkWidthIncreaseFactorRespectToDefault), resizedBasicStroke(na, linkWidthIncreaseFactorRespectToDefault));
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
            if (!isLayerVisible(lowerLayer)) continue;
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

    private Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> getDownCoupling(Collection<Link> links)
    {
        final Set<Demand> res_1 = new HashSet<>();
        final Set<Pair<MulticastDemand, Node>> res_2 = new HashSet<>();
        for (Link link : links)
        {
            if (link.getCoupledDemand() != null) res_1.add(link.getCoupledDemand());
            else if (link.getCoupledMulticastDemand() != null)
                res_2.add(Pair.of(link.getCoupledMulticastDemand(), link.getDestinationNode()));
        }
        return Pair.of(res_1, res_2);

    }

    private Set<Link> getUpCoupling(Collection<Demand> demands, Collection<Pair<MulticastDemand, Node>> mDemands)
    {
        final Set<Link> res = new HashSet<>();
        if (demands != null)
            for (Demand d : demands)
                if (d.isCoupled()) res.add(d.getCoupledLink());
        if (mDemands != null)
            for (Pair<MulticastDemand, Node> md : mDemands)
            {
                if (md.getFirst().isCoupled())
                    res.add(md.getFirst().getCoupledLinks().stream().filter(e -> e.getDestinationNode() == md.getSecond()).findFirst().get());
//    			System.out.println(md.getFirst().getCoupledLinks().stream().map(e->e.getDestinationNode()).collect(Collectors.toList()));
//    			System.out.println(md.getSecond());
            }
        return res;
    }

    class VisualizationSnapshot
    {
        private NetPlan netPlan;

        private List<GUILayer> layerList;
        private Map<NetworkLayer, GUILayer> aux_layerToGUI;

        public VisualizationSnapshot(@NotNull NetPlan netPlan)
        {
            this.layerList = new ArrayList<>();
            this.aux_layerToGUI = new HashMap<>();
            this.resetSnapshot(netPlan);
        }

        private void resetSnapshot()
        {
            // Building GUILayers
            final List<NetworkLayer> networkLayers = netPlan.getNetworkLayers();

            layerList.clear();
            aux_layerToGUI.clear();
            for (NetworkLayer networkLayer : networkLayers)
            {
                GUILayer guiLayer = new GUILayer(networkLayer);
                layerList.add(guiLayer);
                aux_layerToGUI.put(networkLayer, guiLayer);
            }
        }

        public NetPlan getNetPlan()
        {
            return netPlan;
        }

        public void resetSnapshot(NetPlan netPlan)
        {
            this.netPlan = netPlan;
            resetSnapshot();
        }

        public Map<NetworkLayer, Integer> getMapCanvasLayerVisualizationOrder()
        {
            return layerList.stream().collect(Collectors.toMap(GUILayer::getAssociatedNetworkLayer, GUILayer::getLayerOrder));
        }

        public void setLayerVisualizationOrder(NetworkLayer layer, int order)
        {
            if (!aux_layerToGUI.containsKey(layer))
                throw new RuntimeException("Layer does not belong to current NetPlan...");
            aux_layerToGUI.get(layer).setLayerOrder(order);
        }

        public int getCanvasLayerVisualizationOrder(NetworkLayer layer)
        {
            return aux_layerToGUI.get(layer).getLayerOrder();
        }

        public Map<NetworkLayer, Boolean> getMapCanvasLayerVisibility()
        {
            return layerList.stream().collect(Collectors.toMap(GUILayer::getAssociatedNetworkLayer, GUILayer::isLayerVisible));
        }

        public void addLayerVisibility(NetworkLayer layer, boolean visibility)
        {
            if (!aux_layerToGUI.containsKey(layer))
                throw new RuntimeException("Layer does not belong to current NetPlan...");
            aux_layerToGUI.get(layer).setLayerVisibility(visibility);
        }

        public boolean getCanvasLayerVisibility(NetworkLayer layer)
        {
            return aux_layerToGUI.get(layer).isLayerVisible();
        }

        public Map<NetworkLayer, Boolean> getMapCanvasLinkVisibility()
        {
            return layerList.stream().collect(Collectors.toMap(GUILayer::getAssociatedNetworkLayer, GUILayer::isLinksVisible));
        }

        public boolean getCanvasLinkVisibility(NetworkLayer layer)
        {
            return aux_layerToGUI.get(layer).isLinksVisible();
        }

        public void addLinkVisibility(NetworkLayer layer, boolean visibility)
        {
            if (!aux_layerToGUI.containsKey(layer))
                throw new RuntimeException("Layer does not belong to current NetPlan...");
            aux_layerToGUI.get(layer).setLinksVisibility(visibility);
        }

        public VisualizationSnapshot copy()
        {
            final NetPlan npCopy = netPlan.copy();

            final VisualizationSnapshot snapshotCopy = new VisualizationSnapshot(npCopy);

            // Copy layer order
            for (Map.Entry<NetworkLayer, Integer> entry : getMapCanvasLayerVisualizationOrder().entrySet())
            {
                snapshotCopy.setLayerVisualizationOrder(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
            }

            for (Map.Entry<NetworkLayer, Boolean> entry : getMapCanvasLayerVisibility().entrySet())
            {
                snapshotCopy.addLayerVisibility(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
            }

            for (Map.Entry<NetworkLayer, Boolean> entry : getMapCanvasLinkVisibility().entrySet())
            {
                snapshotCopy.addLinkVisibility(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
            }

            return snapshotCopy;
        }

        public Triple<NetPlan, Map<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getSnapshotDefinition()
        {
            return Triple.unmodifiableOf(netPlan, getMapCanvasLayerVisualizationOrder(), getMapCanvasLayerVisibility());
        }
    }
}
