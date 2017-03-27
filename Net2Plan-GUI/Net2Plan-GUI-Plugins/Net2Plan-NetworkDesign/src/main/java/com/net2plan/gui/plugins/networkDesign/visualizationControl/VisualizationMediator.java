package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisualizationMediator
{
    private final CanvasControl canvasController;
    private final ElementSelector elementSelector;
    private final LayerControl layerController;
    private final NetPlanControl netPlanControl;
    private final PickTimeLineManager pickTimeLineManager;

    public VisualizationMediator(NetPlan currentNp, BidiMap<NetworkLayer, Integer> mapLayer2VisualizationOrder, Map<NetworkLayer, Boolean> layerVisibilityMap, int maxSizePickUndoList)
    {
        this.netPlanControl = new NetPlanControl(currentNp);
        this.canvasController = new CanvasControl(this);
        this.elementSelector = new ElementSelector(this);
        this.layerController = new LayerControl(this);
        this.pickTimeLineManager = new PickTimeLineManager();

        this.setCanvasLayerVisibilityAndOrder(currentNp, mapLayer2VisualizationOrder, layerVisibilityMap);
    }

    public NetPlan getNetPlan()
    {
        return netPlanControl.getNetPlan();
    }

    public boolean isNetPlanEditable()
    {
        return this.getNetPlan().isModifiable();
    }

    public boolean isInterLayerLinksShown()
    {
        return canvasController.isInterLayerLinksShown();
    }

    public boolean isNodeNamesShown()
    {
        return canvasController.isNodeNamesShown();
    }

    public boolean isLinkLabelsShown()
    {
        return canvasController.isLinkLabelsShown();
    }

    public boolean isNonConnectedNodesShown()
    {
        return canvasController.isNonConnectedNodesShown();
    }

    public boolean isLinksInNonActiveLayersShown()
    {
        return canvasController.isLinksInNonActiveLayerShown();
    }

    public boolean isWhatIfAnalysisOn()
    {
        return netPlanControl.isWhatIfAnalysisOn();
    }

    public boolean isLowerLayerPropagationShown()
    {
        return canvasController.isLowerLayerPropagationShown();
    }

    public boolean isUpperLayerPropagationShown()
    {
        return canvasController.isUpperLayerPropagationShown();
    }

    public boolean isCurrentLayerPropagationShown()
    {
        return canvasController.isThisLayerPropagationShown();
    }

    public boolean isGivenLayerLinksShown(@NotNull NetworkLayer networkLayer)
    {
        if (networkLayer == null) throw new NullPointerException();
        return layerController.isLinksShown(networkLayer);
    }

    public void setNodeNamesVisibility(final boolean showNodeNames)
    {
        canvasController.setNodeNamesVisibility(showNodeNames);
    }

    public void setInterLayerLinksVisibility(boolean showInterLayerLinks)
    {
        canvasController.setInterlayerLinksVisibility(showInterLayerLinks);
    }

    public void setLinkLabelsVisibility(final boolean showLinkLabels)
    {
        canvasController.setLinkLabelsVisibility(showLinkLabels);
    }

    public void setNonConnectedNodesVisibility(final boolean showNonConnectedNodes)
    {
        canvasController.setNonConnectedNodesVisibility(showNonConnectedNodes);
    }

    public void setLinksInNonActiveLayerVisibility(final boolean linksInNonActiveLayerVisibility)
    {
        canvasController.setLinksInNonActiveLayerVisibility(linksInNonActiveLayerVisibility);
    }

    public void setWhatIfAnalysisState(final boolean state)
    {
        netPlanControl.setWhatIfAnalysisState(state);
    }

    public void setLowerLayerPropagationVisibility(final boolean lowerLayerPropagationVisibility)
    {
        canvasController.setLowerLayerPropagationVisibility(lowerLayerPropagationVisibility);
    }

    public void setUpperLayerPropagationVisibility(final boolean upperLayerPropagationVisibility)
    {
        canvasController.setUpperLayerPropagationVisibility(upperLayerPropagationVisibility);
    }

    public void setCurrentLayerPropagationVisibility(final boolean currentLayerPropagationVisibility)
    {
        canvasController.setThisLayerPropagationVisibility(currentLayerPropagationVisibility);
    }

    public void setInterLayerDistance(final int distanceInPixels)
    {
        canvasController.setInterLayerSpaceInPixels(distanceInPixels);
    }

    public void setGivenLayerLinksVisibility(@NotNull final NetworkLayer networkLayer, final boolean linkVisibility)
    {
        if (networkLayer == null) throw new NullPointerException();
        layerController.setLinksVisibility(networkLayer, linkVisibility);
    }

    public void updateTableRowFilter(@NotNull ITableRowFilter tableRowFilter)
    {
        if (tableRowFilter == null) throw new NullPointerException();
        netPlanControl.updateTableRowFilter(tableRowFilter);
    }

    public ITableRowFilter getTableRowFilter()
    {
        return netPlanControl.getTableRowFilter();
    }

    public ImageIcon getIcon(@Nullable final URL url, int height, Color borderColor)
    {
        return netPlanControl.getIcon(url, height, borderColor).getFirst();
    }

    public boolean isVisible(@NotNull final GUINode node)
    {
        if (node == null) throw new NullPointerException();
        return canvasController.isVisible(node);
    }

    public boolean isVisible(@NotNull final GUILink link)
    {
        if (link == null) throw new NullPointerException();
        return canvasController.isVisible(link);
    }

    public boolean isVisible(@NotNull final Node node)
    {
        if (node == null) throw new NullPointerException();
        return !canvasController.isVisible(node);
    }

    public boolean isVisible(@NotNull final Link link)
    {
        if (link == null) throw new NullPointerException();
        return !canvasController.isVisible(link);
    }

    public boolean isVisible(@NotNull final NetworkLayer layer)
    {
        if (layer == null) throw new NullPointerException();
        return layerController.isLayerVisible(layer);
    }

    public int getNumberOfLayers(final boolean considerNonVisible)
    {
        return considerNonVisible ? getNetPlan().getNumberOfLayers() : layerController.getNumberOfVisibleLayers();
    }

    public void show(@NotNull final Node node)
    {
        if (node == null) throw new NullPointerException();
        canvasController.show(node);
    }

    public void show(@NotNull final Link link)
    {
        if (link == null) throw new NullPointerException();
        canvasController.show(link);
    }

    public void show(@NotNull final NetworkLayer networkLayer)
    {
        if (networkLayer == null) throw new NullPointerException();
        layerController.setLayerVisibility(networkLayer, true);
    }

    public void hide(@NotNull final Node node)
    {
        if (node == null) throw new NullPointerException();
        canvasController.hide(node);
    }

    public void hide(@NotNull final Link link)
    {
        if (link == null) throw new NullPointerException();
        canvasController.hide(link);
    }

    public void hide(@NotNull final NetworkLayer networkLayer)
    {
        if (networkLayer == null) throw new NullPointerException();
        layerController.setLayerVisibility(networkLayer, false);
    }

    public int getLayerOrderPosition(@NotNull final NetworkLayer layer, final boolean considerNonVisible)
    {
        if (layer == null) throw new NullPointerException();
        return layerController.getLayerOrderPosition(layer, considerNonVisible);
    }

    public NetworkLayer getLayerAtPosition(final int orderPosition, final boolean considerNonVisible)
    {
        return layerController.getLayerAtPosition(orderPosition, considerNonVisible);
    }

    public List<NetworkLayer> getLayersInOrder(final boolean considerNonVisible)
    {
        return layerController.getLayersInVisualizationOrder(considerNonVisible);
    }

    public Map<NetworkLayer, Integer> getLayerOrderMap(final boolean considerNonVisible)
    {
        return layerController.getLayerOrderMap(considerNonVisible);
    }

    public Map<NetworkLayer, Boolean> getLayerVisibilityMap()
    {
        return layerController.getLayerVisibilityMap();
    }

    public Set<GUINode> getAllGUINodes()
    {
        return canvasController.getAllGUINodes();
    }

    public Set<GUILink> getAllGUILinks(final boolean includeRegularLinks, final boolean includeLayerLinks)
    {
        return canvasController.getAllGUILinks(includeRegularLinks, includeLayerLinks);
    }

    public List<GUINode> getStackedGUINodes(@NotNull final Node node)
    {
        if (node == null) throw new NullPointerException();
        return canvasController.getStackedGUINodes(node);
    }

    public GUINode getAssociatedGUINode(@NotNull final NetworkLayer layer, @NotNull Node node)
    {
        if (layer == null || node == null) throw new NullPointerException();
        return canvasController.getAssociatedGUINode(node, layer);
    }

    public GUILink getAssociatedGUILink(@NotNull Link link)
    {
        if (link == null) throw new NullPointerException();
        return canvasController.getAssociatedGUILink(link);
    }

    public GUILink getIntraNodeGUILink(@NotNull final NetworkLayer from, @NotNull Node node, @NotNull NetworkLayer to)
    {
        if (from == null || node == null || to == null) throw new NullPointerException();
        return canvasController.getIntraNodeGUILink(node, from, to);
    }

    public List<GUILink> getIntraNodeGUISequence(@NotNull final NetworkLayer from, @NotNull Node node, @NotNull NetworkLayer to)
    {
        if (from == null || node == null || to == null) throw new NullPointerException();
        return canvasController.getIntraNodeGUILinkSequence(node, from, to);
    }

    public Set<GUILink> getIntraNodeGUILinks(@NotNull final Node node)
    {
        return canvasController.getIntraNodeGUILinks(node);
    }

    public int getInterLayerDistance()
    {
        return canvasController.getInterLayerSpaceInPixels();
    }

    public void increaseFontSize()
    {
        canvasController.increaseFontSizeAll();
    }

    public void increaseNodeSize()
    {
        canvasController.increaseNodeSizeAll();
    }

    public void increaseLinkSize()
    {
        canvasController.increaseLinkSizeAll();
    }

    public void decreaseFontSize()
    {
        canvasController.decreaseFontSizeAll();
    }

    public void decreaseNodeSize()
    {
        canvasController.decreaseNodeSizeAll();
    }

    public void decreaseLinkSize()
    {
        canvasController.decreaseLinkSizeAll();
    }

    public boolean isElementPicked(@NotNull NetworkElement networkElement)
    {
        if (networkElement == null) throw new NullPointerException();
        return elementSelector.isElementPicked(networkElement);
    }

    public void pickElement(@NotNull NetworkElement element)
    {
        if (element == null) throw new NullPointerException();
        elementSelector.pickElement(element);
    }

    @Nullable
    public Object getPickedElement()
    {
        return elementSelector.getPickedElement();
    }

    public void resetPickState()
    {
        elementSelector.resetPickedState();
    }

    public Object getPickNavigationBackElement()
    {
        return pickTimeLineManager.getPickNavigationBackElement();
    }

    public Object getPickNavigationForwardElement()
    {
        return pickTimeLineManager.getPickNavigationForwardElement();
    }

    public void addElementToPickTimeline(@NotNull final Object object)
    {
        if (object == null) throw new NullPointerException();
        if (!(object instanceof NetworkElement) && !(object instanceof Pair)) throw new RuntimeException();

        if (object instanceof NetworkElement)
        {
            pickTimeLineManager.addElement(this.getNetPlan(), (NetworkElement) object);
        } else if (object instanceof Pair)
        {
            Pair aux = (Pair) object;
            if (!(aux.getFirst() instanceof Demand) && !(aux.getSecond() instanceof Link)) throw new RuntimeException();
            pickTimeLineManager.addElement(this.getNetPlan(), aux);
        }
    }

    void setCurrentDefaultEdgeStroke(final GUILink e, final BasicStroke a, final BasicStroke na)
    {
        canvasController.setCurrentDefaultEdgeStroke(e, a, na);
    }

//    public static void checkNpToVsConsistency(VisualizationMediator vs, NetPlan np)
//    {
//        if (vs.getNetPlan() != np)
//            throw new RuntimeException("inputVs.currentNp:" + vs.getNetPlan().hashCode() + ", inputNp: " + np.hashCode());
//        for (Node n : vs.nodesToHideInCanvasAsMandatedByUserInTable)
//            if (n.getNetPlan() != np) throw new RuntimeException();
//        for (Link e : vs.linksToHideInCanvasAsMandatedByUserInTable)
//            if (e.getNetPlan() != np) throw new RuntimeException();
//        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLayerVisualizationOrder().keySet())
//            if (e.getNetPlan() != np) throw new RuntimeException();
//        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLayerVisibility().keySet())
//            if (e.getNetPlan() != np) throw new RuntimeException();
//        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLinkVisibility().keySet())
//            if (e.getNetPlan() != np) throw new RuntimeException();
//        for (Node e : vs.cache_canvasIntraNodeGUILinks.keySet()) if (e.getNetPlan() != np) throw new RuntimeException();
//        for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values())
//            for (GUILink e : s)
//                if (e.getOriginNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
//        for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values())
//            for (GUILink e : s)
//                if (e.getDestinationNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
//        for (Link e : vs.cache_canvasRegularLinkMap.keySet()) if (e.getNetPlan() != np) throw new RuntimeException();
//        for (GUILink e : vs.cache_canvasRegularLinkMap.values())
//            if (e.getAssociatedNetPlanLink().getNetPlan() != np) throw new RuntimeException();
//        for (NetworkLayer e : vs.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.keySet())
//            if (e.getNetPlan() != np) throw new RuntimeException();
//        for (Node e : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.keySet())
//            if (e.getNetPlan() != np) throw new RuntimeException();
//        for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values())
//            for (GUILink gl : map.values())
//                if (gl.getOriginNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
//        for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values())
//            for (GUILink gl : map.values())
//                if (gl.getDestinationNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
//        for (Node e : vs.cache_mapNode2ListVerticallyStackedGUINodes.keySet())
//            if (e.getNetPlan() != np) throw new RuntimeException();
//        for (List<GUINode> list : vs.cache_mapNode2ListVerticallyStackedGUINodes.values())
//            for (GUINode gn : list) if (gn.getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
//        if (vs.pickedElementNotFR != null) if (vs.pickedElementNotFR.getNetPlan() != np) throw new RuntimeException();
//        if (vs.pickedElementFR != null)
//            if (vs.pickedElementFR.getFirst().getNetPlan() != np) throw new RuntimeException();
//        if (vs.pickedElementFR != null)
//            if (vs.pickedElementFR.getSecond().getNetPlan() != np) throw new RuntimeException();
//    }

    // TODO: Test
//    private void checkCacheConsistency()
//    {
//        for (Node n : currentNp.getNodes())
//        {
//            assertTrue(cache_canvasIntraNodeGUILinks.get(n) != null);
//            assertTrue(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n) != null);
//            assertTrue(cache_mapNode2ListVerticallyStackedGUINodes.get(n) != null);
//            for (Entry<Pair<Integer, Integer>, GUILink> entry : cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).entrySet())
//            {
//                final int fromLayer = entry.getKey().getFirst();
//                final int toLayer = entry.getKey().getSecond();
//                final GUILink gl = entry.getValue();
//                assertTrue(gl.isIntraNodeLink());
//                assertTrue(gl.getOriginNode().getAssociatedNode() == n);
//                assertTrue(getCanvasVisualizationOrderRemovingNonVisible(gl.getOriginNode().getLayer()) == fromLayer);
//                assertTrue(getCanvasVisualizationOrderRemovingNonVisible(gl.getDestinationNode().getLayer()) == toLayer);
//            }
//            assertEquals(new HashSet<>(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).values()), cache_canvasIntraNodeGUILinks.get(n));
//            for (GUILink gl : cache_canvasIntraNodeGUILinks.get(n))
//            {
//                assertTrue(gl.isIntraNodeLink());
//                assertEquals(gl.getOriginNode().getAssociatedNode(), n);
//                assertEquals(gl.getDestinationNode().getAssociatedNode(), n);
//            }
//            assertEquals(cache_mapNode2ListVerticallyStackedGUINodes.get(n).size(), getNumberOfVisibleLayers());
//            int indexLayer = 0;
//            for (GUINode gn : cache_mapNode2ListVerticallyStackedGUINodes.get(n))
//            {
//                assertEquals(gn.getLayer(), cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(indexLayer));
//                assertEquals(getCanvasVisualizationOrderRemovingNonVisible(gn.getLayer()), indexLayer++);
//                assertEquals(gn.getAssociatedNode(), n);
//            }
//        }
//    }
}
