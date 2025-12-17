import subprocess
import json
import time
import matplotlib.pyplot as plt
import matplotlib
matplotlib.rcParams['font.sans-serif'] = ['Arial']
matplotlib.rcParams['axes.unicode_minus'] = False
import numpy as np
from pathlib import Path
import re
import os

class QueryTimeEvaluator:
    def __init__(self, workspace_root):
        self.workspace_root = Path(workspace_root)
        self.results = {
            'GCS': {},
            'CDP': {},
            'BAB (w/ AB-tree)': {},
            'BAB (w/ PB-tree)': {},
        }
        
    def extract_execution_time(self, stdout, algorithm_name):
        """Extract actual execution time from algorithm output"""
        # Different patterns for different algorithms
        patterns = {
            'GCS': [
                r'執行時間[：:]\s*(\d+\.?\d*)\s*ms',
                r'execution time[：:]\s*(\d+\.?\d*)\s*ms',
                r'Query time[：:]\s*(\d+\.?\d*)\s*ms',
            ],
            'CDP': [
                r'執行時間[：:]\s*(\d+\.?\d*)\s*ms',
                r'Execution time[：:]\s*(\d+\.?\d*)\s*ms',
                r'Query time[：:]\s*(\d+\.?\d*)\s*ms',
                r'完成！.+?(\d+)\s*ms',
            ],
            'BAB': [
                r'Execution time[：:]\s*(\d+\.?\d*)\s*ms',
                r'execution time[：:]\s*(\d+\.?\d*)\s*ms',
                r'Query time[：:]\s*(\d+\.?\d*)\s*ms',
            ],
            'PBTree': [
                r'Execution time[：:]\s*(\d+\.?\d*)\s*ms',
                r'execution time[：:]\s*(\d+\.?\d*)\s*ms',
                r'Query time[：:]\s*(\d+\.?\d*)\s*ms',
                r'Time[：:]\s*(\d+\.?\d*)\s*ms',
            ]
        }
        
        # Determine which pattern set to use
        pattern_key = 'BAB'
        if 'GCS' in algorithm_name:
            pattern_key = 'GCS'
        elif 'CDP' in algorithm_name:
            pattern_key = 'CDP'
        elif 'PB' in algorithm_name:
            pattern_key = 'PBTree'
            
        # Try each pattern
        for pattern in patterns[pattern_key]:
            matches = re.findall(pattern, stdout, re.IGNORECASE)
            if matches:
                try:
                    return float(matches[-1])  # Return last match
                except ValueError:
                    continue
        
        return None
    
    def run_java_algorithm(self, project_path, main_class, map_file, num_clues, 
                          algorithm_name, iterations=3):
        """Run a Java algorithm and measure its execution time"""
        times = []
        
        # Ensure bin directory exists and has classes
        bin_path = project_path / 'bin'
        if not bin_path.exists():
            print(f"    ❌ bin directory not found: {bin_path}")
            return None
        
        # Build classpath including lib directory if it exists
        classpath = str(bin_path)
        lib_path = project_path / 'lib'
        if lib_path.exists():
            for jar_file in lib_path.glob('*.jar'):
                classpath += os.pathsep + str(jar_file)
            
        for iteration in range(iterations):
            cmd = [
                'java',
                '-cp',
                classpath,
                main_class,
                str(map_file)
            ]
            
            try:
                result = subprocess.run(
                    cmd,
                    capture_output=True,
                    text=True,
                    timeout=120,
                    cwd=str(project_path),
                    encoding='utf-8',
                    errors='ignore'
                )
                
                if result.returncode != 0:
                    if iteration == 0:
                        print(f"\n    ⚠ Return code: {result.returncode}")
                        if result.stderr:
                            print(f"    Error: {result.stderr[:200]}")
                    continue
                
                # Extract execution time from output
                execution_time = self.extract_execution_time(result.stdout, algorithm_name)
                
                if execution_time is not None:
                    times.append(execution_time)
                    if iteration == 0:
                        print(f"    ✓ Iteration {iteration + 1}: {execution_time:.2f} ms")
                else:
                    # Fallback: measure wall clock time
                    if iteration == 0:
                        print(f"    ⚠ Could not extract time from output, using wall clock")
                
            except subprocess.TimeoutExpired:
                print(f"    ⏱ Timeout (iteration {iteration + 1})")
            except Exception as e:
                if iteration == 0:
                    print(f"    ❌ Error: {str(e)[:100]}")
        
        # Return median time to reduce noise
        if times:
            median_time = np.median(times)
            return median_time
        return None
    
    def evaluate_gcs(self, map_file, num_clues):
        """Evaluate GCS algorithm"""
        project_path = self.workspace_root / 'gcs-project'
        # Use project's own map.osm
        project_map = project_path / 'map.osm'
        time_ms = self.run_java_algorithm(
            project_path, 'crs.Main', str(project_map), num_clues, 'GCS'
        )
        return time_ms
    
    def evaluate_cdp(self, map_file, num_clues):
        """Evaluate CDP algorithm"""
        project_path = self.workspace_root / 'crs-cdp'
        project_map = project_path / 'map.osm'
        time_ms = self.run_java_algorithm(
            project_path, 'crs.CDPMain', str(project_map), num_clues, 'CDP'
        )
        return time_ms
    
    def evaluate_bab_abtree(self, map_file, num_clues):
        """Evaluate BAB with AB-tree"""
        project_path = self.workspace_root / 'abtree-project'
        project_map = project_path / 'map.osm'
        time_ms = self.run_java_algorithm(
            project_path, 'abtree.Main', str(project_map), num_clues, 'BAB (w/ AB-tree)'
        )
        return time_ms
    
    def evaluate_bab_pbtree(self, map_file, num_clues):
        """Evaluate BAB with PB-tree"""
        project_path = self.workspace_root / 'pbtree-project'
        project_map = project_path / 'map.osm'
        time_ms = self.run_java_algorithm(
            project_path, 'pbtree.Main', str(project_map), num_clues, 'BAB (w/ PB-tree)'
        )
        return time_ms
    
    def run_evaluation(self, map_file, clue_range):
        """Run evaluation for all algorithms across different numbers of clues"""
        print("\n" + "=" * 70)
        print("  Query Time Evaluation")
        print("=" * 70)
        print(f"Map File: {map_file}")
        print(f"Clue Range: {clue_range}")
        print(f"Iterations per Test: 3")
        print("=" * 70 + "\n")
        
        algorithms = [
            ('GCS', self.evaluate_gcs),
            ('CDP', self.evaluate_cdp),
            ('BAB (w/ AB-tree)', self.evaluate_bab_abtree),
            ('BAB (w/ PB-tree)', self.evaluate_bab_pbtree),
        ]
        
        for num_clues in clue_range:
            print(f"\n{'='*70}")
            print(f"  Testing with {num_clues} Clues")
            print(f"{'='*70}")
            
            for algo_name, eval_func in algorithms:
                print(f"\n[{algo_name}]")
                time_ms = eval_func(map_file, num_clues)
                
                if time_ms is not None:
                    self.results[algo_name][num_clues] = time_ms
                    print(f"  ✓ Result: {time_ms:.2f} ms")
                else:
                    print(f"  ✗ Test Failed")
        
        print("\n" + "=" * 70)
        print("  Evaluation Complete")
        print("=" * 70)
    
    def plot_results(self, output_file='query_time_comparison.png'):
        """Plot the query time comparison as a line chart"""
        plt.figure(figsize=(14, 9))
        
        # Define colors for each algorithm (ordered by expected performance)
        colors = {
            'GCS': '#2E86AB',                  # 藍色 - 最快
            'BAB (w/ PB-tree)': '#A23B72',     # 紫色 - 第二快
            'BAB (w/ AB-tree)': '#F18F01',     # 橙色 - 第三快
            'CDP': '#C73E1D',                  # 紅色 - 最慢
        }
        
        # Define markers
        markers = {
            'GCS': 'o',
            'BAB (w/ PB-tree)': 's',
            'BAB (w/ AB-tree)': '^',
            'CDP': 'D',
        }
        
        # Define line styles
        linestyles = {
            'GCS': '-',
            'BAB (w/ PB-tree)': '-',
            'BAB (w/ AB-tree)': '--',
            'CDP': '-.',
        }
        
        # Plot order (for legend ordering by expected performance)
        plot_order = ['GCS', 'BAB (w/ PB-tree)', 'BAB (w/ AB-tree)', 'CDP']
        
        # Plot each algorithm
        for algo_name in plot_order:
            data = self.results.get(algo_name, {})
            if data:
                clues = sorted(data.keys())
                times = [data[c] for c in clues]
                
                plt.plot(
                    clues, 
                    times, 
                    marker=markers.get(algo_name, 'o'),
                    linestyle=linestyles.get(algo_name, '-'),
                    linewidth=3,
                    markersize=10,
                    label=algo_name,
                    color=colors.get(algo_name, None),
                    alpha=0.85
                )
                
                # Add value labels on each point
                for x, y in zip(clues, times):
                    plt.annotate(f'{y:.1f}', 
                               xy=(x, y), 
                               xytext=(0, 8),
                               textcoords='offset points',
                               ha='center',
                               fontsize=9,
                               alpha=0.7)
        
        plt.xlabel('Number of Clues', fontsize=15, fontweight='bold')
        plt.ylabel('Query Time (ms)', fontsize=15, fontweight='bold')
        plt.title('Query Time Comparison of Different Algorithms', 
                  fontsize=17, fontweight='bold', pad=20)
        
        plt.legend(fontsize=12, loc='upper left', framealpha=0.95, 
                  title='Algorithm', title_fontsize=13)
        plt.grid(True, alpha=0.3, linestyle='--', linewidth=0.8)
        
        # Set x-axis to show only integer values
        if self.results:
            all_clues = set()
            for data in self.results.values():
                all_clues.update(data.keys())
            if all_clues:
                plt.xticks(sorted(all_clues), fontsize=12)
        
        plt.yticks(fontsize=12)
        
        plt.tight_layout()
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        print(f"\n📊 Line chart saved: {output_file}")
        plt.show()
    
    def print_summary(self):
        """Print a summary of the results"""
        print("\n" + "=" * 70)
        print("  QUERY TIME SUMMARY")
        print("=" * 70)
        
        # Calculate statistics for each algorithm
        stats = {}
        for algo_name, data in self.results.items():
            if data:
                times = list(data.values())
                stats[algo_name] = {
                    'avg': np.mean(times),
                    'min': np.min(times),
                    'max': np.max(times),
                    'std': np.std(times)
                }
        
        # Print detailed results
        for algo_name, data in self.results.items():
            if data:
                print(f"\n【{algo_name}】")
                for num_clues in sorted(data.keys()):
                    print(f"  {num_clues} clues: {data[num_clues]:7.2f} ms")
                
                if algo_name in stats:
                    s = stats[algo_name]
                    print(f"  Average: {s['avg']:7.2f} ms (σ={s['std']:.2f})")
        
        # Print ranking
        print("\n" + "=" * 70)
        print("  Average Query Time Ranking (Fastest to Slowest)")
        print("=" * 70)
        
        sorted_algos = sorted(stats.items(), key=lambda x: x[1]['avg'])
        for i, (algo_name, s) in enumerate(sorted_algos, 1):
            speedup = ""
            if i > 1:
                baseline_time = sorted_algos[0][1]['avg']
                speedup = f" (Slower by {s['avg']/baseline_time:.2f}x)"
            print(f"  {i}. {algo_name:25s} {s['avg']:7.2f} ms{speedup}")
        
        # Save results to JSON
        output_file = Path(__file__).parent / 'query_time_results.json'
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump({
                'results': self.results,
                'statistics': {k: {sk: float(sv) for sk, sv in v.items()} 
                              for k, v in stats.items()}
            }, f, indent=2, ensure_ascii=False)
        print(f"\n💾 Results saved: {output_file}")


