package bab.visualization;

import bab.algorithm.SearchResult;
import bab.algorithm.SearchResult.SearchStep;
import bab.model.Edge;
import bab.model.Node;
import bab.model.RoadNetwork;

import java.io.*;
import java.util.*;

/**
 * HTML Visualization Generator
 * 
 * Generates:
 * 1. graph_data.json - Graph data for JavaScript visualization
 * 2. search_trace.json - Algorithm execution trace
 * 3. Complete HTML visualization page
 */
public class HtmlVisualizer {

    private final RoadNetwork network;

    public HtmlVisualizer(RoadNetwork network) {
        this.network = network;
    }

    /**
     * Export graph data to JSON format
     */
    public void exportGraphToJson(String filename) throws IOException {
        System.out.println("Exporting graph data to: " + filename);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
            writer.println("{");

            // Export nodes
            writer.println("  \"nodes\": [");
            boolean firstNode = true;
            for (Node node : network.getAllNodes()) {
                if (!firstNode) writer.println(",");
                firstNode = false;
                
                String keywordsJson = node.getKeywords().stream()
                    .map(k -> "\"" + escapeJson(k) + "\"")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
                
                writer.print(String.format(
                    "    {\"id\": %d, \"lat\": %.7f, \"lon\": %.7f, \"keywords\": [%s]}",
                    node.getId(), node.getLat(), node.getLon(), keywordsJson
                ));
            }
            writer.println("\n  ],");

            // Export edges
            writer.println("  \"edges\": [");
            Set<String> addedEdges = new HashSet<>();
            boolean firstEdge = true;
            for (Node node : network.getAllNodes()) {
                for (Edge edge : network.getOutgoingEdges(node.getId())) {
                    String edgeKey = Math.min(edge.getFrom(), edge.getTo()) + "-" + 
                                     Math.max(edge.getFrom(), edge.getTo());
                    if (!addedEdges.contains(edgeKey)) {
                        addedEdges.add(edgeKey);
                        if (!firstEdge) writer.println(",");
                        firstEdge = false;
                        writer.print(String.format(
                            "    {\"from\": %d, \"to\": %d, \"weight\": %.2f}",
                            edge.getFrom(), edge.getTo(), edge.getWeight()
                        ));
                    }
                }
            }
            writer.println("\n  ],");

            // Export keyword index
            writer.println("  \"keywordIndex\": {");
            boolean firstKeyword = true;
            for (String keyword : network.getAllKeywords()) {
                Set<Long> nodeIds = network.getNodesWithKeyword(keyword);
                if (!firstKeyword) writer.println(",");
                firstKeyword = false;
                
                String nodeIdsJson = nodeIds.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
                
                writer.print(String.format("    \"%s\": [%s]", 
                    escapeJson(keyword), nodeIdsJson));
            }
            writer.println("\n  },");

            // Export stats
            writer.println("  \"stats\": {");
            writer.println("    \"nodeCount\": " + network.getNodeCount() + ",");
            writer.println("    \"edgeCount\": " + addedEdges.size() + ",");
            writer.println("    \"keywordCount\": " + network.getKeywordCount());
            writer.println("  }");

            writer.println("}");
        }

