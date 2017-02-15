package com.net2plan.gui.utils.visualizationFilters;


import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import org.apache.commons.collections15.map.LinkedMap;

import java.util.*;

/**
 * @author César
 * @date 15/11/2016
 */
public class VisualizationFiltersController
{
    private ArrayList<IVisualizationFilter> currentVisualizationFilters;
    private String filteringMode = "AND";
    private Map<String, Boolean> isFilterActive;
    private Map<String, Map<String, String>> filtersParameters;
    private static VisualizationFiltersController filtersController = null;


    private VisualizationFiltersController()
    {
        currentVisualizationFilters = new ArrayList<>();
        isFilterActive = new LinkedHashMap<>();
        filtersParameters = new LinkedHashMap<>();

    }

    public static VisualizationFiltersController getController()
    {
        if(filtersController == null)
        {
            filtersController = new VisualizationFiltersController();
        }
        return filtersController;
    }

    public void addVisualizationFilter(IVisualizationFilter vf)
    {

        if (containsVisualizationFilter(vf))
        {
            throw new Net2PlanException("A visualization filter with the same name has been already added");
        }
        if (vf == null)
        {
            throw new Net2PlanException("A null visualization filter cannot be added");
        } else
        {
            currentVisualizationFilters.add(vf);
            isFilterActive.put(vf.getUniqueName(),false);
            addFilterParameters(vf.getUniqueName());
        }

    }

    public  void removeVisualizationFilter(String visFilterName)
    {

        for (IVisualizationFilter vf : currentVisualizationFilters)
        {
            if (visFilterName.equals(vf.getUniqueName()))
            {
                currentVisualizationFilters.remove(vf);
                isFilterActive.remove(visFilterName);
                filtersParameters.remove(visFilterName);
                break;
            }
        }
    }

    public  ArrayList<IVisualizationFilter> getCurrentVisualizationFilters()
    {

        return currentVisualizationFilters;
    }


    public  boolean containsVisualizationFilter(IVisualizationFilter vf)
    {

        boolean flag = false;
        for (IVisualizationFilter vfg : currentVisualizationFilters)
        {
            if (vfg.getUniqueName().equals(vf.getUniqueName()))
            {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public void addFilterParameters(String vfName)
    {
        IVisualizationFilter vf = getVisualizationFilterByName(vfName);
        Map<String, String> param = new LinkedHashMap<>();
        for(Triple<String, String, String> t : vf.getParameters())
        {
            param.put(t.getFirst(), t.getSecond());
        }
        filtersParameters.put(vfName, param);
    }

    public Map<String, String> getFilterParameters(String vfName)
    {
        return filtersParameters.get(vfName);
    }

    public List<Triple<String, String, String>> getDefaultParameters(String vfName)
    {
        IVisualizationFilter vf = getVisualizationFilterByName(vfName);
        return vf.getParameters();
    }

    public  void removeAllVisualizationFilters()
    {

        currentVisualizationFilters.clear();
        isFilterActive.clear();
        filtersParameters.clear();
    }

    public  void updateFilteringMode(String newFilteringMode)
    {

        FilteringModes currentMode = FilteringModes.fromStringToEnum(newFilteringMode);

        switch (currentMode)
        {

            case AND:
                filteringMode = "AND";
                break;
            case OR:
                filteringMode = "OR";
                break;
        }
    }

    public  IVisualizationFilter getVisualizationFilterByName(String visFilterName)
    {

        IVisualizationFilter finalVf = null;
        for (IVisualizationFilter vf : currentVisualizationFilters)
        {
            if (visFilterName.equals(vf.getUniqueName()))
            {
                finalVf = vf;
                break;
            }
        }
        return finalVf;
    }

    public  void activateVisualizationFilter(IVisualizationFilter vf)
    {

        isFilterActive.replace(vf.getUniqueName(), true);
    }

    public  void deactivateVisualizationFilter(IVisualizationFilter vf)
    {

        isFilterActive.replace(vf.getUniqueName(), false);

    }

    public  boolean isVisualizationFilterActive(IVisualizationFilter vf)
    {
        return isFilterActive.get(vf.getUniqueName());
    }

    public void updateParameter(String visFilterName, String parameter, String newValue)
    {
        filtersParameters.get(visFilterName).replace(parameter, newValue);
    }


    public  boolean areAllFiltersInactive()
    {
        int counter = 0;
        for(IVisualizationFilter vf : currentVisualizationFilters)
        {
            if(!isVisualizationFilterActive(vf))
                counter++;
        }

        return counter == currentVisualizationFilters.size();
    }


    public Set<NetworkElement> getVisibleNetworkElements(NetPlan netPlan, Class networkTypeClass)
    {
        Map<Class<? extends NetworkElement>, Set<NetworkElement>> elementsMap = new HashMap<>();
        Set<NetworkElement> elemSet = new LinkedHashSet<>();
        if(getCurrentVisualizationFilters().size() > 0 && !areAllFiltersInactive())
        {

            if(filteringMode.equals("OR"))
            {
                for(IVisualizationFilter vf : currentVisualizationFilters)
                {
                    if(isVisualizationFilterActive(vf))
                    {
                        elementsMap = vf.executeFilter(netPlan, netPlan.getNetworkLayerDefault(), getFilterParameters(vf.getUniqueName()), Configuration.getNet2PlanOptions());
                        if(elementsMap.get(networkTypeClass) != null)
                        {
                            elemSet.addAll(elementsMap.get(networkTypeClass));
                        }
                    }
                }
                if(elemSet.size() == 0)
                {
                    return null;
                }


            }
            else{
                ArrayList<Set<NetworkElement>> filterSets = new ArrayList<>();
                boolean contains = true;
                for(IVisualizationFilter vf : currentVisualizationFilters)
                {
                    if(isVisualizationFilterActive(vf))
                    {
                        elementsMap = vf.executeFilter(netPlan, netPlan.getNetworkLayerDefault(), getFilterParameters(vf.getUniqueName()), Configuration.getNet2PlanOptions());
                        if(elementsMap.get(networkTypeClass) != null)
                        {
                            filterSets.add(elementsMap.get(networkTypeClass));
                        }
                    }
                }
                if(filterSets.size() > 0)
                {
                    Set<NetworkElement> elements = filterSets.get(0);
                    for (NetworkElement elem : elements)
                    {
                        contains = true;
                        for (Set<NetworkElement> set : filterSets) {
                            if (!set.contains(elem)) {
                                contains = false;
                                break;
                            }

                        }
                        if (contains)
                            elemSet.add(elem);

                    }

                }
                else{
                    return null;
                }

            }

        }
        else{
            return null;
        }
        return elemSet;
    }

    public enum FilteringModes
    {
        AND("AND"),
        OR("OR");

        private final String text;

        private FilteringModes(final String text)
        {
            this.text = text;
        }

        public  static FilteringModes fromStringToEnum(final String text)
        {
            switch (text)
            {
                case "AND":
                    return FilteringModes.AND;
                case "OR":
                    return FilteringModes.OR;
            }
            return null;
        }
    }
}
