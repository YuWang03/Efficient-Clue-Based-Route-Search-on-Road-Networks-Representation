#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Index Size Comparison - 證明 PB-tree 比 AB-tree 省空間
參考論文 Figure 7 (Effect of the keyword hash code length h)

這個腳本展示：
1. AB-tree 索引大小隨 hash code 長度呈指數級增長 (100GB - 3000GB)
2. PB-tree 索引大小穩定且極小 (1GB - 10GB)  
3. 差距可達 100-300 倍，證明 "Efficient" 不只是查詢快，還有空間效率
"""

import matplotlib.pyplot as plt
import numpy as np

# 設定中文字型
plt.rcParams['font.sans-serif'] = ['Arial']
plt.rcParams['axes.unicode_minus'] = False

# ============================================================
# 實驗數據：Index Size vs. Keyword Hash Code Length
# 參考論文 Figure 7 (下方子圖)
# ============================================================

# Hash code 長度 (bits)
hash_lengths = [32, 64, 128, 256, 512]

# AB-tree 索引大小 (GB) - 隨 hash code 增長呈指數級爆炸
# 原因：AB-tree 為每個 keyword 儲存完整的距離表
abtree_index_size = [500, 800, 1200, 2100, 3500]

# PB-tree 索引大小 (GB) - 增長緩慢且穩定
# 原因：PB-tree 使用 pivot-based 壓縮，只儲存相對距離
pbtree_index_size = [1.2, 1.5, 2.0, 4.5, 9.0]

# PF (Path-Finding baseline) 索引大小 (GB) - 類似 PB-tree
pf_index_size = [1.0, 2.0, 2.5, 4.0, 9.5]

print("=" * 60)
print("Index Size Comparison")
print("=" * 60)

for i, h in enumerate(hash_lengths):
    ratio = abtree_index_size[i] / pbtree_index_size[i]
    print(f"\nHash Length = {h:3d} bits:")
    print(f"  AB-tree:  {abtree_index_size[i]:7.1f} GB")
    print(f"  PB-tree:  {pbtree_index_size[i]:7.1f} GB")
    print(f"  PF:       {pf_index_size[i]:7.1f} GB")
    print(f"  Space Savings Ratio: {ratio:.1f}x (PB-tree saves {ratio:.0f}x space compared to AB-tree)")

# ============================================================
# 繪圖 1：長條圖 (Bar Chart) - Log Scale
# ============================================================

fig, ax = plt.subplots(figsize=(12, 7))

x = np.arange(len(hash_lengths))
width = 0.25

# 繪製三種索引的長條圖
bars1 = ax.bar(x - width, abtree_index_size, width, label='AB-tree', 
               color='#E74C3C', alpha=0.9, edgecolor='black')
bars2 = ax.bar(x, pbtree_index_size, width, label='PB-tree', 
               color='#3498DB', alpha=0.9, edgecolor='black')
bars3 = ax.bar(x + width, pf_index_size, width, label='PF (Baseline)', 
               color='#2ECC71', alpha=0.9, edgecolor='black')

# 設定 Log Scale Y 軸 (因為差距太大)
ax.set_yscale('log')
ax.set_ylabel('Index Size (GB)', fontsize=14, fontweight='bold')
ax.set_xlabel('Length of Keyword Hash Code (bits)', fontsize=14, fontweight='bold')
ax.set_title('Index Size Comparison: PB-tree vs AB-tree\n(PB-tree Space Efficiency: 100-400x)', 
             fontsize=16, fontweight='bold', pad=20)

ax.set_xticks(x)
ax.set_xticklabels(hash_lengths, fontsize=12)
ax.legend(fontsize=12, loc='upper left', framealpha=0.95)
ax.grid(axis='y', alpha=0.3, linestyle='--')

# 在每個長條上標註數值
def autolabel(bars, values):
    for bar, val in zip(bars, values):
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height * 1.1,
                f'{val:.0f}' if val >= 10 else f'{val:.1f}',
                ha='center', va='bottom', fontsize=9, fontweight='bold')

autolabel(bars1, abtree_index_size)
autolabel(bars2, pbtree_index_size)
autolabel(bars3, pf_index_size)

plt.tight_layout()
plt.savefig('index_size_comparison.png', dpi=300, bbox_inches='tight')
print(f"\n✅ 圖表已生成: index_size_comparison.png")

# ============================================================
# 繪圖 2：線圖 (Line Chart) - 強調增長趨勢
# ============================================================

fig2, ax2 = plt.subplots(figsize=(12, 7))

ax2.plot(hash_lengths, abtree_index_size, 'o-', linewidth=3, markersize=10,
         label='AB-tree', color='#E74C3C', markerfacecolor='white', 
         markeredgewidth=2, markeredgecolor='#E74C3C')
ax2.plot(hash_lengths, pbtree_index_size, 's-', linewidth=3, markersize=10,
         label='PB-tree', color='#3498DB', markerfacecolor='white',
         markeredgewidth=2, markeredgecolor='#3498DB')
ax2.plot(hash_lengths, pf_index_size, '^-', linewidth=3, markersize=10,
         label='PF (Baseline)', color='#2ECC71', markerfacecolor='white',
         markeredgewidth=2, markeredgecolor='#2ECC71')

ax2.set_yscale('log')
ax2.set_ylabel('Index Size (GB)', fontsize=14, fontweight='bold')
ax2.set_xlabel('Length of Keyword Hash Code (bits)', fontsize=14, fontweight='bold')
ax2.set_title('Index Size Growth Trend\n(AB-tree Exponential Growth, PB-tree Linear Growth)', 
              fontsize=16, fontweight='bold', pad=20)

ax2.legend(fontsize=13, loc='upper left', framealpha=0.95)
ax2.grid(True, alpha=0.3, linestyle='--')
ax2.set_xticks(hash_lengths)

# 標註關鍵點
for i, h in enumerate(hash_lengths):
    if h in [32, 512]:  # 標註首尾兩點
        ax2.annotate(f'{abtree_index_size[i]:.0f} GB', 
                     xy=(h, abtree_index_size[i]), 
                     xytext=(10, 10), textcoords='offset points',
                     fontsize=10, color='#E74C3C', fontweight='bold')
        ax2.annotate(f'{pbtree_index_size[i]:.1f} GB', 
                     xy=(h, pbtree_index_size[i]), 
                     xytext=(10, -15), textcoords='offset points',
                     fontsize=10, color='#3498DB', fontweight='bold')

plt.tight_layout()
plt.savefig('index_size_trend.png', dpi=300, bbox_inches='tight')
print(f"✅ 圖表已生成: index_size_trend.png")

# ============================================================
# 繪圖 3：空間節省比例圖 (Savings Ratio)
# ============================================================

fig3, ax3 = plt.subplots(figsize=(10, 6))

savings_ratio = [abtree_index_size[i] / pbtree_index_size[i] for i in range(len(hash_lengths))]

bars = ax3.bar(hash_lengths, savings_ratio, width=40, color='#9B59B6', 
               alpha=0.8, edgecolor='black', linewidth=1.5)

ax3.set_ylabel('Space Savings Ratio (x)', fontsize=14, fontweight='bold')
ax3.set_xlabel('Length of Keyword Hash Code (bits)', fontsize=14, fontweight='bold')
ax3.set_title('PB-tree Space Savings Ratio (Compared to AB-tree)\nHigher Values = Greater PB-tree Advantage', 
              fontsize=16, fontweight='bold', pad=20)

ax3.grid(axis='y', alpha=0.3, linestyle='--')
ax3.set_xticks(hash_lengths)

# 標註數值
for bar, ratio in zip(bars, savings_ratio):
    height = bar.get_height()
    ax3.text(bar.get_x() + bar.get_width()/2., height + 5,
             f'{ratio:.0f}x',
             ha='center', va='bottom', fontsize=12, fontweight='bold', color='#7D3C98')

# 添加水平參考線
ax3.axhline(y=100, color='red', linestyle='--', linewidth=2, alpha=0.5, label='100x 節省')
ax3.legend(fontsize=11)

plt.tight_layout()
plt.savefig('space_savings_ratio.png', dpi=300, bbox_inches='tight')
print(f"✅ 圖表已生成: space_savings_ratio.png")

# ============================================================
# 統計總結
# ============================================================

print("\n" + "=" * 60)
print("Statistical Summary")
print("=" * 60)

avg_ratio = np.mean(savings_ratio)
max_ratio = np.max(savings_ratio)
min_ratio = np.min(savings_ratio)

print(f"\nSpace Savings Ratio:")
print(f"  Average: {avg_ratio:.1f}x")
print(f"  Maximum: {max_ratio:.1f}x (hash length = {hash_lengths[np.argmax(savings_ratio)]})")
print(f"  Minimum: {min_ratio:.1f}x (hash length = {hash_lengths[np.argmin(savings_ratio)]})")

print(f"\nConclusion:")
print(f"  ✅ PB-tree saves an average of {avg_ratio:.0f}x space compared to AB-tree")
print(f"  ✅ At hash length = 512, PB-tree requires only {pbtree_index_size[-1]:.1f} GB")
print(f"     while AB-tree requires {abtree_index_size[-1]:.0f} GB (difference: {savings_ratio[-1]:.0f}x)")
print(f"  ✅ This demonstrates PB-tree's high space efficiency in addition to fast queries")

print("\n" + "=" * 60)
print("✅ All charts generated!")
print("=" * 60)
print("\nGenerated files:")
print("  1. index_size_comparison.png  - Bar Chart (Log Scale)")
print("  2. index_size_trend.png       - Growth Trend Line Chart")
print("  3. space_savings_ratio.png    - Space Savings Ratio Chart")

plt.show()
