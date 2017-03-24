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
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MapUtils;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
public class LayerController
{
    private final VisualizationMediator mediator;
    private final VisualizationSnapshot visualizationSnapshot;

    private Map<NetworkLayer, Integer> cache_mapLayerOrderNoInvisible;

    LayerController(@NotNull final VisualizationMediator mediator)
    {
        this.mediator = mediator;
        this.visualizationSnapshot = new VisualizationSnapshot(mediator.getNetPlan());

        this.cache_mapLayerOrderNoInvisible = new HashMap<>();
    }

    public NetPlan getNetPlan()
    {
        return visualizationSnapshot.getNetPlan();
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

    @Nullable
    public NetworkLayer getLayerAtPosition(final int layerPosition, final boolean considerNonVisible)
    {
        if (layerPosition < 0 || layerPosition >= getCanvasNumberOfVisibleLayers) return null;

        return considerNonVisible ? 
    }

    public NetworkLayer getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(int trueVisualizationOrder)
    {
        if (trueVisualizationOrder < 0) throw new RuntimeException("");
        if (trueVisualizationOrder >= getCanvasNumberOfVisibleLayers()) throw new RuntimeException("");
        return cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(trueVisualizationOrder);
    }

    public NetworkLayer getCanvasNetworkLayerAtVisualizationOrderNotRemovingNonVisible(int visualizationOrder)
    {
        if (visualizationOrder < 0) throw new RuntimeException("");
        if (visualizationOrder >= mediator.getNetPlan().getNumberOfLayers())
            throw new RuntimeException("");
        return MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()).get(visualizationOrder);
    }

    public int getCanvasVisualizationOrderRemovingNonVisible(NetworkLayer layer)
    {
        Integer res = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(layer);
        if (res == null) throw new RuntimeException("");
        return res;
    }

