package de.unipaderborn.visuflow.ui.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import de.unipaderborn.visuflow.model.DataModel;
import de.unipaderborn.visuflow.model.VFClass;
import de.unipaderborn.visuflow.model.VFMethod;
import de.unipaderborn.visuflow.util.ServiceUtil;
import de.visuflow.callgraph.ControlFlowGraph;

public class GraphManager implements Runnable, ViewerListener {

	Graph graph;
	String styleSheet;
	private Viewer viewer;
	private ViewPanel view;
	List<VFClass> analysisData;

	Container panel;
	JApplet applet;
	JButton zoomInButton, zoomOutButton, viewCenterButton, filterGraphButton, toggleLayout;
	JToolBar settingsBar;
	JTextField attribute;
	JScrollPane scrollbar;
	JComboBox<VFMethod> methodList;

	double zoomInDelta, zoomOutDelta, maxZoomPercent, minZoomPercent;

	boolean autoLayoutEnabled = false;

	Layout graphLayout = new SpringBox();

	private JToolTip tip;

	public GraphManager(String graphName, String styleSheet)
	{
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		this.zoomInDelta = .2;
		this.zoomOutDelta = .2;
		this.maxZoomPercent = .5;
		this.minZoomPercent = 2.0;
		this.styleSheet = styleSheet;
		createGraph(graphName);
		createUI();

		EventHandler dataModelHandler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				@SuppressWarnings("unchecked")
				List<VFClass> vfClasses = (List<VFClass>) event.getProperty("model");
				System.out.println("Model changed " + vfClasses.size() + " " + vfClasses);
			}
		};
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put(EventConstants.EVENT_TOPIC, DataModel.EA_TOPIC_DATA_MODEL_CHANGED);
		ServiceUtil.registerService(EventHandler.class, dataModelHandler, properties);
	}

	public Container getApplet() {
		return applet.getRootPane();
	}

	void createGraph(String graphName)
	{
		if(graph == null && graphName != null)
		{
			System.out.println("Graph object is null... Creating a new Graph");
			graph = new MultiGraph(graphName);
		}

		graph.clear();
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.setStrict(true);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);

		view = viewer.addDefaultView(false);
	}

	private void createUI() {
		// TODO Auto-generated method stub
		createZoomControls();
		createViewListeners();
		createAttributeControls();
		createToggleLayoutButton();
		createMethodComboBox();
		createSettingsBar();
		createPanel();
		createAppletContainer();
	}

	private void createAppletContainer() {
		// TODO Auto-generated method stub
		applet = new JApplet();

		scrollbar = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		view.setAutoscrolls(true);
		scrollbar.setPreferredSize(new Dimension(20, 0));
		scrollbar.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				// TODO Auto-generated method stub
				System.out.println("vertical scrollbar " + e.getValue());
			}
		});

		scrollbar.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				// TODO Auto-generated method stub
				Point3 viewCenter = view.getCamera().getViewCenter();
				System.out.println("horizontal scrollbar " + e.getValue());
				System.out.println("view center " + viewCenter);
				if(e.getAdjustmentType() == AdjustmentEvent.UNIT_INCREMENT)
					view.getCamera().setViewCenter(viewCenter.x + 1.0, viewCenter.y + 1.0, 0.0);
				if(e.getAdjustmentType() == AdjustmentEvent.UNIT_DECREMENT)
					view.getCamera().setViewCenter(viewCenter.x + 1.0, viewCenter.y + 1.0, 0.0);
			}
		});
		applet.add(scrollbar);
	}

	private void createAttributeControls() {
		// TODO Auto-generated method stub
		attribute = new JTextField("ui.screenshot,C:/Users/Shashank B S/Desktop/image.png");
		filterGraphButton = new JButton("SetAttribute");

		filterGraphButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				String[] newAttribute = attribute.getText().split(",");
				graph.setAttribute(newAttribute[0], newAttribute[1]);
			}
		});
	}

	private void createMethodComboBox()
	{
		methodList = new JComboBox<VFMethod>();
		//		methodList.addItem("Select Method");

		methodList.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				@SuppressWarnings("unchecked")
				JComboBox<String> methodBox = (JComboBox<String>) e.getSource();
				if(analysisData == null)
					System.out.println("analysis data is null");
				try {
					VFMethod selectedMethod = (VFMethod) methodBox.getSelectedItem();
					System.out.println(selectedMethod.getControlFlowGraph().listEdges.size());
					System.out.println(selectedMethod.getControlFlowGraph().listNodes.size());
					renderMethodCFG(selectedMethod.getControlFlowGraph());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				experimentalLayout();
			}
		});
	}

	private void createSettingsBar() {
		// TODO Auto-generated method stub
		settingsBar = new JToolBar("ControlsBar", JToolBar.HORIZONTAL);

		settingsBar.add(zoomInButton);
		settingsBar.add(zoomOutButton);
		settingsBar.add(viewCenterButton);
		settingsBar.add(methodList);
		settingsBar.add(filterGraphButton);
		settingsBar.add(attribute);
		settingsBar.add(toggleLayout);
	}

	private void createPanel() {
		// TODO Auto-generated method stub
		panel = new JFrame().getContentPane();
		panel.add(view);
		panel.add(settingsBar, BorderLayout.PAGE_START);
	}

	private void createViewListeners() {
		// TODO Auto-generated method stub
		view.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				// TODO Auto-generated method stub
				int rotationDirection = e.getWheelRotation();
				if(rotationDirection > 0)
					zoomIn();
				else
					zoomOut();
			}
		});

		view.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent event) {

				GraphicElement curElement = view.findNodeOrSpriteAt(event.getX(), event.getY());

				if(curElement == null && tip != null) {
					tip.setVisible(false);
					setTip(null);
					view.repaint();
				}

				if(curElement != null && tip == null) {
					Node node=graph.getNode(curElement.getId());
					String result = "<html>";
					int maxToolTipLength=0;
					int height=0;
					for(String key:node.getEachAttributeKey()) {
						if(key.startsWith("nodeData")){
							height++;
							Object value = node.getAttribute(key);
							String tempVal=key.substring(key.lastIndexOf(".")+1)+" : "+value.toString();
							if(tempVal.length()>maxToolTipLength){
								maxToolTipLength=tempVal.length();
							}

							result+=tempVal+"<br>";
						}
					}
					result+="</html>";
					tip = new JToolTip();
					String tipText = result;
					tip.setTipText(tipText);
					tip.setBounds(event.getX() - tipText.length()*3 + 1, event.getY(), maxToolTipLength*6+3,height*20 );
					setTip(tip);
					tip.setVisible(true);

					if(tipText.length() > 10) {
						tip.setLocation(event.getX()-15, event.getY());
					}

					view.add(tip);
					tip.repaint();
				}
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub
				/*if(e.getButton() == 0)
				{
					Point dest = e.getPoint();
					System.out.println("dragged with button");
					System.out.println(dest);

					Point3 currViewCenter = view.getCamera().getViewCenter();

					for(int i=0; i<e.getClickCount(); i++)
					{
						view.getCamera().setViewCenter(currViewCenter.x+.2, currViewCenter.y+.2, 0);
						//						try {
						//							Thread.sleep(1000);
						//						} catch (InterruptedException e1) {
						//							// TODO Auto-generated catch block
						//							e1.printStackTrace();
						//						}
					}
				}*/
			}
		});
	}

	private void zoomIn()
	{
		double viewPercent = view.getCamera().getViewPercent();
		if(viewPercent > maxZoomPercent)
			view.getCamera().setViewPercent(viewPercent - zoomInDelta);
	}

	private void zoomOut()
	{
		double viewPercent = view.getCamera().getViewPercent();
		if(viewPercent < minZoomPercent)
			view.getCamera().setViewPercent(viewPercent + zoomOutDelta);
	}

	private void createZoomControls() {
		// TODO Auto-generated method stub
		zoomInButton = new JButton("+");
		zoomOutButton = new JButton("-");
		viewCenterButton = new JButton("reset");

		zoomInButton.setBackground(Color.gray);
		zoomInButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				zoomIn();
			}
		});

		zoomOutButton.setBackground(Color.gray);
		zoomOutButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				zoomOut();
			}
		});

		viewCenterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				view.getCamera().resetView();
			}
		});
	}

	private void createToggleLayoutButton()
	{
		toggleLayout = new JButton();
		toggleAutoLayout();
		toggleLayout.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				toggleAutoLayout();
			}
		});
	}

	private void toggleAutoLayout()
	{
		if(!autoLayoutEnabled)
		{
			if(viewer != null && graphLayout != null)
			{
				//				viewer.enableAutoLayout(graphLayout);
				experimentalLayout();
			}
			else if(viewer != null)
			{
				//				viewer.enableAutoLayout();
				experimentalLayout();
			}
			autoLayoutEnabled = true;
			toggleLayout.setText("Disable Layouting");
		}
		else
		{
			viewer.disableAutoLayout();
			autoLayoutEnabled = false;
			toggleLayout.setText("Enable Layouting");
		}
	}

	void generateGraphFromGraphStructure()
	{
		DataModel dataModel = ServiceUtil.getService(DataModel.class);
		analysisData = dataModel.listClasses();
		if(!analysisData.isEmpty()) {
			VFClass first = analysisData.get(0);
			for (VFMethod vfMethod : first.getMethods()) {
				methodList.addItem(vfMethod);
			}
		}
	}

	private void renderMethodCFG(ControlFlowGraph interGraph) throws Exception
	{
		if(interGraph == null)
			throw new Exception("GraphStructure is null");

		Iterator<Node> itr = graph.iterator();

		try {
			for(Edge curr : itr.next())
			{
				if(graph.getId() != null)
				{
					graph.removeNode(curr.getNode0().getId());
					graph.removeNode(curr.getNode1().getId());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		ListIterator<de.visuflow.callgraph.Edge> edgeIterator = interGraph.listEdges.listIterator();

		while(edgeIterator.hasNext())
		{
			de.visuflow.callgraph.Edge currEdgeIterator = edgeIterator.next();

			de.visuflow.callgraph.Node src = currEdgeIterator.getSource();
			de.visuflow.callgraph.Node dest = currEdgeIterator.getDestination();

			createGraphNode(src);
			createGraphNode(dest);
			createGraphEdge(src,dest);
		}
	}

	private void createGraphEdge(de.visuflow.callgraph.Node src, de.visuflow.callgraph.Node dest) {
		// TODO Auto-generated method stub
		if(graph.getEdge("" + src.getId() + dest.getId()) == null)
		{
			Edge createdEdge = graph.addEdge(src.getId() + "" + dest.getId(), src.getId() + "", dest.getId() + "", true);
			createdEdge.addAttribute("ui.label", "{a,b}");
			createdEdge.addAttribute("edgeData.outSet", "{a,b}");
		}
	}

	private void createGraphNode(de.visuflow.callgraph.Node node) {
		// TODO Auto-generated method stub
		if(graph.getNode(node.getId() + "") == null)
		{
			Node createdNode = graph.addNode(node.getId() + "");
			createdNode.setAttribute("ui.label", node.getLabel().toString());
			createdNode.setAttribute("nodeData.unit", node.getLabel().toString());
			createdNode.setAttribute("nodeData.unitType", node.getLabel().getClass());
			createdNode.setAttribute("nodeData.inSet", "coming soon");
			createdNode.setAttribute("nodeData.outSet", "coming soon");
		}
	}

	private void experimentalLayout()
	{
		//		viewer.disableAutoLayout();
		double spacing = 2.0;
		double rowSpacing = 12.0;
		double nodeCount = graph.getNodeCount() * spacing;
		Iterator<Node> nodeIterator = graph.getNodeIterator();
		while(nodeIterator.hasNext())
		{
			Node curr = nodeIterator.next();

			Iterator<Edge> leavingEdgeIterator = curr.getEdgeIterator();
			double outEdges = 0.0;
			while(leavingEdgeIterator.hasNext())
			{
				Edge outEdge = leavingEdgeIterator.next();
				Node target = outEdge.getTargetNode();
				target.setAttribute("xyz", outEdges, nodeCount, 0.0);
				outEdges += rowSpacing;
			}

			curr.setAttribute("xyz", 0.0, nodeCount, 0.0);
			nodeCount -= spacing;
		}
	}

	void toggleNode(String id){
		System.out.println("Togglenodes called");
		Node n  = graph.getNode(id);
		Object[] pos = n.getAttribute("xyz");
		Iterator<Node> it = n.getBreadthFirstIterator(true);
		if(n.hasAttribute("collapsed")){
			n.removeAttribute("collapsed");
			while(it.hasNext()){
				Node m  =  it.next();

				for(Edge e : m.getLeavingEdgeSet()) {
					e.removeAttribute("ui.hide");
				}
				m.removeAttribute("layout.frozen");
				m.setAttribute("x",((double)pos[0])+Math.random()*0.0001);
				m.setAttribute("y",((double)pos[1])+Math.random()*0.0001);

				m.removeAttribute("ui.hide");

			}
			n.removeAttribute("ui.class");

		} else {
			n.setAttribute("ui.class", "plus");
			n.setAttribute("collapsed");

			while(it.hasNext()){
				Node m  =  it.next();

				for(Edge e : m.getLeavingEdgeSet()) {
					e.setAttribute("ui.hide");
				}
				if(n != m) {
					m.setAttribute("layout.frozen");
					//					m.setAttribute("x", ((double) pos[0]) + Math.random() * 0.0001);
					//					m.setAttribute("y", ((double) pos[1]) + Math.random() * 0.0001);

					m.setAttribute("xyz", ((double) pos[0]) + Math.random() * 0.0001, ((double) pos[1]) + Math.random() * 0.0001, 0.0);

					m.setAttribute("ui.hide");
				}

			}
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		generateGraphFromGraphStructure();

		ViewerPipe fromViewer = viewer.newViewerPipe();
		fromViewer.addViewerListener(this);
		fromViewer.addSink(graph);
		
		/*Iterator<? extends Node> nodeIterator = graph.getEachNode().iterator();
		while(nodeIterator.hasNext())
		{
			Node curr = nodeIterator.next();
			System.out.println("Attribute count " + curr.getAttributeCount());
			System.out.println("nodeData.unit " + curr.getAttribute("nodeData.unit").toString());
		}*/

		// FIXME the Thread.sleep slows down the loop, so that it does not eat up the CPU
		// but this really should be implemented differently. isn't there an event listener
		// or something we can use, so that we call pump() only when necessary
		while(true) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
			fromViewer.pump();
		}
	}

	@Override
	public void buttonPushed(String id) {
		// TODO Auto-generated method stub
		toggleNode(id);
		experimentalLayout();
	}

	@Override
	public void buttonReleased(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void viewClosed(String arg0) {
		// TODO Auto-generated method stub

	}

	protected void setTip(JToolTip toolTip) {
		this.tip = toolTip;
	}

}