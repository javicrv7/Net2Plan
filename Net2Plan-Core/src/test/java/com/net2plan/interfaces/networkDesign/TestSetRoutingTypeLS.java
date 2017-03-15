package com.net2plan.interfaces.networkDesign;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.net2plan.interfaces.TestConstants;
import com.net2plan.utils.Constants.RoutingType;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

public class TestSetRoutingTypeLS {

	private NetPlan netTriangle = null;
	private Node netTriangle_n1,  netTriangle_n2, netTriangle_n3;
	private Resource netTriangle_r1,  netTriangle_r2, netTriangle_r3;
	private Link netTriangle_e12,  netTriangle_e21, netTriangle_e13 , netTriangle_e31 , netTriangle_e23 , netTriangle_e32;
	private Demand netTriangle_d12,  netTriangle_d21, netTriangle_d13 , netTriangle_d31 , netTriangle_d23 , netTriangle_d32;

	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12, link23 , link13;
	private Demand d13, d12 , scd123;
	private MulticastDemand d123;
	private MulticastTree tStar, t123;
	private Set<Link> star, line123;
	private Set<Node> endNodes;
	private Route r12, r123a, r123b , sc123;
	private List<Link> path13;
	private List<NetworkElement> pathSc123;
	private Resource res2 , res2backup;
	private Route segm13;
	private NetworkLayer lowerLayer , upperLayer;
	private Link upperLink12;
	private Link upperMdLink12 , upperMdLink13;
	private MulticastDemand upperMd123;
	private MulticastTree upperMt123;

