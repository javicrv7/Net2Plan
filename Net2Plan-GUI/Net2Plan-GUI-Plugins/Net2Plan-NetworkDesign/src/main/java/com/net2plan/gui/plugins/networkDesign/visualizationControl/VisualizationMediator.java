package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import edu.uci.ics.jung.visualization.FourPassImageShaper;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MapUtils;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class VisualizationMediator
{
    private static Map<Triple<URL, Integer, Color>, Pair<ImageIcon, Shape>> databaseOfAlreadyReadIcons = new HashMap<>(); // for each url, height, and border color, an image

    private final PickTimeLineManager pickTimeLineManager;
    private final CanvasController canvasController;
    private final ElementSelector elementSelector;

    private boolean whatIfAnalysisActive;

    private ITableRowFilter tableRowFilter;

    private int interLayerSpaceInPixels;
    private Set<Node> nodesToHideInCanvasAsMandatedByUserInTable;
    private Set<Link> linksToHideInCanvasAsMandatedByUserInTable;

    private VisualizationSnapshot visualizationSnapshot;
    private float linkWidthIncreaseFactorRespectToDefault;
    private float nodeSizeIncreaseFactorRespectToDefault;

    /* These need is recomputed inside a rebuild */
    private Map<Node, Set<GUILink>> cache_canvasIntraNodeGUILinks;
    private Map<Link, GUILink> cache_canvasRegularLinkMap;
    private BidiMap<NetworkLayer, Integer> cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible; // as many elements as visible layers
    private Map<Node, Map<Pair<Integer, Integer>, GUILink>> cache_mapNode2IntraNodeCanvasGUILinkMap; // integers are orders of REAL VISIBLE LAYERS
    private Map<Node, List<GUINode>> cache_mapNode2ListVerticallyStackedGUINodes;

    public NetPlan getNetPlan()
    {
        return visualizationSnapshot.getNetPlan();
    }

    public VisualizationMediator(NetPlan currentNp, BidiMap<NetworkLayer, Integer> mapLayer2VisualizationOrder, Map<NetworkLayer, Boolean> layerVisibilityMap, int maxSizePickUndoList)
    {
        this.visualizationSnapshot = new VisualizationSnapshot(currentNp);

        this.canvasController = new CanvasController(this);
        this.elementSelector = new ElementSelector(this);
        this.pickTimeLineManager = new PickTimeLineManager();

        this.nodesToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        this.linksToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        this.interLayerSpaceInPixels = 50;
        this.tableRowFilter = null;
        this.linkWidthIncreaseFactorRespectToDefault = 1;
        this.nodeSizeIncreaseFactorRespectToDefault = 1;

        this.whatIfAnalysisActive = false;

        this.setCanvasLayerVisibilityAndOrder(currentNp, mapLayer2VisualizationOrder, layerVisibilityMap);
    }

    public boolean isWhatIfAnalysisActive()
    {
        return whatIfAnalysisActive;
    }

    public void setWhatIfAnalysisState(boolean isWhatIfAnalysisActive)
    {
        this.whatIfAnalysisActive = isWhatIfAnalysisActive;
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

    /**
     * @return the interLayerSpaceInNetPlanCoordinates
     */
    public int getInterLayerSpaceInPixels()
    {
        return interLayerSpaceInPixels;
    }

    /**
     */
    public void setInterLayerSpaceInPixels(int interLayerSpaceInPixels)
    {
        this.interLayerSpaceInPixels = interLayerSpaceInPixels;
    }

    /**
     * @return the isNetPlanEditable
     */
    public boolean isNetPlanEditable()
    {
        return this.getNetPlan().isModifiable();
    }

    VisualizationSnapshot getSnapshot()
    {
        return visualizationSnapshot;
    }

    /**
     * Implicitly it produces a reset of the picked state
     *
     * @param newCurrentNetPlan
     * @param newLayerOrderMap
     * @param newLayerVisibilityMap
     */
    public void setCanvasLayerVisibilityAndOrder(NetPlan newCurrentNetPlan, Map<NetworkLayer, Integer> newLayerOrderMap,
                                                 Map<NetworkLayer, Boolean> newLayerVisibilityMap)
    {
        if (newCurrentNetPlan == null) throw new RuntimeException("Trying to update an empty topology");

        final Map<NetworkLayer, Boolean> mapCanvasLinkVisibility = this.visualizationSnapshot.getMapCanvasLinkVisibility();

        this.visualizationSnapshot.resetSnapshot(newCurrentNetPlan);

        if (this.getNetPlan() != newCurrentNetPlan)
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
            if (!this.getNetPlan().getNetworkLayers().contains(entry.getKey())) continue;
            visualizationSnapshot.addLinkVisibility(entry.getKey(), entry.getValue());
        }

        /* implicitly we restart the picking state */
        elementSelector.resetPickedState();

        this.cache_canvasIntraNodeGUILinks = new HashMap<>();
        this.cache_canvasRegularLinkMap = new HashMap<>();
        this.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible = new DualHashBidiMap<>();
        this.cache_mapNode2IntraNodeCanvasGUILinkMap = new HashMap<>();
        this.cache_mapNode2ListVerticallyStackedGUINodes = new HashMap<>();
        for (int layerVisualizationOrderIncludingNonVisible = 0; layerVisualizationOrderIncludingNonVisible < this.getNetPlan().getNumberOfLayers(); layerVisualizationOrderIncludingNonVisible++)
        {
            final NetworkLayer layer = MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()).get(layerVisualizationOrderIncludingNonVisible);
            if (isLayerVisibleInCanvas(layer))
                cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.put(layer, cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size());
        }
        for (Node n : this.getNetPlan().getNodes())
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
            for (Link e : this.getNetPlan().getLinks(layer))
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

    /**
     * To call when the topology has new/has removed any link or node, but keeping the same layers.
     * The topology is remade, which involves implicitly a reset of the view
     */
    public void recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals()
    {
        this.setCanvasLayerVisibilityAndOrder(this.getNetPlan(), null, null);
    }

    public void setCanvasLayerVisibility(final NetworkLayer layer, final boolean isVisible)
    {
        if (!this.getNetPlan().getNetworkLayers().contains(layer)) throw new RuntimeException();
        BidiMap<NetworkLayer, Integer> new_layerVisiblityOrderMap = new DualHashBidiMap<>(this.visualizationSnapshot.getMapCanvasLayerVisualizationOrder());
        Map<NetworkLayer, Boolean> new_layerVisibilityMap = new HashMap<>(this.visualizationSnapshot.getMapCanvasLayerVisibility());
        new_layerVisibilityMap.put(layer, isVisible);
        setCanvasLayerVisibilityAndOrder(this.getNetPlan(), new_layerVisiblityOrderMap, new_layerVisibilityMap);
    }

    public boolean isLayerVisibleInCanvas(final NetworkLayer layer)
    {
        return visualizationSnapshot.getCanvasLayerVisibility(layer);
    }

    public void setLayerLinksVisibilityInCanvas(final NetworkLayer layer, final boolean showLinks)
    {
        if (!this.getNetPlan().getNetworkLayers().contains(layer)) throw new RuntimeException();
        visualizationSnapshot.getMapCanvasLinkVisibility().put(layer, showLinks);
    }

    public boolean isCanvasLayerLinksShown(final NetworkLayer layer)
    {
        return visualizationSnapshot.getCanvasLinkVisibility(layer);
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
        if (visualizationOrder >= this.getNetPlan().getNumberOfLayers())
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

    private void drawDownPropagationInterLayerLinks(Set<Link> links, Paint color)
    {
        for (Link link : links)
        {
            final GUILink gl = getCanvasAssociatedGUILink(link);
            if (gl == null) continue;
            if (!link.isCoupled()) continue;
            final boolean isCoupledToDemand = link.getCoupledDemand() != null;
            final NetworkLayer upperLayer = link.getLayer();
            final NetworkLayer lowerLayer = isCoupledToDemand ? link.getCoupledDemand().getLayer() : link.getCoupledMulticastDemand().getLayer();
            if (!isLayerVisibleInCanvas(lowerLayer)) continue;
            for (GUILink interLayerLink : getCanvasIntraNodeGUILinkSequence(link.getOriginNode(), upperLayer, lowerLayer))
            {
                setCurrentDefaultEdgeStroke(interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
            for (GUILink interLayerLink : getCanvasIntraNodeGUILinkSequence(link.getDestinationNode(), lowerLayer, upperLayer))
            {
                setCurrentDefaultEdgeStroke(interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
        }
    }

    private void drawColateralLinks(Collection<Link> links, Paint colorIfNotFailedLink)
    {
        for (Link link : links)
        {
            final GUILink glColateral = getCanvasAssociatedGUILink(link);
            if (glColateral == null) continue;
            setCurrentDefaultEdgeStroke(glColateral, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER);
            final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : colorIfNotFailedLink;
            glColateral.setEdgeDrawPaint(color);
            glColateral.setShownSeparated(true);
            glColateral.setHasArrow(true);
        }
    }

    private static Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> getDownCoupling(Collection<Link> links)
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

    private static Set<Link> getUpCoupling(Collection<Demand> demands, Collection<Pair<MulticastDemand, Node>> mDemands)
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

    public Object getPickNavigationBackElement()
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

    private static BasicStroke resizedBasicStroke(BasicStroke a, float multFactorSize)
    {
        if (multFactorSize == 1) return a;
        return new BasicStroke(a.getLineWidth() * multFactorSize, a.getEndCap(), a.getLineJoin(), a.getMiterLimit(), a.getDashArray(), a.getDashPhase());
    }

    private void setCurrentDefaultEdgeStroke(GUILink e, BasicStroke a, BasicStroke na)
    {
        e.setEdgeStroke(resizedBasicStroke(a, linkWidthIncreaseFactorRespectToDefault), resizedBasicStroke(na, linkWidthIncreaseFactorRespectToDefault));
    }
}
