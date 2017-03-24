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


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.specificTables;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationMediator;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_layer extends AdvancedJTable_networkElement
{
    public static final String netPlanViewTabName = "Layers";
    public static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Name", "Routing type", "Number of links",
            "Number of demands", "Number of multicast demands", "Number of routes", "Number of forwarding rules", "Number of backup routes",
            "Number of multicast trees", "Description", "Link capacity units name", "Demand traffic units name", "Tags", "Attributes");
    public static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Name", "Routing type", "Number of links", "Number of demands", "Number of multicast demands", "Number of routes", "Number of forwarding rules", "Number of routes that are designated as backup of other route", "Number of multicast trees", "Description", "Link capacity units name", "Demand traffic units name", "Tags", "Attributes");
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_NAME = 2;
    public static final int COLUMN_ROUTINGTYPE = 3;
    public static final int COLUMN_NUMLINKS = 4;
    public static final int COLUMN_NUMDEMANDS = 5;
    public static final int COLUMN_NUMMULTICASTDEMANDS = 6;
    public static final int COLUMN_NUMROUTES = 7;
    public static final int COLUMN_NUMFORWARDINRULES = 8;
    public static final int COLUMN_NUMSEGMENTS = 9;
    public static final int COLUMN_NUMTREES = 10;
    public static final int COLUMN_DESCRIPTION = 11;
    public static final int COLUMN_LINKCAPUNITS = 12;
    public static final int COLUMN_DEMANDTRAFUNITS = 13;
    public static final int COLUMN_TAGS = 14;
    public static final int COLUMN_ATTRIBUTES = 15;


    public AdvancedJTable_layer(final GUINetworkDesign networkViewer) {
        super(createTableModel(networkViewer), networkViewer, NetworkElementType.LAYER, false);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        this.getTableHeader().setReorderingAllowed(false);
        setAutoCreateRowSorter(true);
    }


    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesColumns) {
        NetworkLayer layer = currentState.getNetworkLayerDefault();
        List<Object[]> allLayerData = new LinkedList<Object[]>();
        for (NetworkLayer auxLayer : currentState.getNetworkLayers()) {
            RoutingType routingType_thisLayer = currentState.getRoutingType(auxLayer);
            Object[] layerData = new Object[netPlanViewTableHeader.length];
            layerData[COLUMN_ID] = auxLayer.getId();
            layerData[COLUMN_INDEX] = auxLayer.getIndex();
            layerData[COLUMN_NAME] = auxLayer.getName();
            layerData[COLUMN_ROUTINGTYPE] = currentState.getRoutingType(auxLayer);
            layerData[COLUMN_NUMLINKS] = currentState.getNumberOfLinks(auxLayer);
            layerData[COLUMN_NUMDEMANDS] = currentState.getNumberOfDemands(auxLayer);
            layerData[COLUMN_NUMMULTICASTDEMANDS] = currentState.getNumberOfMulticastDemands(auxLayer);
            layerData[COLUMN_NUMROUTES] = routingType_thisLayer == RoutingType.SOURCE_ROUTING ? currentState.getNumberOfRoutes(auxLayer) : 0;
            layerData[COLUMN_NUMFORWARDINRULES] = routingType_thisLayer == RoutingType.HOP_BY_HOP_ROUTING ? currentState.getNumberOfForwardingRules(auxLayer) : 0;
            layerData[COLUMN_NUMSEGMENTS] = routingType_thisLayer == RoutingType.SOURCE_ROUTING ? currentState.getRoutesAreBackup(auxLayer).size() : 0;
            layerData[COLUMN_NUMTREES] = currentState.getNumberOfMulticastTrees(auxLayer);
            layerData[COLUMN_DESCRIPTION] = auxLayer.getDescription();
            layerData[COLUMN_LINKCAPUNITS] = currentState.getLinkCapacityUnitsName(auxLayer);
            layerData[COLUMN_DEMANDTRAFUNITS] = currentState.getDemandTrafficUnitsName(auxLayer);
            layerData[COLUMN_TAGS] = currentState.getTags();
            layerData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(auxLayer.getAttributes());
            allLayerData.add(layerData);
        }
        return allLayerData;
    }

    public String getTabName() {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders() {
        return netPlanViewTableHeader;
    }

    public String[] getCurrentTableHeaders(){
        ArrayList<String> attColumnsHeaders = getAttributesColumnsHeaders();
        String[] headers = new String[netPlanViewTableHeader.length + attColumnsHeaders.size()];
        for(int i = 0; i < headers.length ;i++)
        {
            if(i<netPlanViewTableHeader.length)
            {
                headers[i] = netPlanViewTableHeader[i];
            }
            else{
                headers[i] = "Att: "+attColumnsHeaders.get(i - netPlanViewTableHeader.length);
            }
        }


        return headers;
    }
    public String[] getTableTips() {
        return netPlanViewTableTips;
    }

    public boolean hasElements()
    {
        return true;
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

//    public int[] getColumnsOfSpecialComparatorForSorting() {
//        return new int[]{};
//    }

    private static TableModel createTableModel(final GUINetworkDesign networkViewer) {
        TableModel layerTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex >= netPlanViewTableHeader.length;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);
                /* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;
                /* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };

        return layerTableModel;
    }

    private void setDefaultCellRenderers(final GUINetworkDesign networkViewer) {
    }

    private void setSpecificCellRenderers() {
    }

    public void setColumnRowSortingFixedAndNonFixedTable()
    {
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet();
        final DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES ; col ++)
        	rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter , columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    public int getNumberOfDecoratorColumns() {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        return new ArrayList<String>();
    }


    @Override
    public void doPopup(MouseEvent e, int row, final Object itemId) {
        JPopupMenu popup = new JPopupMenu();

        if (callback.getVisualizationState().isNetPlanEditable()) {
            popup.add(getAddOption());
            for (JComponent item : getExtraAddOptions())
                popup.add(item);
        }

        if (!isTableEmpty()) {
            if (callback.getVisualizationState().isNetPlanEditable()) {
                if (row != -1) {
                    if (networkElementType != NetworkElementType.LAYER || callback.getDesign().getNumberOfLayers() != 1)
                    {
                        JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);

                        removeItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                NetPlan netPlan = callback.getDesign();

                                try {
                                    netPlan.removeNetworkLayer(netPlan.getNetworkLayerFromId((long) itemId));

                                    final VisualizationMediator vs = callback.getVisualizationState();
                            		Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer,Boolean>> res =
                    				vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<> (callback.getDesign().getNetworkLayers()));
                    		vs.setCanvasLayerVisibilityAndOrder(callback.getDesign() , res.getFirst() , res.getSecond());
                                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LAYER));
                                    callback.addNetPlanChange();
                                } catch (Throwable ex) {
                                    ErrorHandling.addErrorOrException(ex, getClass());
                                    ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                                }
                            }
                        });

                        popup.add(removeItem);
                    }

                    addPopupMenuAttributeOptions(e, row, itemId, popup);
                }
                List<JComponent> extraOptions = getExtraOptions(row, itemId);
                if (!extraOptions.isEmpty()) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : extraOptions) popup.add(item);
                }
            }

            List<JComponent> forcedOptions = getForcedOptions();
            if (!forcedOptions.isEmpty()) {
                if (popup.getSubElements().length > 0) popup.addSeparator();
                for (JComponent item : forcedOptions) popup.add(item);
            }
        }

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    @Override
    public void showInCanvas(MouseEvent e, Object itemId) {
        return;
    }

    private boolean isTableEmpty() {
        return false;
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);

        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = callback.getDesign();

                try {
                    netPlan.addLayer("Layer " + netPlan.getNumberOfLayers(), null, null, null, null , null);
                    final VisualizationMediator vs = callback.getVisualizationState();
            		Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer,Boolean>> res = vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<> (callback.getDesign().getNetworkLayers()));
            		vs.setCanvasLayerVisibilityAndOrder(callback.getDesign() , res.getFirst() , res.getSecond());
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LAYER));
                    callback.addNetPlanChange();
                } catch (Throwable ex) {
                	ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        return addItem;
    }

    private List<JComponent> getExtraAddOptions() {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}