	private NetPlan npAgg;
	private Node nagg1, nagg2 , nagg3 , nagg4, nagg5, nagg6;
	private Link linka13, linka23, linka34 , linka45 , linka46;
	private Demand da13, da23 , da34 , da45, da46;
	
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
    }

	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan ();
		this.lowerLayer = np.getNetworkLayerDefault();
		lowerLayer.addTag("t1");
		np.setDemandTrafficUnitsName("Mbps" , lowerLayer);
		this.upperLayer = np.addLayer("upperLayer" , "description" , "Mbps" , "upperTrafficUnits" , new URL ("file:/upperIcon") , null);
		upperLayer.addTag("t1");
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		n1.addTag("t1");
		this.n2 = np.addNode(0 , 0 , "node2" , null);
		n1.setPopulation(200);
		this.n3 = np.addNode(0 , 0 , "node3" , null);
		n1.setPopulation(100);
		n1.setAttribute("att" , "1");
		n1.setSiteName("s12");
		n2.setSiteName("s12");
		n3.setSiteName("s3");
		this.n1.setUrlNodeIcon(lowerLayer , new URL ("file:/lowerIcon"));
		this.link12 = np.addLink(n1,n2,100,100,1,null,lowerLayer);
		this.link23 = np.addLink(n2,n3,100,100,1,null,lowerLayer);
		this.link13 = np.addLink(n1,n3,100,100,1,null,lowerLayer);
		link12.addTag("t1");
		this.d13 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		d13.addTag("t1"); d13.addTag("t2");
		d13.setIntendedRecoveryType(Demand.IntendedRecoveryType.NONE);
		this.d12 = np.addDemand(n1, n2, 3 , null,lowerLayer);
		d12.setIntendedRecoveryType(Demand.IntendedRecoveryType.PROTECTION_NOREVERT);
		this.r12 = np.addRoute(d12,1,1.5,Collections.singletonList(link12),null);
		r12.addTag("t1"); r12.addTag("t3");
		np.addTag("t1");
		this.path13 = new LinkedList<Link> (); path13.add(link12); path13.add(link23);
		this.r123a = np.addRoute(d13,1,1.5,path13,null);
		this.r123b = np.addRoute(d13,1,1.5,path13,null);
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		res2.addTag("t1");
		this.res2backup = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.scd123 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		this.scd123.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("type"));
		this.pathSc123 = Arrays.asList(link12 ,res2 , link23);
		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(300.0 , 50.0 , 302.0) , pathSc123 , null);
		sc123.addTag("t1");
		this.segm13 = np.addRoute(d13 , 0 , 50 , Collections.singletonList(link13) , null);
		this.r123a.addBackupRoute(segm13);
		this.upperLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.d12.coupleToUpperLayerLink(upperLink12);
		this.line123 = new HashSet<Link> (Arrays.asList(link12, link23));
		this.star = new HashSet<Link> (Arrays.asList(link12, link13));
		this.endNodes = new HashSet<Node> (Arrays.asList(n2,n3));
		this.d123 = np.addMulticastDemand(n1 , endNodes , 100 , null , lowerLayer);
		d123.addTag("t1");
		this.t123 = np.addMulticastTree(d123 , 10,15,line123,null);
		t123.addTag("t1");
		this.tStar = np.addMulticastTree(d123 , 10,15,star,null);
		this.upperMdLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.upperMdLink13 = np.addLink(n1,n3,10,100,1,null,upperLayer);
		this.upperMd123 = np.addMulticastDemand (n1 , endNodes , 100 , null , upperLayer);
		this.upperMt123 = np.addMulticastTree (upperMd123 , 10 , 15 , new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)) , null);
		d123.couple(new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)));

		/* Triangle link cap 100, length 1, demands offered 1 */
		this.netTriangle = new NetPlan ();
		this.netTriangle_n1 = this.netTriangle.addNode(0 , 0 , "node1" , null);
		this.netTriangle_n2 = this.netTriangle.addNode(0 , 0 , "node2" , null);
		this.netTriangle_n3 = this.netTriangle.addNode(0 , 0 , "node3" , null);
		this.netTriangle_e12 = this.netTriangle.addLink(netTriangle_n1,netTriangle_n2,100,1,1,null);
		this.netTriangle_e21 = this.netTriangle.addLink(netTriangle_n2,netTriangle_n1,100,1,1,null);
		this.netTriangle_e13 = this.netTriangle.addLink(netTriangle_n1,netTriangle_n3,100,1,1,null);
		this.netTriangle_e31 = this.netTriangle.addLink(netTriangle_n3,netTriangle_n1,100,1,1,null);
		this.netTriangle_e23 = this.netTriangle.addLink(netTriangle_n2,netTriangle_n3,100,1,1,null);
		this.netTriangle_e32 = this.netTriangle.addLink(netTriangle_n3,netTriangle_n2,100,1,1,null);
		this.netTriangle_d12 = this.netTriangle.addDemand(netTriangle_n1,netTriangle_n2,1,null);
		this.netTriangle_d21 = this.netTriangle.addDemand(netTriangle_n2,netTriangle_n1,1,null);
		this.netTriangle_d13 = this.netTriangle.addDemand(netTriangle_n1,netTriangle_n3,1,null);
		this.netTriangle_d31 = this.netTriangle.addDemand(netTriangle_n3,netTriangle_n1,1,null);
		this.netTriangle_d23 = this.netTriangle.addDemand(netTriangle_n2,netTriangle_n3,1,null);
		this.netTriangle_d32 = this.netTriangle.addDemand(netTriangle_n3,netTriangle_n2,1,null);
		this.netTriangle_r1 = netTriangle.addResource("type1" , "name" , netTriangle_n1 , 100.0 , "units" , null , 1.0 , null);
		this.netTriangle_r2 = netTriangle.addResource("type2" , "name" , netTriangle_n2 , 100.0 , "units" , null , 1.0 , null);
		this.netTriangle_r3 = netTriangle.addResource("type3" , "name" , netTriangle_n3 , 100.0 , "units" , null , 1.0 , null);

		this.npAgg = new NetPlan ();
		this.npAgg.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		this.nagg1 = this.npAgg.addNode(0 , 0 , "" , null);
		this.nagg2 = this.npAgg.addNode(0 , 0 , "" , null);
		this.nagg3 = this.npAgg.addNode(0 , 0 , "" , null);
		this.nagg4 = this.npAgg.addNode(0 , 0 , "" , null);
		this.nagg5 = this.npAgg.addNode(0 , 0 , "" , null);
		this.nagg6 = this.npAgg.addNode(0 , 0 , "" , null);
		this.linka13 = npAgg.addLink(nagg1,nagg3,100,100,1,null);
		this.linka23 = npAgg.addLink(nagg2,nagg3,100,100,1,null);
		this.linka34 = npAgg.addLink(nagg3,nagg4,100,100,1,null);
		this.linka45 = npAgg.addLink(nagg4,nagg5,100,100,1,null);
		this.linka46 = npAgg.addLink(nagg4,nagg6,100,100,1,null);
		this.da13 = npAgg.addDemand(nagg1 , nagg3 , 10 , null);
		this.da23 = npAgg.addDemand(nagg2 , nagg3 , 10 , null);
		this.da34 = npAgg.addDemand(nagg3 , nagg4 , 0 , null);
		this.da45 = npAgg.addDemand(nagg4 , nagg5 , 0 , null);
		this.da46 = npAgg.addDemand(nagg4 , nagg6 , 0 , null);
		this.da13.attachToAggregatedDemands(ImmutableMap.of (da34 , 1.0));
		this.da23.attachToAggregatedDemands(ImmutableMap.of (da34 , 1.0));
		this.da34.attachToAggregatedDemands(ImmutableMap.of (da45 , 0.4 , da46 , 0.6));
		
		File resourcesDir = new File(TestConstants.TEST_FILE_DIRECTORY);
		if (!resourcesDir.exists()) resourcesDir.mkdirs();
	}

	@After
	public void tearDown() throws Exception
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testNetPlan()
	{
		this.np = new NetPlan (new File ("C:\\Users\\Pablo\\Desktop\\net2plan-0.5.0.3\\workspace\\data\\networkTopologies\\allSpainFilteredNodesAndTraffic.n2p"));
		final NetworkLayer ipLayer = np.getNetworkLayer("IP");
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING, ipLayer);

		final int N = np.getNumberOfNodes();
		final int D = np.getNumberOfDemands(ipLayer);
		final int E = np.getNumberOfLinks(ipLayer) + 2*N;
		DoubleMatrix2D f_de = DoubleFactory2D.sparse.make(D,E);

		long i = NetPlan.profileTime("", -1);
		final Random rng = new Random (1L);
		
		List<Node> originNodes = new ArrayList<> ();
		List<Node> destinationNodes = new ArrayList<> ();
		for (int cont = 0; cont < N ; cont ++)
		{
			final Demand d = np.getDemand (rng.nextInt(D) , ipLayer);
			originNodes.add(d.getIngressNode());
			destinationNodes.add(d.getEgressNode());
		}
		List<Link> addedLinks = np.addLinks(originNodes ,  destinationNodes ,  true ,  ipLayer);
		for (Link e : addedLinks)
		{
			final Demand d = np.getNodePairDemands(e.getOriginNode(), e.getDestinationNode(), false, ipLayer).iterator().next();
			f_de.set (d.getIndex() , e.getIndex() , 1.0);
		}
		
		System.out.println("hereaaaa");
		np.setForwardingRules(f_de, ipLayer);
		System.out.println("here");
	}

}
