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

import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Pair;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections15.MapUtils;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.util.*;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
public class VisualizationUpdater
{
    private final VisualizationMediator mediator;

    VisualizationUpdater(@NotNull final VisualizationMediator mediator)
    {
        this.mediator = mediator;
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
}