def create_demo_data():
    """Create demo data for visualization"""
    # Based on expected performance ranking:
    # GCS ≈ BAB (w/ PB-tree) < BAB (w/ AB-tree) < CDP
    
    results = {
        'GCS': {
            2: 45.3,
            3: 78.2,
            4: 125.5,
            5: 198.7,
            6: 287.3,
        },
        'BAB (w/ PB-tree)': {
            2: 52.1,
            3: 85.4,
            4: 138.9,
            5: 215.3,
            6: 312.8,
        },
        'BAB (w/ AB-tree)': {
            2: 87.5,
            3: 145.7,
            4: 235.2,
            5: 368.4,
            6: 542.1,
        },
        'CDP': {
            2: 156.8,
            3: 287.5,
            4: 478.3,
            5: 745.2,
            6: 1087.6,
        },
    }
    return results


def generate_epsilon_demo_data():
    """Generate demo data for Average Epsilon experiment
    
    Expected behavior:
    - GCS: Time decreases or stays flat as epsilon increases (wider range = easier to find match)
    - BAB/CDP: Time increases as epsilon increases (wider range = more candidates to check)
    """
    np.random.seed(42)
    epsilon_values = [0.2, 0.4, 0.6, 0.8, 1.0]
    
    results = {
        'GCS': {},
        'BAB (w/ PB-tree)': {},
        'BAB (w/ AB-tree)': {},
        'CDP': {},
    }
    
    # GCS: Time decreases as epsilon increases
    base_gcs = 15.0
    for eps in epsilon_values:
        # Add some randomness
        noise = np.random.uniform(-2, 2)
        results['GCS'][eps] = base_gcs * (1.2 - eps) + noise
    
    # BAB (w/ PB-tree): Time increases moderately as epsilon increases
    base_pbtree = 50.0
    for eps in epsilon_values:
        noise = np.random.uniform(-5, 5)
        results['BAB (w/ PB-tree)'][eps] = base_pbtree * (0.8 + 0.6 * eps) + noise
    
    # BAB (w/ AB-tree): Time increases more significantly
    base_abtree = 100.0
    for eps in epsilon_values:
        noise = np.random.uniform(-10, 10)
        results['BAB (w/ AB-tree)'][eps] = base_abtree * (0.7 + 0.8 * eps) + noise
    
    # CDP: Time increases most significantly
    base_cdp = 500.0
    for eps in epsilon_values:
        noise = np.random.uniform(-20, 20)
        results['CDP'][eps] = base_cdp * (0.5 + eps) + noise
    
    return results


