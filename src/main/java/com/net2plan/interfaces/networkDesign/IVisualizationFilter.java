package com.net2plan.interfaces.networkDesign;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.internal.IExternal;
import com.net2plan.utils.Pair;


import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by César on 15/11/2016.
 */
   /*Forwarding Rules are defined in a Map
    //Key: Pair<Demand,Link>
    //Value: Double*/
public interface IVisualizationFilter extends IExternal
{
    Map<Class<? extends NetworkElement>, Set<NetworkElement>> executeFilter(NetPlan netPlan, NetworkLayer layer, Map<String, String> filtersParameters, Map<String, String> net2PlanParameters);
    String getDescription();
    String getUniqueName();
}
