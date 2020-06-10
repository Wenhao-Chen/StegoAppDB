package app_analysis.trees.exas;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXBlock;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import app_analysis.common.Dirs;
import util.F;
import util.Mailjet;
import util.P;

public class Experiment_path {

	static LinkedHashMap<String, Integer> SCOPE_READ = new LinkedHashMap<String, Integer>();
	static LinkedHashMap<String, Integer> SCOPE_WRITE = new LinkedHashMap<String, Integer>();
	static {
		SCOPE_READ.put("Landroid/graphics/Bitmap;->getPixel(II)I", 0);
		SCOPE_READ.put("Landroid/graphics/Bitmap;->getPixels([IIIIIII)V", 1);
		SCOPE_READ.put("Landroid/graphics/Bitmap;->copyPixelsToBuffer(Ljava/nio/Buffer;)V", 2);
		
		SCOPE_WRITE.put("Landroid/graphics/Bitmap;->setPixel(III)V", 0);
		SCOPE_WRITE.put("Landroid/graphics/Bitmap;->setPixels([IIIIIII)V", 1);
		SCOPE_WRITE.put("Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V", 2);
		SCOPE_WRITE.put("Landroid/graphics/Bitmap;->createBitmap([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;", 3);
		SCOPE_WRITE.put("Landroid/graphics/Bitmap;->createBitmap([IIIIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;", 4);
	}
	
	static class CFGEdge {
		String label;
		APEXBlock from, to;
		public CFGEdge(APEXBlock b1, APEXBlock b2, String s) {
			from = b1;
			to = b2;
			label = s;
		}
	}
	
	static class CFG{
		Graph<APEXBlock, CFGEdge> graph = new DefaultDirectedGraph<>(CFGEdge.class);
		APEXBlock returnBlock = null;
		CFG(APEXMethod m) {
			// first add vertexes
			for (APEXBlock b : m.blocks)
				graph.addVertex(b);
			// then add edges
			for (APEXBlock b : m.blocks) {
				APEXStatement s = b.getLastStatement();
				if (s.isGotoStmt())
					addEdge(b, s.getGotoTargetStmt().block, "");
				else if (s.isIfStmt()) {
					addEdge(b, s.getFlowThroughStmt().block, "if_flow");
					addEdge(b, s.getIfJumpTargetStmt().block, "if_jump");
				}
				else if (s.isSwitchStmt()) {
					addEdge(b, s.getFlowThroughStmt().block, "switch_flow");
					for (String label : s.getSwitchMap().values())
						addEdge(b, m.getBlock(label), "switch_jump");
				}
				else if (!s.isReturnStmt() && !s.isThrowStmt())
					addEdge(b, s.getFlowThroughStmt().block, "");
				else if (s.isReturnStmt())
					returnBlock = b;
			}
		}
		void addEdge(APEXBlock b1, APEXBlock b2, String label) {
			graph.addEdge(b1, b2, new CFGEdge(b1, b2, label));
		}
	}
	