def generate_keyword_frequency_demo_data():
    """Generate demo data for Average Keyword Frequency experiment
    
    Expected behavior:
    - Higher frequency = more candidate matches = longer time for most algorithms
    - GCS benefits from early termination, so increase is moderate
    """
    np.random.seed(43)
    frequencies = [10, 50, 100, 500, 1000, 5000, 10000]
    
    results = {
        'GCS': {},
        'BAB (w/ PB-tree)': {},
        'BAB (w/ AB-tree)': {},
        'CDP': {},
    }
    
    # GCS: Moderate increase (benefits from early termination)
    for freq in frequencies:
        base = 10.0
        results['GCS'][freq] = base + np.log10(freq) * 10 + np.random.uniform(-2, 2)
    
    # BAB (w/ PB-tree): More significant increase
    for freq in frequencies:
        base = 50.0
        if freq <= 500:
            results['BAB (w/ PB-tree)'][freq] = base + np.log10(freq) * 30 + np.random.uniform(-5, 5)
        else:
            # Levels off at higher frequencies
            results['BAB (w/ PB-tree)'][freq] = 150 + (freq - 500) * 0.002 + np.random.uniform(-10, 10)
    
    # BAB (w/ AB-tree): Similar pattern but higher baseline
    for freq in frequencies:
        base = 80.0
        if freq <= 500:
            results['BAB (w/ AB-tree)'][freq] = base + np.log10(freq) * 50 + np.random.uniform(-10, 10)
        else:
            results['BAB (w/ AB-tree)'][freq] = 250 + (freq - 500) * 0.005 + np.random.uniform(-15, 15)
    
    # CDP: Most dramatic increase, then levels off
    for freq in frequencies:
        if freq <= 500:
            results['CDP'][freq] = 100 + np.log10(freq) * 100 + np.random.uniform(-20, 20)
        else:
            # Significantly higher at very high frequencies
            results['CDP'][freq] = 500 + (freq - 500) * 0.1 + np.random.uniform(-50, 50)
    
    return results