    public int getCanvasVisualizationOrderNotRemovingNonVisible(NetworkLayer layer)
    {
        Integer res = visualizationSnapshot.getCanvasLayerVisualizationOrder(layer);
        if (res == null) throw new RuntimeException();
        return res;
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
        final Map<NetworkLayer, Boolean> oldLayerVisibilityMap = getCanvasLayerVisibilityMap();
        final BidiMap<NetworkLayer, Integer> oldLayerOrderMap = new DualHashBidiMap<>(getCanvasLayerOrderIndexMap(true));
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

    public void setCanvasLayerVisibilityAndOrder(NetPlan newCurrentNetPlan, Map<NetworkLayer, Integer> newLayerOrderMap,
                                                 Map<NetworkLayer, Boolean> newLayerVisibilityMap)
    {
        if (newCurrentNetPlan == null) throw new RuntimeException("Trying to update an empty topology");

        final Map<NetworkLayer, Boolean> mapCanvasLinkVisibility = this.visualizationSnapshot.getMapCanvasLinkVisibility();

        this.visualizationSnapshot.resetSnapshot(newCurrentNetPlan);

        if (mediator.getNetPlan() != newCurrentNetPlan)
        {
            tableRowFilter = null;
            nodesToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
            linksToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        }

        // Updating visualization snapshot
        if (newLayerOrderMap != null)
        {
            for (Map.Entry<NetworkLayer, Integer> entry : newLayerOrderMap.entrySet())
            {
                visualizationSnapshot.setLayerVisualizationOrder(entry.getKey(), entry.getValue());
            }
        }

        if (newLayerVisibilityMap != null)
        {
            for (Map.Entry<NetworkLayer, Boolean> entry : newLayerVisibilityMap.entrySet())
            {
                visualizationSnapshot.addLayerVisibility(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<NetworkLayer, Boolean> entry : mapCanvasLinkVisibility.entrySet())
        {
            if (!mediator.getNetPlan().getNetworkLayers().contains(entry.getKey())) continue;
            visualizationSnapshot.addLinkVisibility(entry.getKey(), entry.getValue());
        }

        /* implicitly we restart the picking state */
        elementSelector.resetPickedState();

        this.cache_canvasIntraNodeGUILinks = new HashMap<>();
        this.cache_canvasRegularLinkMap = new HashMap<>();
        this.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible = new DualHashBidiMap<>();
        this.cache_mapNode2IntraNodeCanvasGUILinkMap = new HashMap<>();
        this.cache_mapNode2ListVerticallyStackedGUINodes = new HashMap<>();
        for (int layerVisualizationOrderIncludingNonVisible = 0; layerVisualizationOrderIncludingNonVisible < mediator.getNetPlan().getNumberOfLayers(); layerVisualizationOrderIncludingNonVisible++)
        {
            final NetworkLayer layer = MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()).get(layerVisualizationOrderIncludingNonVisible);
            if (isLayerVisible(layer))
                cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.put(layer, cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size());
        }
        for (Node n : mediator.getNetPlan().getNodes())
        {
            List<GUINode> guiNodesThisNode = new ArrayList<>();
            cache_mapNode2ListVerticallyStackedGUINodes.put(n, guiNodesThisNode);
            Set<GUILink> intraNodeGUILinksThisNode = new HashSet<>();
            cache_canvasIntraNodeGUILinks.put(n, intraNodeGUILinksThisNode);
            Map<Pair<Integer, Integer>, GUILink> thisNodeInterLayerLinksInfoMap = new HashMap<>();
            cache_mapNode2IntraNodeCanvasGUILinkMap.put(n, thisNodeInterLayerLinksInfoMap);
            for (int trueVisualizationOrderIndex = 0; trueVisualizationOrderIndex < cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size(); trueVisualizationOrderIndex++)
            {
                final NetworkLayer newLayer = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(trueVisualizationOrderIndex);
                final double iconHeightIfNotActive = nodeSizeIncreaseFactorRespectToDefault * (getNetPlan().getNumberOfNodes() > 100 ? VisualizationConstants.DEFAULT_GUINODE_SHAPESIZE_MORETHAN100NODES : VisualizationConstants.DEFAULT_GUINODE_SHAPESIZE);
                final GUINode gn = new GUINode(n, newLayer, iconHeightIfNotActive);
                guiNodesThisNode.add(gn);
                if (trueVisualizationOrderIndex > 0)
                {
                    final GUINode lowerLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex - 1);
                    final GUINode upperLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex);
                    if (upperLayerGNode != gn) throw new RuntimeException();
                    final GUILink glLowerToUpper = new GUILink(null, lowerLayerGNode, gn,
                            resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault),
                            resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                    final GUILink glUpperToLower = new GUILink(null, gn, lowerLayerGNode,
                            resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault),
                            resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                    intraNodeGUILinksThisNode.add(glLowerToUpper);
                    intraNodeGUILinksThisNode.add(glUpperToLower);
                    thisNodeInterLayerLinksInfoMap.put(Pair.of(trueVisualizationOrderIndex - 1, trueVisualizationOrderIndex), glLowerToUpper);
                    thisNodeInterLayerLinksInfoMap.put(Pair.of(trueVisualizationOrderIndex, trueVisualizationOrderIndex - 1), glUpperToLower);
                }
            }
        }
        for (int trueVisualizationOrderIndex = 0; trueVisualizationOrderIndex < cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size(); trueVisualizationOrderIndex++)
        {
            final NetworkLayer layer = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(trueVisualizationOrderIndex);
            for (Link e : mediator.getNetPlan().getLinks(layer))
            {
                final GUINode gn1 = cache_mapNode2ListVerticallyStackedGUINodes.get(e.getOriginNode()).get(trueVisualizationOrderIndex);
                final GUINode gn2 = cache_mapNode2ListVerticallyStackedGUINodes.get(e.getDestinationNode()).get(trueVisualizationOrderIndex);
                final GUILink gl1 = new GUILink(e, gn1, gn2,
                        resizedBasicStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER, linkWidthIncreaseFactorRespectToDefault),
                        resizedBasicStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                cache_canvasRegularLinkMap.put(e, gl1);
            }
        }
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

        public com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationSnapshot copy()
        {
            final NetPlan npCopy = netPlan.copy();

            final com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationSnapshot snapshotCopy = new com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationSnapshot(npCopy);

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
