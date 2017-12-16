package main.java.jalexander.ninja;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.organic.JGraphSelfOrganizingOrganicLayout;
import org.jgraph.JGraph;
import org.jgraph.graph.*;
import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphModelAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

/**
 * Created by jalex on 12/12/2017.
 */
public class Visualizer extends JFrame{
    public Visualizer(ListenableGraph<Integer, org.jgrapht.graph.DefaultEdge> inGraph,
                      String wName)
    {
        super(wName);

        JGraphModelAdapter<Integer, org.jgrapht.graph.DefaultEdge> adapter = new JGraphModelAdapter<>(inGraph);
        JGraph jGraph = new JGraph(adapter);
        jGraph.setEdgeLabelsMovable(false);
        JGraphSelfOrganizingOrganicLayout layout = new JGraphSelfOrganizingOrganicLayout();
        JGraphFacade facade = new JGraphFacade(jGraph);
        layout.run(facade);
        facade.scale(new Rectangle(0, 0, 1000, 1000));

        Collection<Object> verts = facade.getVertices();
        for(Object v : verts){
            Dimension2D size = facade.getSize(v);
            facade.setSize(v, size.getWidth() / 2.2, size.getHeight() / 2);
            DefaultGraphCell cell = (DefaultGraphCell) v;
        }

        Map nestedMap = facade.createNestedMap(true, true);
        jGraph.getGraphLayoutCache().edit(nestedMap);

        GraphLayoutCache cache = jGraph.getGraphLayoutCache();
        CellView[] cells = cache.getCellViews();
        for (CellView cell : cells) {
            if (cell instanceof EdgeView) {
                EdgeView ev = (EdgeView) cell;
                org.jgraph.graph.DefaultEdge eval = (org.jgraph.graph.DefaultEdge) ev.getCell();
                eval.setUserObject("");
            }
        }
        cache.reload();
        jGraph.repaint();

        getContentPane().add(jGraph);
    }
}