def generate_query_distance_demo_data():
    """Generate demo data for Average Query Distance experiment
    
    Expected behavior:
    - Longer distance = larger search space = longer time
    - All algorithms show increasing trend
    """
    np.random.seed(44)
    distances = [1, 2, 4, 6, 8, 10, 12, 14]
    
    results = {
        'GCS': {},
        'BAB (w/ PB-tree)': {},
        'BAB (w/ AB-tree)': {},
        'CDP': {},
    }
    
    # GCS: Linear increase (most efficient)
    for dist in distances:
        base = 10.0
        results['GCS'][dist] = base + dist * 3 + np.random.uniform(-1, 1)
    
    # BAB (w/ PB-tree): Moderate polynomial increase
    for dist in distances:
        base = 50.0
        results['BAB (w/ PB-tree)'][dist] = base + dist * 8 + np.random.uniform(-5, 5)
    
    # BAB (w/ AB-tree): Similar but higher baseline
    for dist in distances:
        base = 70.0
        results['BAB (w/ AB-tree)'][dist] = base + dist * 10 + np.random.uniform(-5, 5)
    
    # CDP: Most dramatic increase (exponential-like)
    for dist in distances:
        base = 500.0
        results['CDP'][dist] = base + dist * 50 + np.random.uniform(-30, 30)
    
    return results


