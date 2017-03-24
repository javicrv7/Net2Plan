package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import edu.uci.ics.jung.visualization.FourPassImageShaper;
import org.apache.commons.collections15.BidiMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;

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


    public int getNumberOfVisibleLayers()
    {
        return canvasController.getCanvasNumberOfVisibleLayers();
    }

1    public Object getPickNavigationBackElement()
    {
        return pickTimeLineManager.getPickNavigationBackElement();
    }

    public Object getPickNavigationForwardElement()
    {
        return pickTimeLineManager.getPickNavigationForwardElement();
    }

    public static void checkNpToVsConsistency(VisualizationMediator vs, NetPlan np)
    {
        if (vs.getNetPlan() != np)
            throw new RuntimeException("inputVs.currentNp:" + vs.getNetPlan().hashCode() + ", inputNp: " + np.hashCode());
        for (Node n : vs.nodesToHideInCanvasAsMandatedByUserInTable)
            if (n.getNetPlan() != np) throw new RuntimeException();
        for (Link e : vs.linksToHideInCanvasAsMandatedByUserInTable)
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLayerVisualizationOrder().keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLayerVisibility().keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLinkVisibility().keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (Node e : vs.cache_canvasIntraNodeGUILinks.keySet()) if (e.getNetPlan() != np) throw new RuntimeException();
        for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values())
            for (GUILink e : s)
                if (e.getOriginNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values())
            for (GUILink e : s)
                if (e.getDestinationNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Link e : vs.cache_canvasRegularLinkMap.keySet()) if (e.getNetPlan() != np) throw new RuntimeException();
        for (GUILink e : vs.cache_canvasRegularLinkMap.values())
            if (e.getAssociatedNetPlanLink().getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (Node e : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values())
            for (GUILink gl : map.values())
                if (gl.getOriginNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values())
            for (GUILink gl : map.values())
                if (gl.getDestinationNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Node e : vs.cache_mapNode2ListVerticallyStackedGUINodes.keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (List<GUINode> list : vs.cache_mapNode2ListVerticallyStackedGUINodes.values())
            for (GUINode gn : list) if (gn.getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        if (vs.pickedElementNotFR != null) if (vs.pickedElementNotFR.getNetPlan() != np) throw new RuntimeException();
        if (vs.pickedElementFR != null)
            if (vs.pickedElementFR.getFirst().getNetPlan() != np) throw new RuntimeException();
        if (vs.pickedElementFR != null)
            if (vs.pickedElementFR.getSecond().getNetPlan() != np) throw new RuntimeException();
    }

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
//            assertEquals(cache_mapNode2ListVerticallyStackedGUINodes.get(n).size(), getCanvasNumberOfVisibleLayers());
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