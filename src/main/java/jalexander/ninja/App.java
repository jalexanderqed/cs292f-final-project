package main.java.jalexander.ninja;

import org.jgrapht.*;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.*;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class App {
    final static String inFileName = "/large_degree.txt";
    final static int vertsInGraph = 100;

    public static void main(String[] args) {
        SimpleGraph<Integer, DefaultEdge> originalGraph = getGraph();
        //ListenableGraph<Integer, DefaultEdge> graph = getSimpleGraph();

        for(int i = 0; i < 1; i++) {
            SimpleGraph<Integer, DefaultEdge> graph = (SimpleGraph<Integer, DefaultEdge>)originalGraph.clone();
            downsampleGraph(graph);

            System.out.println("\nAFTER DOWNSAMPLING");
            histogram(graph, 10);

            System.out.println("\nSHORTEST PATHS");
            averageShortestPaths(graph);

            DefaultListenableGraph<Integer, DefaultEdge> lGraph = new DefaultListenableGraph<>(graph);
            Visualizer v = new Visualizer(lGraph);
            v.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            v.setSize(1000, 1000);
            v.setVisible(true);
        }
    }

    public static void downsampleGraph(Graph<Integer, DefaultEdge> graph) {
        LinkedList<Integer> toRemove = new LinkedList<>();
        for (Integer v : graph.vertexSet()) {
            if (graph.inDegreeOf(v) < 20) {
                toRemove.add(v);
            }
        }
        for (Integer v : toRemove) {
            graph.removeVertex(v);
        }
        takeLargestComponent(graph);

        Integer[] verts = graph.vertexSet().toArray(new Integer[0]);
        HashSet<Integer> keep = new HashSet<>();
        while(keep.size() < vertsInGraph){
            keep.add(verts[(int)(verts.length * Math.random())]);
        }

        for (Integer v : graph.vertexSet()) {
            //if (graph.inDegreeOf(v) < 30 || graph.inDegreeOf(v) > 45) {
            if(!keep.contains(v)){
                toRemove.add(v);
            }
        }
        for (Integer v : toRemove) {
            graph.removeVertex(v);
        }
        takeLargestComponent(graph);
    }

    public static void takeLargestComponent(Graph<Integer, DefaultEdge> graph){
        // If graph is disconnected, take only largest connected set
        ConnectivityInspector<Integer, DefaultEdge> connectTest = new ConnectivityInspector<>(graph);
        if (!connectTest.isGraphConnected()) {
            Integer maxDegreeNode = getMaxNode(graph);
            List<Set<Integer>> sets = connectTest.connectedSets();
            Set<Integer> largestSet = null;
            for (Set<Integer> set : sets) {
                if (largestSet == null ||
                        set.size() > largestSet.size())
                    largestSet = set;
            }
            HashSet<Integer> inNodes = new HashSet<>(largestSet);
            for (Set<Integer> set : sets) {
                if (set != largestSet) {
                    for (Integer v : set) {
                        graph.removeVertex(v);
                    }
                }
            }
        }
    }

    public static SimpleGraph<Integer, DefaultEdge> getGraph() {
        SimpleGraph<Integer, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        File inFile = new File(App.class.getResource(inFileName).getFile());
        try (Scanner in = new Scanner(inFile);) {
            while (in.hasNextLine()) {
                String line = in.nextLine();
                if (!line.startsWith("#") && line.length() > 1) {
                    String[] nodes = line.split("\t");
                    int node0 = Integer.parseInt(nodes[0]);
                    int node1 = Integer.parseInt(nodes[1]);
                    graph.addVertex(node0);
                    graph.addVertex(node1);
                    graph.addEdge(node0, node1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        return graph;
    }

    public static ListenableGraph<Integer, DefaultEdge> getSimpleGraph() {
        ListenableGraph<Integer, DefaultEdge> graph = new DefaultListenableGraph<Integer, DefaultEdge>(new SimpleGraph<Integer, DefaultEdge>(DefaultEdge.class));
        graph.addVertex(88888);
        graph.addVertex(9999);
        graph.addEdge(88888, 9999);
        return graph;
    }

    public static Integer getMaxNode(Graph<Integer, DefaultEdge> graph) {
        int maxDegree = 0;
        Integer maxDegreeNode = 0;

        for (Integer v : graph.vertexSet()) {
            int degree = graph.inDegreeOf(v);
            if (degree > maxDegree) {
                maxDegree = degree;
                maxDegreeNode = v;
            }
        }
        return maxDegreeNode;
    }

    public static int[] histogram(Graph<Integer, DefaultEdge> graph, int numSlots) {
        int[] histogram = new int[numSlots];
        int maxDegree = graph.inDegreeOf(getMaxNode(graph));

        for (Integer v : graph.vertexSet()) {
            double degree = graph.inDegreeOf(v);
            int index = (int) Math.floor((degree / (maxDegree + 1)) * histogram.length);
            histogram[index]++;
        }

        System.out.println("Num vertices: " + graph.vertexSet().size());
        System.out.println("Max degree: " + maxDegree);
        for (int i = 0; i < histogram.length; i++) {
            System.out.println(((i * maxDegree) / numSlots) + "-" + (((i + 1) * maxDegree) / numSlots) + ":\t" + histogram[i]);
        }
        return histogram;
    }

    public static void averageShortestPaths(Graph<Integer, DefaultEdge> graph) {
        long startTime = System.currentTimeMillis();
        FloydWarshallShortestPaths<Integer, DefaultEdge> shortestPaths = new FloydWarshallShortestPaths<>(graph);
        double totalWeight = 0;
        for (Integer v1 : graph.vertexSet()) {
            for (Integer v2 : graph.vertexSet()) {
                if (!v1.equals(v2)) {
                    totalWeight += shortestPaths.getPathWeight(v1, v2);
                }
            }
        }
        long stopTime = System.currentTimeMillis();

        System.out.println("Time taken: " + (stopTime - startTime));
        System.out.println("Number of Paths: " + shortestPaths.getShortestPathsCount());
        System.out.println("Average: " + (totalWeight / shortestPaths.getShortestPathsCount()));
    }

    public static void outputGraph(Graph<Integer, DefaultEdge> graph, String outFileName) {
        File inFile = new File(App.class.getResource(inFileName).getFile());
        File outFile = new File(inFile.getParentFile(), outFileName);
        System.out.println("Writing to: " + outFile.getAbsolutePath());

        try {
            outFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        try (
                FileWriter writer = new FileWriter(outFile);
                PrintWriter out = new PrintWriter(writer);
        ) {
            for (DefaultEdge edge : graph.edgeSet()) {
                out.print(graph.getEdgeSource(edge) + "\t" +
                        graph.getEdgeTarget(edge) + "\n");

            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