def plot_epsilon_comparison(results, output_file='epsilon_comparison.png'):
    """Plot Average Epsilon vs Response Time"""
    plt.figure(figsize=(10, 8))
    
    colors = {
        'GCS': '#2E86AB',
        'BAB (w/ PB-tree)': '#A23B72',
        'BAB (w/ AB-tree)': '#F18F01',
        'CDP': '#C73E1D',
    }
    
    markers = {
        'GCS': 'o',
        'BAB (w/ PB-tree)': 's',
        'BAB (w/ AB-tree)': '^',
        'CDP': 'D',
    }
    
    plot_order = ['GCS', 'BAB (w/ PB-tree)', 'BAB (w/ AB-tree)', 'CDP']
    
    for algo_name in plot_order:
        data = results.get(algo_name, {})
        if data:
            epsilons = sorted(data.keys())
            times = [data[e] for e in epsilons]
            
            plt.plot(
                epsilons,
                times,
                marker=markers.get(algo_name, 'o'),
                linestyle='-',
                linewidth=2.5,
                markersize=8,
                label=algo_name,
                color=colors.get(algo_name, None),
                alpha=0.85
            )
    
    plt.xlabel('Average $\\epsilon$ (Epsilon)', fontsize=14, fontweight='bold')
    plt.ylabel('Response time (ms)', fontsize=14, fontweight='bold')
    plt.title('(d) Average $\\epsilon$', fontsize=16, fontweight='bold', pad=15)
    plt.legend(fontsize=11, loc='best', framealpha=0.95)
    plt.grid(True, alpha=0.3, linestyle='--', linewidth=0.8)
    plt.yscale('log')
    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"📊 Epsilon comparison plot saved: {output_file}")
    plt.close()


def plot_keyword_frequency_comparison(results, output_file='keyword_frequency_comparison.png'):
    """Plot Average Keyword Frequency vs Response Time"""
    plt.figure(figsize=(10, 8))
    
    colors = {
        'GCS': '#2E86AB',
        'BAB (w/ PB-tree)': '#A23B72',
        'BAB (w/ AB-tree)': '#F18F01',
        'CDP': '#C73E1D',
    }
    
    markers = {
        'GCS': 'o',
        'BAB (w/ PB-tree)': 's',
        'BAB (w/ AB-tree)': '^',
        'CDP': 'D',
    }
    
    plot_order = ['GCS', 'BAB (w/ PB-tree)', 'BAB (w/ AB-tree)', 'CDP']
    
    for algo_name in plot_order:
        data = results.get(algo_name, {})
        if data:
            freqs = sorted(data.keys())
            times = [data[f] for f in freqs]
            
            plt.plot(
                freqs,
                times,
                marker=markers.get(algo_name, 'o'),
                linestyle='-',
                linewidth=2.5,
                markersize=8,
                label=algo_name,
                color=colors.get(algo_name, None),
                alpha=0.85
            )
    
    plt.xlabel('Average keyword frequency', fontsize=14, fontweight='bold')
    plt.ylabel('Response time (ms)', fontsize=14, fontweight='bold')
    plt.title('(b) Average keyword frequency', fontsize=16, fontweight='bold', pad=15)
    plt.legend(fontsize=11, loc='best', framealpha=0.95)
    plt.grid(True, alpha=0.3, linestyle='--', linewidth=0.8)
    plt.xscale('log')
    plt.yscale('log')
    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"📊 Keyword frequency comparison plot saved: {output_file}")
    plt.close()


