#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Accuracy of GCS - Demonstrate why GCS is not the optimal solution

Purpose: 
- Show that GCS is FAST but has POOR ACCURACY
- Matching Ratio > 1 means GCS finds suboptimal paths
- Hitting Ratio < 1 means GCS doesn't always find the optimal solution
- This justifies developing BAB algorithm for better solution quality

Chart Structure:
- 4 subplots: (a) Number of clues, (b) Keyword frequency, 
              (c) Query distance, (d) Average epsilon
- 2 metrics per subplot: Matching Ratio (red), Hitting Ratio (blue)
"""

import matplotlib.pyplot as plt
import numpy as np

# Set font to Arial for English charts
plt.rcParams['font.sans-serif'] = ['Arial']
plt.rcParams['axes.unicode_minus'] = False

# ============================================================
# Demo Data: GCS Accuracy Metrics
# ============================================================

def generate_gcs_accuracy_data():
    """
    Generate demo data showing GCS accuracy degrades with complexity
    
    Matching Ratio = Distance found by GCS / Optimal distance
    - Value of 1.0 = Perfect (GCS found optimal solution)
    - Value > 1.0 = Suboptimal (GCS found longer path)
    - Higher value = Worse accuracy
    
    Hitting Ratio = Percentage of queries where GCS finds optimal solution
    - Value of 1.0 = 100% optimal
    - Value < 1.0 = Sometimes suboptimal
    - Lower value = Worse accuracy
    """
    
    # (a) Number of clues: 2, 3, 4, 5, 6, 7, 8
    clues = [2, 3, 4, 5, 6, 7, 8]
    matching_ratio_clues = [1.5, 2.0, 2.3, 2.35, 2.4, 2.6, 2.9]
    hitting_ratio_clues = [0.85, 0.70, 0.55, 0.48, 0.43, 0.38, 0.35]
    
    # (b) Average keyword frequency: 10, 50, 100, 500, 1000, 5000, 10000
    keyword_freq = [10, 50, 100, 500, 1000, 5000, 10000]
    matching_ratio_freq = [1.1, 1.5, 2.3, 2.35, 2.35, 2.35, 2.35]
    hitting_ratio_freq = [0.88, 0.75, 0.55, 0.52, 0.50, 0.50, 0.50]
    
    # (c) Average query distance (km): 2, 4, 6, 8, 10, 12, 14
    query_distance = [2, 4, 6, 8, 10, 12, 14]
    matching_ratio_dist = [1.8, 2.1, 2.3, 2.4, 2.45, 2.6, 2.8]
    hitting_ratio_dist = [0.65, 0.58, 0.55, 0.53, 0.52, 0.50, 0.48]
    
    # (d) Average epsilon: 0.2, 0.4, 0.6, 0.8, 1.0
    epsilon = [0.2, 0.4, 0.6, 0.8, 1.0]
    matching_ratio_eps = [1.5, 2.0, 2.4, 2.7, 2.8]
    hitting_ratio_eps = [0.65, 0.60, 0.58, 0.55, 0.52]
    
    return {
        'clues': {
            'x': clues,
            'matching': matching_ratio_clues,
            'hitting': hitting_ratio_clues
        },
        'frequency': {
            'x': keyword_freq,
            'matching': matching_ratio_freq,
            'hitting': hitting_ratio_freq
        },
        'distance': {
            'x': query_distance,
            'matching': matching_ratio_dist,
            'hitting': hitting_ratio_dist
        },
        'epsilon': {
            'x': epsilon,
            'matching': matching_ratio_eps,
            'hitting': hitting_ratio_eps
        }
    }


def plot_accuracy_subplot(ax, x_data, matching_ratio, hitting_ratio, 
                          xlabel, title, xscale='linear'):
    """
    Plot a single subplot with dual y-axes
    
    Left y-axis: Matching Ratio (red, 1.0-3.0)
    Right y-axis: Hitting Ratio (blue, 0.0-1.0)
    """
    
    # Create twin axis for hitting ratio
    ax2 = ax.twinx()
    
    # Plot Matching Ratio (Left Y-axis, Red)
    line1 = ax.plot(x_data, matching_ratio, 'o-', color='#E74C3C', 
                    linewidth=2.5, markersize=8, label='Matching ratio',
                    markerfacecolor='white', markeredgewidth=2, 
                    markeredgecolor='#E74C3C')
    
    # Plot Hitting Ratio (Right Y-axis, Blue)
    line2 = ax2.plot(x_data, hitting_ratio, 's-', color='#3498DB', 
                     linewidth=2.5, markersize=8, label='Hitting ratio',
                     markerfacecolor='white', markeredgewidth=2, 
                     markeredgecolor='#3498DB')
    
    # Configure left y-axis (Matching Ratio)
    ax.set_ylabel('Matching ratio', fontsize=11, fontweight='bold', color='#E74C3C')
    ax.set_ylim(1.0, 3.0)
    ax.tick_params(axis='y', labelcolor='#E74C3C', labelsize=10)
    ax.grid(True, alpha=0.3, linestyle='--', linewidth=0.8)
    
    # Configure right y-axis (Hitting Ratio)
    ax2.set_ylabel('Hitting ratio', fontsize=11, fontweight='bold', color='#3498DB')
    ax2.set_ylim(0.0, 1.0)
    ax2.tick_params(axis='y', labelcolor='#3498DB', labelsize=10)
    
    # Configure x-axis
    ax.set_xlabel(xlabel, fontsize=11, fontweight='bold')
    ax.set_title(title, fontsize=12, fontweight='bold', pad=10)
    ax.tick_params(axis='x', labelsize=10)
    
    # Set x-axis scale (log for frequency)
    if xscale == 'log':
        ax.set_xscale('log')
    
    # Add legend at the top
    lines = line1 + line2
    labels = [l.get_label() for l in lines]
    ax.legend(lines, labels, loc='upper center', bbox_to_anchor=(0.5, 1.15),
              ncol=2, fontsize=9, framealpha=0.95)


def main():
    """Generate Fig. 5: Accuracy of GCS"""
    
    print("=" * 70)
    print("  Accuracy of GCS - Demonstrating Suboptimal Solution Quality")
    print("=" * 70)
    print("\n📊 Purpose: Show that GCS is FAST but INACCURATE")
    print("   - Matching Ratio > 1.0 → GCS finds longer paths (suboptimal)")
    print("   - Hitting Ratio < 1.0 → GCS doesn't always find optimal solution")
    print("   - This justifies developing BAB algorithm for better accuracy\n")
    print("=" * 70 + "\n")
    
    # Generate data
    data = generate_gcs_accuracy_data()
    
    # Print statistical summary
    print("\n📈 Statistical Summary:\n")
    
    print("(a) Number of Clues:")
    print(f"    Matching Ratio: {min(data['clues']['matching']):.2f} → {max(data['clues']['matching']):.2f}")
    print(f"    Hitting Ratio:  {max(data['clues']['hitting']):.2f} → {min(data['clues']['hitting']):.2f}")
    print(f"    → As clues increase, GCS accuracy degrades significantly\n")
    
    print("(b) Keyword Frequency:")
    print(f"    Matching Ratio: {min(data['frequency']['matching']):.2f} → {max(data['frequency']['matching']):.2f}")
    print(f"    Hitting Ratio:  {max(data['frequency']['hitting']):.2f} → {min(data['frequency']['hitting']):.2f}")
    print(f"    → High frequency keywords lead to suboptimal paths\n")
    
    print("(c) Query Distance:")
    print(f"    Matching Ratio: {min(data['distance']['matching']):.2f} → {max(data['distance']['matching']):.2f}")
    print(f"    Hitting Ratio:  {max(data['distance']['hitting']):.2f} → {min(data['distance']['hitting']):.2f}")
    print(f"    → Longer distances result in worse accuracy\n")
    
    print("(d) Average Epsilon:")
    print(f"    Matching Ratio: {min(data['epsilon']['matching']):.2f} → {max(data['epsilon']['matching']):.2f}")
    print(f"    Hitting Ratio:  {max(data['epsilon']['hitting']):.2f} → {min(data['epsilon']['hitting']):.2f}")
    print(f"    → Larger epsilon tolerance causes more errors\n")
    
    print("=" * 70)
    
    # Create figure with 4 subplots
    fig, axes = plt.subplots(1, 4, figsize=(16, 4))
    fig.suptitle('Fig. 5. Accuracy of GCS', fontsize=16, fontweight='bold', y=1.02)
    
    # (a) Number of clues
    plot_accuracy_subplot(
        axes[0], 
        data['clues']['x'], 
        data['clues']['matching'], 
        data['clues']['hitting'],
        xlabel='Number of clues',
        title='(a) Number of clues'
    )
    
    # (b) Average keyword frequency (log scale)
    plot_accuracy_subplot(
        axes[1], 
        data['frequency']['x'], 
        data['frequency']['matching'], 
        data['frequency']['hitting'],
        xlabel='Average keyword frequency',
        title='(b) Average keyword frequency',
        xscale='log'
    )
    
    # (c) Average query distance
    plot_accuracy_subplot(
        axes[2], 
        data['distance']['x'], 
        data['distance']['matching'], 
        data['distance']['hitting'],
        xlabel='Average query distance (km)',
        title='(c) Average query distance (km)'
    )
    
    # (d) Average epsilon
    plot_accuracy_subplot(
        axes[3], 
        data['epsilon']['x'], 
        data['epsilon']['matching'], 
        data['epsilon']['hitting'],
        xlabel='Average ε',
        title='(d) Average ε'
    )
    
    plt.tight_layout()
    
    # Save figure
    output_file = 'accuracy_of_gcs.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"\n✅ Chart saved: {output_file}")
    
    print("\n" + "=" * 70)
    print("  Key Insights:")
    print("=" * 70)
    print("\n  1. GCS has poor accuracy (Matching Ratio 1.5~2.9x)")
    print("  2. GCS only finds optimal solution 35-88% of the time")
    print("  3. Accuracy degrades with query complexity")
    print("  4. This justifies developing BAB algorithm despite higher time cost")
    print("\n  💡 Conclusion: GCS is FAST but INACCURATE →")
    print("     BAB provides better solution quality at acceptable time cost")
    print("=" * 70)
    
    plt.show()


if __name__ == "__main__":
    main()
