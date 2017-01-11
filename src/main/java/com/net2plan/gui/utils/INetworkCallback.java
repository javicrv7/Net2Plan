/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.utils;

import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTableNetworkElement;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants;
import com.net2plan.utils.Pair;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

/**
 * Interface to be implemented by any class dealing with network designs.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public interface INetworkCallback
{
    /**
     * Adds a new link.
     *
     * @param originNode      Origin node identifier
     * @param destinationNode Destination node identifier
     * @return Link identifier
     * @since 0.3.1
     */
    public long addLink(long originNode, long destinationNode);

    /**
     * Adds a new link.
     *
     * @param layer           Layer identifier
     * @param originNode      Origin node identifier
     * @param destinationNode Destination node identifier
     * @return Link identifier
     * @since 0.3.1
     */
    public long addLink(long layer, long originNode, long destinationNode);

    /**
     * Adds a new bidirectional link (one on each direction).
     *
     * @param originNode      Origin node identifier
     * @param destinationNode Destination node identifier
     * @return Link identifiers
     * @since 0.3.1
     */
    public Pair<Long, Long> addLinkBidirectional(long originNode, long destinationNode);

    /**
     * Adds a new bidirectional link (one on each direction).
     *
     * @param layer           Layer identifier
     * @param originNode      Origin node identifier
     * @param destinationNode Destination node identifier
     * @return Link identifiers
     * @since 0.3.1
     */
    public Pair<Long, Long> addLinkBidirectional(long layer, long originNode, long destinationNode);

    /**
     * Adds a node at the given coordinates.
     *
     * @param pos 2D position
     * @since 0.3.1
     */
    public void addNode(Point2D pos);

    /**
     * Returns the current network design.
     *
     * @return Current {@code NetPlan}
     * @since 0.3.1
     */
    public NetPlan getDesign();

    /**
     * Returns the current network plan.
     *
     * @return First item is the network plan, and the second one is the active layer
     * @since 0.3.0
     */
    public NetPlan getInitialDesign();

    /**
     * Returns the set of actions to be added to the popup menu for the network
     * canvas, where no element (either node or link) is selected.
     *
     * @param pos Network coordinates where the popup action was triggered
     * @return List of actions to be shown for the canvas
     * @since 0.3.1
     */
    public List<JComponent> getCanvasActions(Point2D pos);

    /**
     * Returns the set of actions to be added to the popup menu for links.
     *
     * @param link Link identifier
     * @param pos  Network coordinates where the popup action was triggered
     * @return List of actions to be shown for the given link
     * @since 0.3.1
     */
    public List<JComponent> getLinkActions(long link, Point2D pos);

    /**
     * Returns the set of actions to be added to the popup menu for nodes.
     *
     * @param node Node identifier
     * @param pos  Network coordinates where the popup action was triggered
     * @return List of actions to be shown for the given node
     * @since 0.3.1
     */
    public List<JComponent> getNodeActions(long node, Point2D pos);

    /**
     * Indicates whether or not the design is editable after loading.
     *
     * @return {@code true} if it is editable, and {@code false} otherwise
     * @since 0.3.1
     */
    public boolean isEditable();

    /**
     * Allows to execute some action whenever a layer is selected in the GUI.
     *
     * @param layer Layer identifier
     * @since 0.3.1
     */
    public void layerChanged(long layer);

    /**
     * It is called when a new network design is loaded.
     *
     * @param netPlan Network design
     * @since 0.3.1
     */
    public void loadDesign(NetPlan netPlan);

    /**
     * Loads a set of traffic demands from the given {@code NetPlan}.
     *
     * @param netPlan Network design containing a demand set
     * @since 0.3.1
     */
    public void loadTrafficDemands(NetPlan netPlan);

    /**
     * Moves the node to the given position.
     *
     * @param node Node identifier
     * @param pos  2D position
     * @since 0.3.1
     */
    public void moveNode(long node, Point2D pos);

    /**
     * Removes the given link.
     *
     * @param link Link identifier
     * @since 0.3.1
     */
    public void removeLink(long link);


    /**
     * Removes the given node.
     *
     * @param node Node identifier
     * @since 0.3.1
     */
    public void removeNode(long node);

    /**
     * Resets the current topology (i.e. remove any node/link).
     *
     * @since 0.3.1
     */
    public void reset();

    /**
     * Resets the current view (i.e. reset picked state).
     *
     * @since 0.3.1
     */
    public void resetView();

    /**
     * Shows the given link.
     *
     * @param link Link identifier
     * @since 0.3.1
     */
    public void showLink(long link);

    /**
     * Shows the given node.
     *
     * @param node Node identifier
     * @since 0.3.1
     */
    public void showNode(long node);

    /**
     * Shows the given demand.
     *
     * @param demand Demand identifier
     * @since 0.3.0
     */
    public void showDemand(long demand);

    /**
     * Shows the given multicast demand.
     *
     * @param demand Demand identifier
     * @since 0.3.1
     */
    public void showMulticastDemand(long demand);

    /**
     * Shows the given forwarding rule.
     *
     * @param demandLink Forwarding rule identifier (first: demand identifier, second: link identifier)
     * @since 0.3.0
     */
    public void showForwardingRule(Pair<Integer, Integer> demandLink);

    /**
     * Shows the given route.
     *
     * @param route Route identifier
     * @since 0.3.0
     */
    public void showRoute(long route);

    /**
     * Shows the given multicast tree.
     *
     * @param tree Route identifier
     * @since 0.3.1
     */
    public void showMulticastTree(long tree);

    /**
     * Shows the given SRG.
     *
     * @param srg SRG identifier
     * @since 0.3.0
     */
    public void showSRG(long srg);

    /**
     * Shows the given SRG.
     *
     * @param layer Layer identifier
     * @param srg   SRG identifier
     * @since 0.3.0
     */
    public void showSRG(long layer, long srg);

    /**
     * Updates the {@code NetPlan} view (i.e. node info, link info, and so on).
     *
     * @since 0.2.3
     */
    public void updateNetPlanView();

    /**
     * Updates the {@code NetPlan} warnings (over-subscribed links, blocked demands, and so on).
     *
     * @since 0.3.0
     */
    public void updateWarnings();

    // added by Pablo
//    public boolean allowDocumentUpdate ();
    // added by Pablo
    public TopologyPanel getTopologyPanel();

    // added by Pablo
    public boolean inOnlineSimulationMode();

    // added by Pablo
    public void showNetPlanView();
    // added by Pablo
    //public boolean allowLoadTrafficDemands();

    // added by Jorge
    public Map<Constants.NetworkElementType, AdvancedJTableNetworkElement> getTables();

}