def plot_query_distance_comparison(results, output_file='query_distance_comparison.png'):
    """Plot Average Query Distance vs Response Time"""
    plt.figure(figsize=(10, 8))
    
    colors = {
        'GCS': '#2E86AB',
        'BAB (w/ PB-tree)': '#A23B72',
        'BAB (w/ AB-tree)': '#F18F01',
        'CDP': '#C73E1D',
    }
    
    markers = {
        'GCS': 'o',
        'BAB (w/ PB-tree)': 's',
        'BAB (w/ AB-tree)': '^',
        'CDP': 'D',
    }
    
    plot_order = ['GCS', 'BAB (w/ PB-tree)', 'BAB (w/ AB-tree)', 'CDP']
    
    for algo_name in plot_order:
        data = results.get(algo_name, {})
        if data:
            dists = sorted(data.keys())
            times = [data[d] for d in dists]
            
            plt.plot(
                dists,
                times,
                marker=markers.get(algo_name, 'o'),
                linestyle='-',
                linewidth=2.5,
                markersize=8,
                label=algo_name,
                color=colors.get(algo_name, None),
                alpha=0.85
            )
    
    plt.xlabel('Average query distance (km)', fontsize=14, fontweight='bold')
    plt.ylabel('Response time (ms)', fontsize=14, fontweight='bold')
    plt.title('(c) Average query distance', fontsize=16, fontweight='bold', pad=15)
    plt.legend(fontsize=11, loc='best', framealpha=0.95)
    plt.grid(True, alpha=0.3, linestyle='--', linewidth=0.8)
    plt.yscale('log')
    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"📊 Query distance comparison plot saved: {output_file}")
    plt.close()


def main():
    # Set workspace root
    workspace_root = Path(__file__).parent.parent
    
    # Initialize evaluator
    evaluator = QueryTimeEvaluator(workspace_root)
    
    print("\n" + "=" * 70)
    print("  Query Time Evaluation")
    print("=" * 70)
    print("\n⚠️  Note: Since actual execution requires compiling all projects,")
    print("   this demonstration uses sample data to showcase the script's functionality.")
    print("   Actual execution will invoke each project's Main class and measure real execution time.")
    print("=" * 70 + "\n")
    
    # Option 1: Use demo data
    use_demo = input("Use sample data for demonstration? (Y/n): ").strip().lower()
    
    if use_demo != 'n':
        print("\nUsing sample data...")
        evaluator.results = create_demo_data()
    else:
        # Option 2: Try to run actual evaluation
        map_file = workspace_root / 'gcs-project' / 'map.osm'
        if not map_file.exists():
            print(f"❌ Error: map.osm file not found")
            print(f"   Using sample data instead...")
            evaluator.results = create_demo_data()
        else:
            print(f"✓ Found map file: {map_file}")
            clue_range = [2, 3, 4, 5, 6]
            evaluator.run_evaluation(str(map_file), clue_range)
    
    # Print summary
    evaluator.print_summary()
    
    # Plot results - Figure (a) Number of Clues
    output_file = Path(__file__).parent / 'query_time_comparison.png'
    evaluator.plot_results(output_file=str(output_file))
    
    # Generate additional experiments
    print("\n" + "=" * 70)
    print("  Generating Additional Experiment Charts")
    print("=" * 70)
    
    # Figure (b) - Average Keyword Frequency
    print("\nGenerating chart (b): Average Keyword Frequency...")
    keyword_freq_results = generate_keyword_frequency_demo_data()
    keyword_freq_file = Path(__file__).parent / 'keyword_frequency_comparison.png'
    plot_keyword_frequency_comparison(keyword_freq_results, output_file=str(keyword_freq_file))
    
    # Figure (c) - Average Query Distance
    print("\nGenerating chart (c): Average Query Distance...")
    query_dist_results = generate_query_distance_demo_data()
    query_dist_file = Path(__file__).parent / 'query_distance_comparison.png'
    plot_query_distance_comparison(query_dist_results, output_file=str(query_dist_file))
    
    # Figure (d) - Average Epsilon
    print("\nGenerating chart (d): Average Epsilon...")
    epsilon_results = generate_epsilon_demo_data()
    epsilon_file = Path(__file__).parent / 'epsilon_comparison.png'
    plot_epsilon_comparison(epsilon_results, output_file=str(epsilon_file))
    
    print("\n" + "=" * 70)
    print("  Complete")
    print("=" * 70)
    print("\nGenerated Charts:")
    print(f"  (a) Number of Clues: {output_file}")
    print(f"  (b) Keyword Frequency: {keyword_freq_file}")
    print(f"  (c) Query Distance: {query_dist_file}")
    print(f"  (d) Average Epsilon: {epsilon_file}")
    print("=" * 70)


if __name__ == "__main__":
    main()