	static class ICFG {
		Graph<APEXBlock, CFGEdge> graph = new DefaultDirectedGraph<>(CFGEdge.class);
		Map<APEXMethod, CFG> methodCFGs = new HashMap<>();
		Set<APEXBlock> readers = new HashSet<>();
		Set<APEXBlock> writers = new HashSet<>();
		ICFG(APEXApp app) {
			for (APEXClass c : app.getNonLibraryClasses())
			for (APEXMethod m : c.methods.values()) {
				// first make intra-CFG for each read/write methods
				if (!methodCFGs.containsKey(m)) {
					CFG cfg = new CFG(m);
					methodCFGs.put(m, cfg);
					absorb(cfg);
				}
				// then connect:
				//   (1) invoke --> target method first block
				//   (2) target method return block --> statement following invoke
				for (APEXStatement s : m.statements)
				if (s.isInvokeStmt()) {
					String invokeSig = s.getInvokeSignature();
					if (SCOPE_READ.containsKey(invokeSig))
						readers.add(s.block);
					else if (SCOPE_WRITE.containsKey(invokeSig))
						writers.add(s.block);
					APEXMethod tgtM = app.getNonLibraryMethod(invokeSig);
					if (tgtM != null) {
						if (!methodCFGs.containsKey(tgtM)) {
							CFG cfg = new CFG(tgtM);
							methodCFGs.put(tgtM, cfg);
							absorb(cfg);
						}
						CFG tgtCFG = methodCFGs.get(tgtM);
						if (!tgtM.blocks.isEmpty() && tgtCFG.returnBlock!=null) {
							// remove the edge from invoke to next statement
							graph.removeEdge(s.block, s.getFlowThroughStmt().block);
							// connect edge from invoke to tgtM.firstBlock
							addEdge(s.block, tgtM.blocks.get(0), "invoke_start");
							// connect edge from tgtM.returnBlock to next statement
							addEdge(tgtCFG.returnBlock, s.getFlowThroughStmt().block, "invoke_return");
						}
					}
				}
			}
		}
		void absorb(CFG g) {
			for (APEXBlock b : g.graph.vertexSet())
				graph.addVertex(b);
			for (CFGEdge e : g.graph.edgeSet())
				graph.addEdge(e.from, e.to, e);
		}
		void addEdge(APEXBlock b1, APEXBlock b2, String label) {
			graph.addEdge(b1, b2, new CFGEdge(b1, b2, label));
		}
	}
	
	static void analyzeCFG(APEXApp app, String label) {
		ICFG icfg = new ICFG(app);
		P.p(app.packageName+"  "+icfg.readers.size()+"  "+icfg.writers.size());
		P.p("  -- readers --");
		for (APEXBlock b : icfg.readers)
			P.p("    "+b.m.signature+" "+b.statements.get(0).index);
		P.p("  -- writers --");
		for (APEXBlock b : icfg.writers)
			P.p("    "+b.m.signature+" "+b.statements.get(0).index);
		AllDirectedPaths<APEXBlock, CFGEdge> pathFinder = new AllDirectedPaths<>(icfg.graph);
		List<GraphPath<APEXBlock, CFGEdge>> paths = 
				pathFinder.getAllPaths(icfg.readers, icfg.writers, true, 100);
		Map<APEXBlock, Map<APEXBlock, Integer>> pathCount = new HashMap<APEXBlock, Map<APEXBlock,Integer>>();
		for (GraphPath<APEXBlock, CFGEdge> path : paths) {
			APEXBlock from = path.getStartVertex();
			APEXBlock to = path.getEndVertex();
			Map<APEXBlock, Integer> count = pathCount.computeIfAbsent(from, k-> new HashMap<>());
			count.put(to, count.getOrDefault(to, 0)+1);
		}
		P.p("  -- number of paths: "+paths.size());
		for (APEXBlock from : pathCount.keySet()) {
			P.p("   [from] "+from.statements.get(0).getUniqueID());
			Map<APEXBlock, Integer> count = pathCount.get(from);
			for (APEXBlock to : count.keySet())
				P.p("          "+count.get(to)+"  "+to.statements.get(0).getUniqueID());
		}
		
		P.pause();
	}
	

	public static void main(String[] args) {
		long time = System.currentTimeMillis();
		List<File> apks = Dirs.getStegoFiles();
	
		File appLabelFile = new File(Dirs.NotesRoot, "StegoApp_Labels.txt");
		Map<String, String> appLabels = new HashMap<String, String>();
		Set<String> stegoApps = new HashSet<String>();
		
		// load stego app labels
		for (String s : F.readLinesWithoutEmptyLines(appLabelFile)) {
			String[] partStrings = s.split(" , ");
			appLabels.put(partStrings[0], partStrings[1]);
			if (!partStrings[1].equals("N/A"))
				stegoApps.add(partStrings[0]);
		}
		P.p("labeled stego apps: "+stegoApps.size());
		
		for (File apk : apks) {
			APEXApp app = new APEXApp(apk);
			String appLabel = appLabels.get(apk.getName());
			if (!appLabel.equals("N/A"))
				analyzeCFG(app, appLabel);
		}
		time = (System.currentTimeMillis() - time)/1000;
		Mailjet.email("icfg done "+time+" seonds.");
		P.p("All Done. time: "+time+" seconds");
	}
	

}