        System.out.println("Graph export complete!");
    }

    /**
     * Export search trace to JSON format
     */
    public void exportSearchTraceToJson(SearchResult result, String filename) throws IOException {
        System.out.println("Exporting search trace to: " + filename);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
            writer.println("{");
            writer.println("  \"result\": {");
            writer.println("    \"hasValidPath\": " + result.hasValidPath() + ",");
            writer.print("    \"bestPath\": [");
            writer.print(result.getBestPath().stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
            writer.println("],");
            writer.println("    \"matchingDistance\": " + 
                (result.getMatchingDistance() == Double.MAX_VALUE ? "null" : result.getMatchingDistance()) + ",");
            writer.println("    \"executionTimeMs\": " + result.getExecutionTimeMs());
            writer.println("  },");

            // Export steps
            writer.println("  \"steps\": [");
            boolean firstStep = true;
            for (SearchStep step : result.getSearchSteps()) {
                if (!firstStep) writer.println(",");
                firstStep = false;
                
                writer.println("    {");
                writer.println("      \"stepNumber\": " + step.getStepNumber() + ",");
                writer.println("      \"action\": \"" + step.getAction() + "\",");
                writer.print("      \"stackV\": [");
                writer.print(step.getStackV().stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                writer.println("],");
                writer.print("      \"stackD\": [");
                writer.print(step.getStackD().stream()
                    .map(d -> String.format("%.4f", d))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                writer.println("],");
                writer.println("      \"upperBound\": " + 
                    (step.getUpperBound() == Double.MAX_VALUE ? "null" : step.getUpperBound()) + ",");
                writer.println("      \"candidate\": " + 
                    (step.getCandidate() == null ? "null" : step.getCandidate()) + ",");
                writer.println("      \"candidateMatchingDist\": " + 
                    (step.getCandidateMatchingDist() == null ? "null" : 
                     String.format("%.4f", step.getCandidateMatchingDist())) + ",");
                writer.println("      \"accepted\": " + step.isAccepted() + ",");
                writer.println("      \"reason\": \"" + escapeJson(step.getReason()) + "\"");
                writer.print("    }");
            }
            writer.println("\n  ]");

            writer.println("}");
        }

        System.out.println("Search trace export complete!");
    }

    /**
     * Generate complete HTML visualization file with embedded data
     */
    public void generateHtmlVisualization(String htmlFilename, String graphJsonFilename) throws IOException {
        System.out.println("Generating HTML visualization: " + htmlFilename);

        // Read graph data
        StringBuilder graphData = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(graphJsonFilename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                graphData.append(line).append("\n");
            }
        }

        // Generate HTML with embedded data
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(htmlFilename)))) {
            writer.println(generateHtmlContent(graphData.toString()));
        }

        System.out.println("HTML visualization generated!");
    }

    /**
     * Generate HTML content with embedded graph data
     */
    private String generateHtmlContent(String graphDataJson) {
        return getHtmlTemplate().replace("__GRAPH_DATA_PLACEHOLDER__", graphDataJson);
    }

    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Get HTML template with CDP-style UI
     */
    private String getHtmlTemplate() {
        return """
<!DOCTYPE html>
<html lang="zh-TW">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BAB Algorithm Visualization - 分支定界路徑搜尋</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; }
        .header { background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%); color: white; padding: 15px 20px; text-align: center; }
        .header h1 { font-size: 1.4em; }
        .header p { font-size: 0.85em; opacity: 0.9; }
        .main-container { display: flex; height: calc(100vh - 70px); }
        .left-panel { width: 420px; background: white; overflow-y: auto; border-right: 2px solid #dee2e6; padding: 15px; }
        #map { flex: 1; }
        .panel { background: #fafafa; margin-bottom: 15px; padding: 15px; border-radius: 8px; border: 1px solid #e0e0e0; }
        .panel h3 { color: #2c3e50; font-size: 1em; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 2px solid #3498db; }
        .info-row { display: flex; justify-content: space-between; padding: 5px 0; }
        .info-label { color: #666; }
        .info-value { font-weight: bold; color: #2c3e50; }
        .clue-badge { display: inline-block; background: #3498db; color: white; padding: 3px 10px; border-radius: 12px; font-size: 0.85em; margin: 2px; }
        .step-table { width: 100%; border-collapse: collapse; font-size: 0.8em; margin-top: 10px; }
        .step-table th, .step-table td { border: 1px solid #ddd; padding: 6px; text-align: center; }
        .step-table th { background: #3498db; color: white; }
        .step-table .accepted { background: #d4edda; border-left: 4px solid #27ae60; }
        .step-table .rejected { background: #f8d7da; border-left: 4px solid #e74c3c; }
        .legend-item { display: flex; align-items: center; margin: 5px 0; }
        .legend-dot { width: 14px; height: 14px; border-radius: 50%; margin-right: 8px; border: 2px solid white; box-shadow: 0 1px 3px rgba(0,0,0,0.3); }
        .path-step { display: flex; align-items: center; padding: 8px; margin: 5px 0; background: #e8f4f8; border-radius: 6px; border-left: 4px solid #3498db; cursor: pointer; }
        .path-step:hover { transform: translateX(5px); background: #d4edda; }
        .path-step.optimal { border-left-color: #27ae60; background: #d4edda; }
        .step-num { width: 24px; height: 24px; background: #3498db; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 10px; font-size: 0.8em; }
        .path-step.optimal .step-num { background: #27ae60; }
        .btn { background: #3498db; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin: 3px; }
        .btn:hover { background: #2980b9; }
        .btn-success { background: #27ae60; }
        .loading-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(44, 62, 80, 0.95); display: flex; align-items: center; justify-content: center; z-index: 1000; color: white; }
    </style>
</head>
<body>
<div class="loading-overlay" id="loadingOverlay"><div>載入中...</div></div>
<div class="header">
    <h1>🔍 Algorithm 3: BAB - 分支定界線索路徑搜尋</h1>
    <p>Branch and Bound | <span id="headerResult">請執行查詢</span></p>
</div>
<div class="main-container">
<div class="left-panel">
    <div class="panel">
        <h3>📍 查詢設定</h3>
        <select id="sourceSelect" style="width:100%;padding:8px;margin-bottom:10px;"><option>載入中...</option></select>
        <div id="cluesContainer"></div>
        <button class="btn" onclick="addClue()">+ 新增線索</button>
        <button class="btn btn-success" style="width:100%;margin-top:10px;" onclick="runBAB()">▶ 執行 BAB</button>
    </div>
    <div class="panel" id="resultPanel" style="display:none;">
        <h3>🏆 搜尋結果</h3>
        <div id="resultBanner"></div>
        <div id="optimalPath"></div>
    </div>
    <div class="panel">
        <h3>🎨 圖例</h3>
        <div class="legend-item"><div class="legend-dot" style="background:#e74c3c;"></div>起點</div>
        <div class="legend-item"><div class="legend-dot" style="background:#27ae60;"></div>最優路徑</div>
        <div class="legend-item"><div class="legend-dot" style="background:#f39c12;"></div>候選節點</div>
    </div>
    <div class="panel">
        <h3>📊 搜尋步驟</h3>
        <button class="btn btn-success" onclick="animateSteps()">▶ 動畫</button>
        <button class="btn" onclick="resetAnim()">⟲ 重置</button>
        <div id="stepTableContainer" style="max-height:250px;overflow:auto;margin-top:10px;">
            <table class="step-table"><thead><tr><th>步驟</th><th>動作</th><th>UB</th><th>狀態</th></tr></thead><tbody id="stepTableBody"></tbody></table>
        </div>
    </div>
</div>
<div id="map"></div>
</div>
<script>
const EMBEDDED_GRAPH_DATA = __GRAPH_DATA_PLACEHOLDER__;
let graphData, map, nodes = new Map(), adjacencyList = new Map(), keywordIndex = new Map(), distanceCache = new Map(), searchSteps = [], pathMarkers = [];

function initMap() {
    map = L.map('map').setView([25.016, 121.543], 16);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {attribution: '© OSM'}).addTo(map);
}

function createIcon(color, size) {
    return L.divIcon({className:'', html:'<div style="background:'+color+';width:'+size+'px;height:'+size+'px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"></div>', iconSize:[size,size], iconAnchor:[size/2,size/2]});
}

async function loadGraphData() {
    graphData = EMBEDDED_GRAPH_DATA;
    for (const n of graphData.nodes) { nodes.set(n.id, n); adjacencyList.set(n.id, []); }
    for (const e of graphData.edges) { adjacencyList.get(e.from)?.push(e); adjacencyList.get(e.to)?.push({from:e.to,to:e.from,weight:e.weight}); }
    for (const [k,v] of Object.entries(graphData.keywordIndex)) keywordIndex.set(k.toLowerCase(), new Set(v));
    drawNetwork();
    populateSelect();
    addClue(); addClue();
    document.getElementById('loadingOverlay').style.display = 'none';
}

function drawNetwork() {
    const lines = [], added = new Set();
    for (const e of graphData.edges) {
        const k = Math.min(e.from,e.to)+'-'+Math.max(e.from,e.to);
        if (!added.has(k)) { added.add(k); const f=nodes.get(e.from), t=nodes.get(e.to); if(f&&t) lines.push([[f.lat,f.lon],[t.lat,t.lon]]); }
    }
    L.polyline(lines, {color:'#bdc3c7',weight:1}).addTo(map);
}

function populateSelect() {
    const sel = document.getElementById('sourceSelect');
    sel.innerHTML = '<option value="">選擇起點...</option>';
    for (const [id,edges] of adjacencyList) if(edges.length>0) { const n=nodes.get(id); const o=document.createElement('option'); o.value=id; o.textContent=id+' ('+(n.keywords?.slice(0,2).join(',')||'-')+')'; sel.appendChild(o); if(sel.options.length>200) break; }
}

function addClue() {
    const c = document.getElementById('cluesContainer'), d = document.createElement('div');
    d.style.cssText = 'background:#f8f9fa;border:1px solid #ddd;border-radius:6px;padding:10px;margin-bottom:8px;';
    d.innerHTML = '<input class="clue-kw" placeholder="關鍵字" style="width:100%;padding:6px;margin-bottom:4px;"><div style="display:flex;gap:8px;"><input class="clue-d" type="number" value="150" style="flex:1;padding:6px;" placeholder="距離m"><input class="clue-e" type="number" value="0.5" step="0.1" style="flex:1;padding:6px;" placeholder="ε"></div>';
    c.appendChild(d);
}

class MinHeap { constructor(){this.h=[];} ins(i){this.h.push(i);this.up(this.h.length-1);} ext(){if(!this.h.length)return null;const m=this.h[0],l=this.h.pop();if(this.h.length){this.h[0]=l;this.dn(0);}return m;} emp(){return!this.h.length;} up(i){while(i>0){const p=Math.floor((i-1)/2);if(this.h[p].d<=this.h[i].d)break;[this.h[p],this.h[i]]=[this.h[i],this.h[p]];i=p;}} dn(i){const n=this.h.length;while(1){let s=i,l=2*i+1,r=2*i+2;if(l<n&&this.h[l].d<this.h[s].d)s=l;if(r<n&&this.h[r].d<this.h[s].d)s=r;if(s===i)break;[this.h[s],this.h[i]]=[this.h[i],this.h[s]];i=s;}}}

function getDist(f,t){const k=f+'-'+t;if(distanceCache.has(k))return distanceCache.get(k);if(f===t)return 0;const pq=new MinHeap(),ds=new Map(),vs=new Set();pq.ins({d:0,n:f});ds.set(f,0);while(!pq.emp()){const{d,n}=pq.ext();if(n===t){distanceCache.set(k,d);distanceCache.set(t+'-'+f,d);return d;}if(vs.has(n))continue;vs.add(n);for(const e of adjacencyList.get(n)||[])if(!vs.has(e.to)){const nd=d+e.weight;if(nd<(ds.get(e.to)||Infinity)){ds.set(e.to,nd);pq.ins({d:nd,n:e.to});}}}distanceCache.set(k,Infinity);return Infinity;}

function calcDm(nd,c){return Math.abs(nd-c.distance)/(c.epsilon*c.distance);}

function findNext(fv,c,th,ex){const cands=keywordIndex.get(c.keyword)||new Set();if(!cands.size)return{found:false};const mn=c.distance*(1-c.epsilon),mx=c.distance*(1+c.epsilon);let best={found:false,v:-1,dm:Infinity};for(const cd of cands){if(ex.has(cd))continue;const nd=getDist(fv,cd);if(nd<mn||nd>mx||nd===Infinity)continue;const dm=calcDm(nd,c);if(dm>=th&&dm<best.dm)best={found:true,v:cd,dm,nd};}return best;}

function runBAB(){const sv=parseInt(document.getElementById('sourceSelect').value);if(!sv){alert('請選擇起點');return;}const clues=[];document.querySelectorAll('#cluesContainer > div').forEach(d=>{const kw=d.querySelector('.clue-kw')?.value.toLowerCase().trim(),di=parseFloat(d.querySelector('.clue-d')?.value),ep=parseFloat(d.querySelector('.clue-e')?.value);if(kw&&!isNaN(di)&&!isNaN(ep))clues.push({keyword:kw,distance:di,epsilon:ep});});if(!clues.length){alert('請新增線索');return;}
searchSteps=[];pathMarkers.forEach(m=>map.removeLayer(m));pathMarkers=[];let sc=0;const stV=[sv],stD=[];let th=0,ub=Infinity,bp=[],bdm=Infinity,k=clues.length;const exL=new Map();for(let i=0;i<=k;i++)exL.set(i,new Set());
searchSteps.push({s:++sc,a:'INIT',v:[...stV],d:[...stD],ub,ok:true,r:'初始化'});let it=0;
while(stV.length>0&&it<5000){it++;const lv=stV.length;if(lv>k){stV.pop();if(stD.length)stD.pop();continue;}const cv=stV[stV.length-1],cc=clues[lv-1],ex=exL.get(lv),res=findNext(cv,cc,th,ex);
if(res.found){if(res.dm>ub){searchSteps.push({s:++sc,a:'PRUNE',v:[...stV],d:[...stD],ub,cd:res.v,cdm:res.dm,ok:false,r:'超過UB'});ex.add(res.v);stV.pop();if(stD.length)th=stD.pop();else th=0;continue;}
searchSteps.push({s:++sc,a:'PUSH',v:[...stV],d:[...stD],ub,cd:res.v,cdm:res.dm,ok:true,r:'找到候選'});th=0;stV.push(res.v);stD.push(res.dm);
if(stV.length===k+1){const mx=Math.max(...stD);if(mx<=ub){ub=mx;bp=[...stV];bdm=mx;searchSteps.push({s:++sc,a:'UPDATE',v:[...stV],d:[...stD],ub,ok:true,r:'更新UB'});}ex.add(stV.pop());stD.pop();if(stV.length>1){exL.get(lv-1).add(stV.pop());if(stD.length)th=stD.pop();}exL.get(lv).clear();}}
else{searchSteps.push({s:++sc,a:'BACK',v:[...stV],d:[...stD],ub,ok:false,r:'回溯'});stV.pop();if(stD.length)th=stD.pop();exL.get(lv).clear();}}
searchSteps.push({s:++sc,a:'DONE',v:bp,d:[],ub:bdm,ok:bp.length>0,r:bp.length?'完成':'無路徑'});
displayResults(bp,bdm);displaySteps();visualize(bp,sv);}

function displayResults(bp,dm){const rp=document.getElementById('resultPanel'),rb=document.getElementById('resultBanner'),op=document.getElementById('optimalPath');rp.style.display='block';
if(bp.length){rb.innerHTML='<div style="background:#d4edda;padding:10px;border-radius:6px;border:1px solid #27ae60;"><strong style="color:#27ae60;">✓ 找到最優路徑</strong><br>d_m = '+dm.toFixed(4)+'</div>';
op.innerHTML=bp.map((id,i)=>{const n=nodes.get(id);return '<div class="path-step optimal" onclick="focusNode('+n?.lat+','+n?.lon+')"><div class="step-num">'+i+'</div><div><strong>'+(i?'匹配'+i:'起點')+'</strong><br><small>Node-'+id+'</small></div></div>';}).join('');
document.getElementById('headerResult').textContent='最優匹配距離: '+dm.toFixed(4);}
else{rb.innerHTML='<div style="background:#fff3cd;padding:10px;border-radius:6px;border:1px solid #f39c12;"><strong style="color:#f39c12;">⚠ 無可行路徑</strong></div>';op.innerHTML='';document.getElementById('headerResult').textContent='無可行路徑';}}

function displaySteps(){document.getElementById('stepTableBody').innerHTML=searchSteps.map((s,i)=>'<tr class="'+(s.ok?'accepted':'rejected')+'" data-i="'+i+'"><td>'+s.s+'</td><td>'+s.a+'</td><td>'+(s.ub===Infinity?'∞':s.ub.toFixed(3))+'</td><td>'+(s.ok?'✓':'✗')+'</td></tr>').join('');}

function visualize(bp,sv){const sn=nodes.get(sv);if(sn){const m=L.marker([sn.lat,sn.lon],{icon:createIcon('#e74c3c',18)}).addTo(map).bindPopup('起點');pathMarkers.push(m);}
if(bp.length>1){const coords=bp.map(id=>{const n=nodes.get(id);return n?[n.lat,n.lon]:null;}).filter(c=>c);const line=L.polyline(coords,{color:'#27ae60',weight:5}).addTo(map);pathMarkers.push(line);
for(let i=1;i<bp.length;i++){const n=nodes.get(bp[i]);if(n){const m=L.marker([n.lat,n.lon],{icon:createIcon('#27ae60',14)}).addTo(map).bindPopup('匹配'+i);pathMarkers.push(m);}}
map.fitBounds(L.polyline(coords).getBounds().pad(0.2));}}

function focusNode(lat,lon){map.setView([lat,lon],17);L.popup().setLatLng([lat,lon]).setContent('選中位置').openOn(map);}

let animIdx=0,animInt=null;
function animateSteps(){resetAnim();animInt=setInterval(()=>{if(animIdx>=searchSteps.length){clearInterval(animInt);return;}const s=searchSteps[animIdx];document.querySelectorAll('.step-table tr[data-i]').forEach(r=>r.style.outline='');const row=document.querySelector('.step-table tr[data-i="'+animIdx+'"]');if(row)row.style.outline='3px solid #e74c3c';if(s.cd){const n=nodes.get(s.cd);if(n){const m=L.marker([n.lat,n.lon],{icon:createIcon(s.ok?'#27ae60':'#f39c12',10)}).addTo(map);pathMarkers.push(m);}}animIdx++;},400);}
function resetAnim(){if(animInt)clearInterval(animInt);animIdx=0;document.querySelectorAll('.step-table tr').forEach(r=>r.style.outline='');}

document.addEventListener('DOMContentLoaded',()=>{initMap();loadGraphData();});
</script>
</body>
</html>
""";
    }
}
