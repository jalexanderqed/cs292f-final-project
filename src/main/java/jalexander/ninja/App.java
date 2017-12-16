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

    static boolean output = true;

    final static int GREEDY = 1;
    final static int BATCH_GREEDY = 2;
    final static int SCREEN = 3;
    final static int MULTI_SCREEN = 4;

    public static void main(String[] args) {
        SimpleGraph<Integer, DefaultEdge> originalGraph = getGraph();
        //ListenableGraph<Integer, DefaultEdge> graph = getSimpleGraph();

        for (int i = 0; i < 1; i++) {
            SimpleGraph<Integer, DefaultEdge> graph = (SimpleGraph<Integer, DefaultEdge>) originalGraph.clone();
            downsampleGraph(graph);

            System.out.println("\nAFTER DOWNSAMPLING");
            histogram(graph, 10);

            System.out.println("\nAVERAGE SHORTEST PATH: " + averageShortestPaths(graph));

            System.out.println("\n\nGAME 1");

            int rounds, designerAlg, adversaryAlg, designerAddsNum, adversaryRemovesNum;

            rounds = 5;
            designerAlg = SCREEN;
            adversaryAlg = GREEDY;
            designerAddsNum = 5;
            adversaryRemovesNum = 5;

            int index = 0;
            File outFile;
            do{
                index++;
                outFile = getFile( "results_" +
                        rounds + "_" +
                        designerAlg + "_" +
                        adversaryAlg + "_" +
                        designerAddsNum + "_" +
                        adversaryRemovesNum + "_" + index + ".csv");
            } while(outFile.exists());

            try {
                outFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

            SimpleGraph<Integer, DefaultEdge> graph2 = (SimpleGraph<Integer, DefaultEdge>) graph.clone();
            try {
                runGame(rounds, designerAlg, adversaryAlg, designerAddsNum,
                        adversaryRemovesNum, graph2, outFile);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

            DefaultListenableGraph<Integer, DefaultEdge> lGraph = new DefaultListenableGraph<>(graph);
            Visualizer v = new Visualizer(lGraph, "Before");
            v.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            v.setSize(1000, 1000);
            v.setVisible(true);

            DefaultListenableGraph<Integer, DefaultEdge> lGraph2 = new DefaultListenableGraph<>(graph2);
            Visualizer v2 = new Visualizer(lGraph2, "After");
            v2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            v2.setSize(1000, 1000);
            v2.setVisible(true);

            /*
            System.out.println("\n\nGREEDY");
			graph2 = (SimpleGraph<Integer, DefaultEdge>)graph.clone();
			greedy_remove(graph2, 10);
			
			System.out.println("\n\nBATCH GREEDY");
			graph2 = (SimpleGraph<Integer, DefaultEdge>)graph.clone();
			//batch_greedy_remove(graph2, 10);
			
			System.out.println("\n\nPATH SCREENING");
			graph2 = (SimpleGraph<Integer, DefaultEdge>)graph.clone();
			path_screening(graph2, 10);

            System.out.println("\n\nGREEDY PATH SCREENING");
            graph2 = (SimpleGraph<Integer, DefaultEdge>)graph.clone();
            greedyPathScreening(graph2, 10);
            */
        }
    }

    public static void runGame(int rounds,
                               int designerAlg,
                               int adversaryAlg,
                               int designerAddsNum,
                               int adversaryRemovesNum,
                               SimpleGraph<Integer, DefaultEdge> graph,
                               File outFile) throws IOException {
        double avg_old = averageShortestPaths(graph);
        double round_old = avg_old;

        FileWriter writer = null;
        PrintWriter out = null;
        if(outFile != null) {
            writer = new FileWriter(outFile);
            out = new PrintWriter(writer);
        }

        if(out != null){
            out.println("round,average shortest path length,time");
        }

        if(out != null) {
            out.println("0," + avg_old + ",0");
        }

        long startTime = System.currentTimeMillis();

        for (int round = 0; round < rounds; round++) {
            long roundStart = System.currentTimeMillis();
            boolean osave = output;
            output = false;
            switch (designerAlg) {
                case GREEDY:
                    greedy(graph, designerAddsNum);
                    break;
                case BATCH_GREEDY:
                    batch_greedy(graph, designerAddsNum);
                    break;
                case SCREEN:
                    path_screening(graph, designerAddsNum);
                    break;
                case MULTI_SCREEN:
                    greedyPathScreening(graph, designerAddsNum);
                    break;
            }

            if(out != null) {
                out.println((round + 0.5) + "," +
                        averageShortestPaths(graph) + "," +
                        (System.currentTimeMillis() - startTime));
            }

            switch (adversaryAlg) {
                case GREEDY:
                    greedy_remove(graph, adversaryRemovesNum);
                    break;
            }
            output = osave;

            double round_new = averageShortestPaths(graph);
            long roundStop = System.currentTimeMillis();

            if(out != null) {
                out.println((round + 1) + "," +
                        round_new + "," +
                        (System.currentTimeMillis() - startTime));
            }

            if (output) {
                System.out.println("\n\nRound 1 finished");
                System.out.println("Old average: " + round_old);
                System.out.println("New average: " + round_new);
                System.out.println("Improvement: " + (round_old - round_new));
                System.out.println("Round time taken: " + (roundStop - roundStart));
            }
            round_old = round_new;
        }

        out.close();
        writer.close();

        double avg_new = averageShortestPaths(graph);
        long stopTime = System.currentTimeMillis();
        if (output) {
            System.out.println("\n\nRESULTS");
            System.out.println("Old average: " + avg_old);
            System.out.println("New average: " + avg_new);
            System.out.println("Improvement: " + (avg_old - avg_new));
            System.out.println("Time taken: " + (stopTime - startTime));
        }
    }

    public static void downsampleGraph(Graph<Integer, DefaultEdge> graph) {
        LinkedList<Integer> toRemove = new LinkedList<>();
        for (Integer v : graph.vertexSet()) {
            if (graph.inDegreeOf(v) < 5) {
                toRemove.add(v);
            }
        }
        for (Integer v : toRemove) {
            graph.removeVertex(v);
        }
        takeLargestComponent(graph);

        Integer[] verts = graph.vertexSet().toArray(new Integer[0]);
        HashSet<Integer> keep = new HashSet<>();
        while (keep.size() < vertsInGraph) {
            keep.add(verts[(int) (verts.length * Math.random())]);
        }

        for (Integer v : graph.vertexSet()) {
            //if (graph.inDegreeOf(v) < 30 || graph.inDegreeOf(v) > 45) {
            if (!keep.contains(v)) {
                toRemove.add(v);
            }
        }
        for (Integer v : toRemove) {
            graph.removeVertex(v);
            takeLargestComponent(graph);
            if (graph.vertexSet().size() <= vertsInGraph) break;
        }
    }

    public static void takeLargestComponent(Graph<Integer, DefaultEdge> graph) {
        // If graph is disconnected, take only largest connected set
        ConnectivityInspector<Integer, DefaultEdge> connectTest = new ConnectivityInspector<>(graph);
        if (!connectTest.isGraphConnected()) {
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

        if (output) {
            for (int i = 0; i < histogram.length; i++) {
                System.out.println(((i * maxDegree) / numSlots) + "-" + (((i + 1) * maxDegree) / numSlots) + ":\t" + histogram[i]);
            }
        }
        return histogram;
    }

    public static double averageShortestPaths(Graph<Integer, DefaultEdge> graph) {
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

        //System.out.println("Time taken: " + (stopTime - startTime));
        //System.out.println("Number of Paths: " + shortestPaths.getShortestPathsCount());
        //System.out.println("Average: " + (totalWeight / shortestPaths.getShortestPathsCount()));
        return totalWeight / shortestPaths.getShortestPathsCount();
    }

    public static DefaultEdge max_edge(Graph<Integer, DefaultEdge> graph) {
        double avg_old = averageShortestPaths(graph);
        double best_improvement = 0.0;
        DefaultEdge best_edge = null;
        for (Integer v1 : graph.vertexSet()) {
            for (Integer v2 : graph.vertexSet()) {
                if ((v1 < v2) && !graph.containsEdge(v1, v2)) {
                    DefaultEdge e = graph.addEdge(v1, v2);
                    double avg_new = averageShortestPaths(graph);
                    double improvement = avg_old - avg_new;
                    if (improvement > best_improvement) {
                        best_edge = e;
                        best_improvement = improvement;
                    }
                    graph.removeEdge(v1, v2);
                }
            }
        }
        return best_edge;
    }

    public static DefaultEdge max_existing_edge(Graph<Integer, DefaultEdge> graph) {
        double avg_old = averageShortestPaths(graph);
        double best_improvement = 0.0;
        DefaultEdge best_edge = null;
        for (DefaultEdge e : new LinkedList<DefaultEdge>(graph.edgeSet())) {
            Integer v1 = graph.getEdgeSource(e);
            Integer v2 = graph.getEdgeTarget(e);
            if (v1 < v2) {
                graph.removeEdge(v1, v2);
                ConnectivityInspector<Integer, DefaultEdge> connect = new ConnectivityInspector<>(graph);
                if (connect.isGraphConnected()) {
                    double avg_new = averageShortestPaths(graph);
                    double improvement = avg_new - avg_old;
                    if (improvement > best_improvement) {
                        best_edge = e;
                        best_improvement = improvement;
                    }
                }
                graph.addEdge(v1, v2, e);
            }
        }
        return best_edge;
    }

    public static void greedy_remove(Graph<Integer, DefaultEdge> graph, int k) {
        long startTime = System.currentTimeMillis();
        double avg_old = averageShortestPaths(graph);
        for (int i = 0; i < k; i++) {
            DefaultEdge e = max_existing_edge(graph);
            graph.removeEdge(e);
        }
        double avg_new = averageShortestPaths(graph);
        long stopTime = System.currentTimeMillis();
        if (output) {
            System.out.println("Old average: " + avg_old);
            System.out.println("New average: " + avg_new);
            System.out.println("Improvement: " + (avg_old - avg_new));
            System.out.println("Time taken: " + (stopTime - startTime));
        }
    }

    public static void greedy(Graph<Integer, DefaultEdge> graph, int k) {
        long startTime = System.currentTimeMillis();
        double avg_old = averageShortestPaths(graph);
        for (int i = 0; i < k; i++) {
            DefaultEdge e = max_edge(graph);
            graph.addEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e), e);
        }
        double avg_new = averageShortestPaths(graph);
        long stopTime = System.currentTimeMillis();
        if (output) {
            System.out.println("Old average: " + avg_old);
            System.out.println("New average: " + avg_new);
            System.out.println("Improvement: " + (avg_old - avg_new));
            System.out.println("Time taken: " + (stopTime - startTime));
        }
    }

    public static void batch_greedy_remove(Graph<Integer, DefaultEdge> graph, int k) {
        long startTime = System.currentTimeMillis();
        double avg_old = averageShortestPaths(graph);
        TreeMap<Double, DefaultEdge> edges = new TreeMap<Double, DefaultEdge>();

        for (DefaultEdge e : new LinkedList<DefaultEdge>(graph.edgeSet())) {
            Integer v1 = graph.getEdgeSource(e);
            Integer v2 = graph.getEdgeTarget(e);
            if (v1 < v2) {
                graph.removeEdge(v1, v2);
                ConnectivityInspector<Integer, DefaultEdge> connect = new ConnectivityInspector<>(graph);
                if (connect.isGraphConnected()) {
                    double avg_new = averageShortestPaths(graph);

                    if (edges.size() < k) {
                        edges.put(avg_new, e);
                    } else if (avg_new > edges.firstEntry().getKey()) {
                        edges.pollFirstEntry();
                        edges.put(avg_new, e);
                    }
                }
                graph.addEdge(v1, v2, e);
            }
        }

        while (edges.size() > 0) {
            DefaultEdge e = edges.pollFirstEntry().getValue();
            graph.removeEdge(e);
        }

        double avg_new = averageShortestPaths(graph);
        long stopTime = System.currentTimeMillis();
        if (output) {
            System.out.println("Old average: " + avg_old);
            System.out.println("New average: " + avg_new);
            System.out.println("Improvement: " + (avg_old - avg_new));
            System.out.println("Time taken: " + (stopTime - startTime));
        }
    }

    public static void batch_greedy(Graph<Integer, DefaultEdge> graph, int k) {
        long startTime = System.currentTimeMillis();
        double avg_old = averageShortestPaths(graph);
        TreeMap<Double, DefaultEdge> edges = new TreeMap<Double, DefaultEdge>();

        for (Integer v1 : graph.vertexSet()) {
            for (Integer v2 : graph.vertexSet()) {
                if (!v1.equals(v2) && !graph.containsEdge(v1, v2)) {
                    DefaultEdge e = graph.addEdge(v1, v2);
                    double avg_new = averageShortestPaths(graph);
                    if (edges.size() < k) {
                        edges.put(avg_new, e);
                    } else if (avg_new < edges.lastEntry().getKey()) {
                        edges.pollLastEntry();
                        edges.put(avg_new, e);
                    }
                    graph.removeEdge(v1, v2);
                }
            }
        }

        while (edges.size() > 0) {
            DefaultEdge e = edges.pollLastEntry().getValue();
            graph.addEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e), e);
        }

        double avg_new = averageShortestPaths(graph);
        long stopTime = System.currentTimeMillis();
        if (output) {
            System.out.println("Old average: " + avg_old);
            System.out.println("New average: " + avg_new);
            System.out.println("Improvement: " + (avg_old - avg_new));
            System.out.println("Time taken: " + (stopTime - startTime));
        }
    }

    public static void greedyPathScreening(SimpleGraph<Integer, DefaultEdge> g, int k) {
        long startTime = System.currentTimeMillis();
        double avg_old = averageShortestPaths(g);
        boolean ostore = output;
        output = false;
        for (int i = 0; i < k; i++) {
            path_screening(g, 1);
        }
        output = ostore;
        double avg_new = averageShortestPaths(g);
        long stopTime = System.currentTimeMillis();
        if (output) {
            System.out.println("Oldest average: " + avg_old);
            System.out.println("Newest average: " + avg_new);
            System.out.println("Greedy screening improvement: " + (avg_old - avg_new));
            System.out.println("Total screening time taken: " + (stopTime - startTime));
        }
    }

    public static void path_screening(SimpleGraph<Integer, DefaultEdge> g, int k) {
        SimpleGraph<Integer, DefaultEdge> graph = (SimpleGraph<Integer, DefaultEdge>) g.clone();
        long startTime = System.currentTimeMillis();
        double avg_old = averageShortestPaths(g);

        HashMap<DefaultEdge, Integer> scores = new HashMap<DefaultEdge, Integer>();

        FloydWarshallShortestPaths<Integer, DefaultEdge> shortestPaths = new FloydWarshallShortestPaths<>(graph);

        for (Integer v1 : graph.vertexSet()) {
            for (Integer v2 : graph.vertexSet()) {
                if (v2 > v1) { //take each pair only once
                    GraphPath<Integer, DefaultEdge> p = shortestPaths.getPath(v1, v2);
                    int l = p.getLength();
                    List<Integer> nodes = p.getVertexList();
                    for (int d = 2; d < l; d++) {
                        for (int i = 0; i < l - d; i++) {
                            Integer x = nodes.get(i);
                            Integer y = nodes.get(i + d);

                            DefaultEdge e = graph.getEdge(x, y);
                            if (e == null) {
                                e = graph.addEdge(x, y);
                            }
                            Integer score = d - 1;
                            if (scores.containsKey(e)) {
                                Integer old_val = scores.get(e);
                                scores.put(e, old_val + score);
                            } else {
                                scores.put(e, score);
                            }
                        }
                    }
                }
            }
        }
//    	System.out.println(scores);
        List<Map.Entry<DefaultEdge, Integer>> sorted_scores = sortByValue(scores);
        int n = sorted_scores.size();

        for (int i = 1; i <= k; i++) {
            if(output) System.out.println(sorted_scores.get(n - i));
            DefaultEdge e = sorted_scores.get(n - i).getKey();
            g.addEdge(g.getEdgeSource(e), g.getEdgeTarget(e), e);
        }
        double avg_new = averageShortestPaths(g);
        long stopTime = System.currentTimeMillis();
        if (output) {
            System.out.println("Old average: " + avg_old);
            System.out.println("New average: " + avg_new);
            System.out.println("Improvement: " + (avg_old - avg_new));
            System.out.println("Time taken: " + (stopTime - startTime));
        }
    }

    public static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                return (e1.getValue()).compareTo(e2.getValue());
            }
        });
        return list;
		/*Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
 	
		return result;*/
    }


    public static void outputGraph(Graph<Integer, DefaultEdge> graph, String outFileName) {
        File outFile = getFile(outFileName);
        System.out.println("Writing to: " + outFile.getAbsolutePath());

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

    public static File getFile(String outFileName) {
        File inFile = new File(App.class.getResource(inFileName).getFile());
        File outFile = new File(inFile.getParentFile(), outFileName);
        return outFile;
    }
}
